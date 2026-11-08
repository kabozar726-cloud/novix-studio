package com.novix.studio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

data class EditSettings(
    val brightness: Int = 100, // 0..200, 100 = normal
    val contrast: Int = 100, // 0..200
    val saturation: Int = 100, // 0..200
    val grayscale: Int = 0, // 0..100
    val warmth: Int = 100, // 0..200, 100 = neutral
    val highlights: Int = 100, // 0..200, 100 = neutral
    val shadows: Int = 100, // 0..200, 100 = neutral
    val sharpness: Int = 0, // 0..100
    val vignette: Int = 0, // 0..100
    val filter: String = "Normal"
)

data class StickerState(
    val emoji: String,
    val x: Float, // 0..1 fraction of photo width
    val y: Float, // 0..1 fraction of photo height
    val size: Float = 0.16f // fraction of photo width
)

fun multiply4x5(a: FloatArray, b: FloatArray): FloatArray {
    // Returns a * b: apply b first, then a.
    val r = FloatArray(20)
    for (row in 0..3) {
        for (col in 0..3) {
            var sum = 0f
            for (k in 0..3) sum += a[row * 5 + k] * b[k * 5 + col]
            r[row * 5 + col] = sum
        }
        var t = a[row * 5 + 4]
        for (k in 0..3) t += a[row * 5 + k] * b[k * 5 + 4]
        r[row * 5 + 4] = t
    }
    return r
}

fun saturationMatrix(s: Float): FloatArray {
    val lr = 0.213f
    val lg = 0.715f
    val lb = 0.072f
    val inv = 1f - s
    return floatArrayOf(
        inv * lr + s, inv * lg, inv * lb, 0f, 0f,
        inv * lr, inv * lg + s, inv * lb, 0f, 0f,
        inv * lr, inv * lg, inv * lb + s, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
}

fun contrastMatrix(c: Float): FloatArray {
    val t = (1f - c) * 128f
    return floatArrayOf(
        c, 0f, 0f, 0f, t,
        0f, c, 0f, 0f, t,
        0f, 0f, c, 0f, t,
        0f, 0f, 0f, 1f, 0f
    )
}

fun brightnessMatrix(b: Float): FloatArray {
    return floatArrayOf(
        1f, 0f, 0f, 0f, b,
        0f, 1f, 0f, 0f, b,
        0f, 0f, 1f, 0f, b,
        0f, 0f, 0f, 1f, 0f
    )
}

fun warmthMatrix(w: Int): FloatArray {
    // w: 0..200, 100 = neutral. Warm = more red, less blue.
    val off = (w - 100) * 0.4f
    return floatArrayOf(
        1f, 0f, 0f, 0f, off,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, -off,
        0f, 0f, 0f, 1f, 0f
    )
}

fun highlightsMatrix(h: Int): FloatArray {
    // Approximation: lift brightness a little, flatten contrast a touch.
    val off = (h - 100) * 0.4f
    val cf = 1f - (h - 100) * 0.001f
    return multiply4x5(brightnessMatrix(off), contrastMatrix(cf))
}

fun shadowsMatrix(s: Int): FloatArray {
    // Approximation: lift shadows, add a touch of contrast.
    val off = (s - 100) * 0.4f
    val cf = 1f + (s - 100) * 0.001f
    return multiply4x5(brightnessMatrix(off), contrastMatrix(cf))
}

fun identityMatrix(): FloatArray {
    return floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    )
}

