package com.novix.studio

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

data class EditSettings(
    val brightness: Int = 100,
    val contrast: Int = 100,
    val saturation: Int = 100,
    val grayscale: Int = 0,
    val filter: String = "Normal"
)

fun multiply4x5(a: FloatArray, b: FloatArray): FloatArray {
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
        else -> identityMatrix()
    }
}

fun buildEditMatrix(s: EditSettings): FloatArray {
    val sat = (s.saturation / 100f) * (1f - s.grayscale / 100f)
    val mSat = saturationMatrix(sat)
    val mCon = contrastMatrix(s.contrast / 100f)
    val mBri = brightnessMatrix((s.brightness - 100).toFloat())
    val mPre = presetMatrix(s.filter)
    return multiply4x5(mBri, multiply4x5(mCon, multiply4x5(mSat, mPre)))
}

fun applyEdits(src: Bitmap, s: EditSettings, overlayText: String, textColor: Int): Bitmap {
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.colorFilter = ColorMatrixColorFilter(ColorMatrix(buildEditMatrix(s)))
    canvas.drawBitmap(src, 0f, 0f, paint)
    if (overlayText.isNotBlank()) {
        val tp = Paint(Paint.ANTI_ALIAS_FLAG)
        tp.color = textColor
        tp.textSize = (src.width / 12f).coerceAtLeast(28f)
        tp.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        tp.textAlign = Paint.Align.CENTER
        tp.setShadowLayer(6f, 0f, 2f, Color.argb(204, 0, 0, 0))
        val x = src.width / 2f
        val y = src.height - src.height * 0.08f
        canvas.drawText(overlayText, x, y, tp)
    }
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

fun saveTempShareFile(context: Context, bmp: Bitmap): File {
    val file = File(context.cacheDir, "novix_share.jpg")
    FileOutputStream(file).use { out ->
        bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
    }
    return file
}