fun presetMatrix(name: String): FloatArray {
    return when (name) {
        "Warm" -> floatArrayOf(
            1.1f, 0f, 0f, 0f, 8f,
            0f, 1.0f, 0f, 0f, 0f,
            0f, 0f, 0.9f, 0f, -6f,
            0f, 0f, 0f, 1f, 0f
        )
        "Cool" -> floatArrayOf(
            0.9f, 0f, 0f, 0f, -4f,
            0f, 1.0f, 0f, 0f, 2f,
            0f, 0f, 1.1f, 0f, 8f,
            0f, 0f, 0f, 1f, 0f
        )
        "Vivid" -> multiply4x5(contrastMatrix(1.05f), saturationMatrix(1.4f))
        "Mono" -> saturationMatrix(0f)
        "Sepia" -> floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        "Fade" -> multiply4x5(brightnessMatrix(10f), contrastMatrix(0.9f))
        "Noir" -> multiply4x5(contrastMatrix(1.3f), saturationMatrix(0f))
        "Retro" -> multiply4x5(
            contrastMatrix(0.95f),
            floatArrayOf(
                1.05f, 0f, 0f, 0f, 6f,
                0f, 1.0f, 0f, 0f, 2f,
                0f, 0f, 0.85f, 0f, -4f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        "Vintage" -> multiply4x5(
            warmthMatrix(115),
            multiply4x5(
                brightnessMatrix(8f),
                multiply4x5(contrastMatrix(0.88f), saturationMatrix(0.85f))
            )
        )
        "Cyberpunk" -> multiply4x5(
            floatArrayOf(
                1.1f, 0f, 0f, 0f, 10f,
                0f, 0.9f, 0f, 0f, 0f,
                0f, 0f, 1.25f, 0f, 14f,
                0f, 0f, 0f, 1f, 0f
            ),
            saturationMatrix(1.2f)
        )
        "Soft Glow" -> multiply4x5(
            brightnessMatrix(12f),
            multiply4x5(contrastMatrix(0.9f), saturationMatrix(0.9f))
        )
        "Golden" -> multiply4x5(warmthMatrix(135), saturationMatrix(1.25f))
        "Rose" -> floatArrayOf(
            1.12f, 0f, 0f, 0f, 10f,
            0f, 0.95f, 0.05f, 0f, 4f,
            0f, 0f, 1.05f, 0f, 8f,
            0f, 0f, 0f, 1f, 0f
        )
        "Ocean" -> multiply4x5(
            floatArrayOf(
                0.9f, 0f, 0f, 0f, -4f,
                0f, 1.05f, 0f, 0f, 4f,
                0f, 0f, 1.2f, 0f, 12f,
                0f, 0f, 0f, 1f, 0f
            ),
            saturationMatrix(1.15f)
        )
        "Forest" -> multiply4x5(
            floatArrayOf(
                0.95f, 0f, 0f, 0f, 0f,
                0f, 1.15f, 0f, 0f, 6f,
                0f, 0f, 0.95f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ),
            saturationMatrix(1.1f)
        )
        else -> identityMatrix()
    }
}

fun buildEditMatrix(s: EditSettings): FloatArray {
    val sat = (s.saturation / 100f) * (1f - s.grayscale / 100f)
    val mSat = saturationMatrix(sat)
    val mCon = contrastMatrix(s.contrast / 100f)
    val mBri = brightnessMatrix((s.brightness - 100).toFloat())
    val mWarm = warmthMatrix(s.warmth)
    val mHi = highlightsMatrix(s.highlights)
    val mSh = shadowsMatrix(s.shadows)
    val mPre = presetMatrix(s.filter)
    return multiply4x5(
        mSh,
        multiply4x5(
            mHi,
            multiply4x5(
                mWarm,
                multiply4x5(mBri, multiply4x5(mCon, multiply4x5(mSat, mPre)))
            )
        )
    )
}

/** Matrix-only edit pass (no text). */
fun applyEdits(src: Bitmap, s: EditSettings): Bitmap {
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(buildEditMatrix(s)))
    canvas.drawBitmap(src, 0f, 0f, paint)
    var result = out
    if (s.sharpness > 0) result = applySharpness(result, s.sharpness)
    if (s.vignette > 0) result = applyVignette(result, s.vignette)
    return result
}

/** v1-compatible: matrix + centred text overlay at default size. */
fun applyEdits(src: Bitmap, s: EditSettings, overlayText: String, textColor: Int): Bitmap {
    val edited = applyEdits(src, s)
    if (overlayText.isBlank()) return edited
    return drawTextOverlay(edited, overlayText, textColor, 100)
}

fun drawTextOverlay(src: Bitmap, text: String, textColor: Int, sizePct: Int): Bitmap {
    if (text.isBlank()) return src
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawBitmap(src, 0f, 0f, null)
    val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    tp.color = textColor
    tp.textSize = (src.width / 12f * (sizePct / 100f)).coerceAtLeast(28f)
    tp.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    tp.textAlign = Paint.Align.CENTER
    tp.setShadowLayer(6f, 0f, 2f, Color.argb(204, 0, 0, 0))
    val x = src.width / 2f
    val y = src.height - src.height * 0.08f
    canvas.drawText(text, x, y, tp)
    return out
}

fun drawStickers(src: Bitmap, stickers: List<StickerState>): Bitmap {
    if (stickers.isEmpty()) return src
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawBitmap(src, 0f, 0f, null)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.textAlign = Paint.Align.CENTER
    for (st in stickers) {
        paint.textSize = (src.width * st.size).coerceAtLeast(24f)
        val x = st.x * src.width
        val y = st.y * src.height + paint.textSize * 0.35f
        canvas.drawText(st.emoji, x, y, paint)
    }
    return out
}

fun rotateBitmap(src: Bitmap, degrees: Int): Bitmap {
    if (degrees % 360 == 0) return src
    val matrix = Matrix()
    matrix.postRotate(degrees.toFloat())
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
}

/** Downscale-then-upscale blur. radius 1..25. */
fun blurBitmap(src: Bitmap, radius: Int): Bitmap {
    if (radius <= 0) return src
    val f = 1f / (1f + radius * 0.4f)
    val sw = (src.width * f).toInt().coerceAtLeast(1)
    val sh = (src.height * f).toInt().coerceAtLeast(1)
    val small = Bitmap.createScaledBitmap(src, sw, sh, true)
    val out = Bitmap.createScaledBitmap(small, src.width, src.height, true)
    if (small != src) small.recycle()
    return out
}

/** Unsharp-mask style sharpening. amount 0..100. */
fun applySharpness(src: Bitmap, amount: Int): Bitmap {
    if (amount <= 0) return src
    val blurred = blurBitmap(src, 6)
    val w = src.width
    val h = src.height
    val srcPx = IntArray(w * h)
    val blurPx = IntArray(w * h)
    src.getPixels(srcPx, 0, w, 0, 0, w, h)
    blurred.getPixels(blurPx, 0, w, 0, 0, w, h)
    val k = amount / 100f * 0.8f
    val outPx = IntArray(w * h)
    for (i in srcPx.indices) {
        val s = srcPx[i]
        val b = blurPx[i]
        val sa = s ushr 24
        val sr = ((s shr 16) and 0xFF) + (((((s shr 16) and 0xFF) - ((b shr 16) and 0xFF)) * k).toInt())
        val sg = ((s shr 8) and 0xFF) + (((((s shr 8) and 0xFF) - ((b shr 8) and 0xFF)) * k).toInt())
        val sb = (s and 0xFF) + ((((s and 0xFF) - (b and 0xFF)) * k).toInt())
        outPx[i] = (sa shl 24) or
            (sr.coerceIn(0, 255) shl 16) or
            (sg.coerceIn(0, 255) shl 8) or
            sb.coerceIn(0, 255)
    }
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    out.setPixels(outPx, 0, w, 0, 0, w, h)
    return out
}

/** Radial darkening at the edges. strength 0..100. */
fun applyVignette(src: Bitmap, strength: Int): Bitmap {
    if (strength <= 0) return src
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawBitmap(src, 0f, 0f, null)
    val cx = src.width / 2f
    val cy = src.height / 2f
    val radius = kotlin.math.max(src.width, src.height) * 0.75f
    val edgeAlpha = (strength / 100f * 0.75f * 255).toInt().coerceIn(0, 255)
    val gradient = RadialGradient(
        cx, cy, radius,
        intArrayOf(Color.TRANSPARENT, Color.argb(edgeAlpha, 0, 0, 0)),
        floatArrayOf(0.55f, 1f),
        Shader.TileMode.CLAMP
    )
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.shader = gradient
    canvas.drawRect(0f, 0f, src.width.toFloat(), src.height.toFloat(), paint)
    return out
}

/** Blend two same-size bitmaps: result = a*(1-alpha) + b*alpha. */
fun blendBitmaps(a: Bitmap, b: Bitmap, alpha: Float): Bitmap {
    val w = a.width
    val h = a.height
    val aPx = IntArray(w * h)
    val bPx = IntArray(w * h)
    a.getPixels(aPx, 0, w, 0, 0, w, h)
    b.getPixels(bPx, 0, w, 0, 0, w, h)
    val outPx = IntArray(w * h)
    for (i in aPx.indices) {
        val pa = aPx[i]
        val pb = bPx[i]
        val r = (((pa shr 16) and 0xFF) * (1f - alpha) + ((pb shr 16) and 0xFF) * alpha).toInt()
        val g = (((pa shr 8) and 0xFF) * (1f - alpha) + ((pb shr 8) and 0xFF) * alpha).toInt()
        val bl = ((pa and 0xFF) * (1f - alpha) + (pb and 0xFF) * alpha).toInt()
        outPx[i] = ((pa ushr 24) shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or bl.coerceIn(0, 255)
    }
    val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    out.setPixels(outPx, 0, w, 0, 0, w, h)
    return out
}

fun applyEffect(src: Bitmap, effect: String): Bitmap {
    return when (effect) {
        "Cartoon" -> {
            // Posterize to 4 levels per channel, then boost saturation.
            val w = src.width
            val h = src.height
            val px = IntArray(w * h)
            src.getPixels(px, 0, w, 0, 0, w, h)
            for (i in px.indices) {
                val p = px[i]
                val r = ((p shr 16) and 0xFF) / 85 * 85
                val g = ((p shr 8) and 0xFF) / 85 * 85
                val b = (p and 0xFF) / 85 * 85
                px[i] = ((p ushr 24) shl 24) or (r shl 16) or (g shl 8) or b
            }
            val flat = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            flat.setPixels(px, 0, w, 0, 0, w, h)
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(saturationMatrix(1.4f)))
            canvas.drawBitmap(flat, 0f, 0f, paint)
            out
        }
        "Anime" -> {
            val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val m = multiply4x5(
                brightnessMatrix(12f),
                multiply4x5(contrastMatrix(1.15f), saturationMatrix(1.6f))
            )
            paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(m))
            canvas.drawBitmap(src, 0f, 0f, paint)
            out
        }
        "Oil Art" -> {
            val blurred = blurBitmap(src, 4)
            val blended = blendBitmaps(src, blurred, 0.35f)
            val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val m = multiply4x5(contrastMatrix(1.08f), saturationMatrix(1.3f))
            paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(m))
            canvas.drawBitmap(blended, 0f, 0f, paint)
            out
        }
        "Vintage", "Cyberpunk" -> {
            val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(presetMatrix(effect)))
            canvas.drawBitmap(src, 0f, 0f, paint)
            out
        }
        else -> src
    }
}

/** smooth 0..100 = blur blend, brighten 0..100 = light lift, detail 0..100 = sharpen. */
fun applyRetouch(src: Bitmap, smooth: Int, brighten: Int, detail: Int): Bitmap {
    var result = src
    if (smooth > 0) {
        val blurred = blurBitmap(result, 3)
        result = blendBitmaps(result, blurred, smooth / 100f)
    }
    if (brighten > 0) {
        val out = Bitmap.createBitmap(result.width, result.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(brightnessMatrix(brighten * 0.3f)))
        canvas.drawBitmap(result, 0f, 0f, paint)
        result = out
    }
    if (detail > 0) result = applySharpness(result, detail)
    return result
}

/** Portrait blur: blurred copy, subject kept sharp inside a centre ellipse. strength 0..100. */
fun portraitBlur(src: Bitmap, strength: Int): Bitmap {
    if (strength <= 0) return src
    val radius = 8 + strength / 8
    val blurred = blurBitmap(src, radius)
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawBitmap(blurred, 0f, 0f, null)
    val left = src.width * 0.14f
    val top = src.height * 0.06f
    val right = src.width * 0.86f
    val bottom = src.height * 0.94f
    val path = Path()
    path.addOval(RectF(left, top, right, bottom), Path.Direction.CW)
    canvas.save()
    canvas.clipPath(path)
    canvas.drawBitmap(src, 0f, 0f, null)
    canvas.restore()
    return out
}

fun drawFrame(src: Bitmap, frame: String): Bitmap {
    return when (frame) {
        "Polaroid" -> {
            val side = (src.width * 0.06f).toInt().coerceAtLeast(8)
            val top = side
            val bottom = (src.width * 0.20f).toInt().coerceAtLeast(40)
            val outW = src.width + side * 2
            val outH = src.height + top + bottom
            val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            canvas.drawColor(Color.WHITE)
            val scaled = Bitmap.createScaledBitmap(src, src.width, src.height, true)
            canvas.drawBitmap(scaled, side.toFloat(), top.toFloat(), null)
            out
        }
        "Classic" -> {
            val border = (src.width * 0.02f).toInt().coerceAtLeast(4)
            val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            canvas.drawColor(Color.BLACK)
            val inner = Bitmap.createBitmap(src, border, border, src.width - border * 2, src.height - border * 2)
            canvas.drawBitmap(inner, border.toFloat(), border.toFloat(), null)
            out
        }
        "Neon" -> {
            val border = (src.width * 0.04f).toInt().coerceAtLeast(12)
            val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            canvas.drawColor(Color.BLACK)
            val inner = Bitmap.createScaledBitmap(
                src,
                (src.width - border * 2).coerceAtLeast(1),
                (src.height - border * 2).coerceAtLeast(1),
                true
            )
            canvas.drawBitmap(inner, border.toFloat(), border.toFloat(), null)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.style = Paint.Style.STROKE
            paint.color = Color.parseColor("#A3E635")
            paint.strokeWidth = (src.width * 0.015f).coerceAtLeast(4f)
            paint.setShadowLayer(24f, 0f, 0f, Color.parseColor("#A3E635"))
            val half = paint.strokeWidth / 2f
            canvas.drawRect(
                border + half, border + half,
                src.width - border - half, src.height - border - half,
                paint
            )
            out
        }
        else -> src
    }
}

/** Story template: lime top bar with bold title text. */
fun drawStoryFrame(src: Bitmap, title: String): Bitmap {
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawBitmap(src, 0f, 0f, null)
    val barH = (src.height * 0.09f).coerceAtLeast(60f)
    val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    barPaint.color = Color.parseColor("#A3E635")
    canvas.drawRect(0f, 0f, src.width.toFloat(), barH, barPaint)
    val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    tp.color = Color.BLACK
    tp.textSize = barH * 0.5f
    tp.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    tp.textAlign = Paint.Align.CENTER
    val label = if (title.isBlank()) "NOVIX" else title
    canvas.drawText(label, src.width / 2f, barH * 0.68f, tp)
    // Lime border around the whole photo.
    val bp = Paint(Paint.ANTI_ALIAS_FLAG)
    bp.style = Paint.Style.STROKE
    bp.color = Color.parseColor("#A3E635")
    bp.strokeWidth = (src.width * 0.012f).coerceAtLeast(4f)
    canvas.drawRect(0f, 0f, src.width.toFloat(), src.height.toFloat(), bp)
    return out
}

fun cropToRatio(src: Bitmap, ratio: String): Bitmap {
    if (ratio == "Original") return src
    val target = when (ratio) {
        "1:1" -> 1f
        "9:16" -> 9f / 16f
        "4:5" -> 4f / 5f
        "3:4" -> 3f / 4f
        "16:9" -> 16f / 9f
        else -> return src
    }
    val w = src.width
    val h = src.height
    val current = w.toFloat() / h
    if (current > target) {
        val nw = (h * target).toInt().coerceAtLeast(1)
        val x = (w - nw) / 2
        return Bitmap.createBitmap(src, x, 0, nw, h)
    } else {
        val nh = (w / target).toInt().coerceAtLeast(1)
        val y = (h - nh) / 2
        return Bitmap.createBitmap(src, 0, y, w, nh)
    }
}

/** Scale + centre-crop to exactly fill w x h. */
fun fitCrop(src: Bitmap, w: Int, h: Int): Bitmap {
    val scale = maxOf(w.toFloat() / src.width, h.toFloat() / src.height)
    val sw = (src.width * scale).toInt().coerceAtLeast(1)
    val sh = (src.height * scale).toInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(src, sw, sh, true)
    val x = (sw - w) / 2
    val y = (sh - h) / 2
    return Bitmap.createBitmap(scaled, x, y, w, h)
}

/** Collage builder. layout: "2V", "2H", "3", "4". Output is a 1080x1080 square. */
fun buildCollage(photos: List<Bitmap>, layout: String, gutterColor: Int): Bitmap {
    val size = 1080
    val gutter = 12
    val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawColor(gutterColor)
    fun cell(x: Int, y: Int, w: Int, h: Int, photo: Bitmap?) {
        if (photo == null) return
        val fitted = fitCrop(photo, w, h)
        canvas.drawBitmap(fitted, x.toFloat(), y.toFloat(), null)
    }
    val half = (size - gutter) / 2
    when (layout) {
        "2V" -> {
            cell(0, 0, size, half, photos.getOrNull(0))
            cell(0, half + gutter, size, half, photos.getOrNull(1))
        }
        "2H" -> {
            cell(0, 0, half, size, photos.getOrNull(0))
            cell(half + gutter, 0, half, size, photos.getOrNull(1))
        }
        "3" -> {
            cell(0, 0, size, half, photos.getOrNull(0))
            cell(0, half + gutter, half, half, photos.getOrNull(1))
            cell(half + gutter, half + gutter, half, half, photos.getOrNull(2))
        }
        else -> { // "4" 2x2 grid
            cell(0, 0, half, half, photos.getOrNull(0))
            cell(half + gutter, 0, half, half, photos.getOrNull(1))
            cell(0, half + gutter, half, half, photos.getOrNull(2))
            cell(half + gutter, half + gutter, half, half, photos.getOrNull(3))
        }
    }
    return out
}

fun saveToGallery(context: Context, bmp: Bitmap, quality: Int): Uri? {
    val name = "Novix_${System.currentTimeMillis()}.jpg"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NovixStudio")
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    resolver.openOutputStream(uri)?.use { out ->
        bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
    }
    return uri
}

fun savePngToGallery(context: Context, bmp: Bitmap): Uri? {
    val name = "Novix_${System.currentTimeMillis()}.png"
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, name)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NovixStudio")
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    resolver.openOutputStream(uri)?.use { out ->
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return uri
}

fun saveTempShareFile(context: Context, bmp: Bitmap): File {
    val file = File(context.cacheDir, "novix_share.jpg")
    FileOutputStream(file).use { out ->
        bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
    }
    return file
}
