package com.novix.studio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Real on-device person background removal using ML Kit Selfie Segmentation.
 * NOTE: the segmentation model downloads on first use (needs internet once),
 * afterwards it works fully on-device.
 * onResult is always called on the main thread: (cutoutBitmap, errorString).
 */
object BgRemover {

    fun removeBackground(context: Context, bitmap: Bitmap, onResult: (Bitmap?, String?) -> Unit) {
        try {
            val options = SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()
            val segmenter = Segmentation.getClient(options)
            val image = InputImage.fromBitmap(bitmap, 0)
            segmenter.process(image)
                .addOnSuccessListener { mask ->
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val cutout = buildCutout(bitmap, mask)
                            withContext(Dispatchers.Main) { onResult(cutout, null) }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                onResult(null, "Cutout banane mein masla: ${e.message}")
                            }
                        } finally {
                            segmenter.close()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    segmenter.close()
                    onResult(null, "Background remove nahi hua: ${e.message}")
                }
        } catch (e: Exception) {
            onResult(null, "Background remover start nahi hua: ${e.message}")
        }
    }

    private fun buildCutout(src: Bitmap, mask: SegmentationMask): Bitmap {
        val w = src.width
        val h = src.height
        val maskW = mask.width
        val maskH = mask.height
        val buffer = mask.buffer
        val srcPx = IntArray(w * h)
        src.getPixels(srcPx, 0, w, 0, 0, w, h)
        val outPx = IntArray(w * h)
        for (y in 0 until h) {
            val my = (y * maskH / h).coerceIn(0, maskH - 1)
            for (x in 0 until w) {
                val mx = (x * maskW / w).coerceIn(0, maskW - 1)
                val conf = buffer.get(my * maskW + mx)
                val idx = y * w + x
                outPx[idx] = if (conf >= 0.5f) srcPx[idx] else 0
            }
        }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(outPx, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * mode: "Transparent" (keep alpha), "Blur" (bg = blurred original Bitmap),
     * "Colour" (bg = solid ARGB Int), "Photo" (bg = user-picked Bitmap).
     */
    fun applyBackground(cutout: Bitmap, mode: String, bg: Any?): Bitmap {
        val out = Bitmap.createBitmap(cutout.width, cutout.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        when (mode) {
            "Blur", "Photo" -> {
                val bgBmp = bg as? Bitmap
                if (bgBmp != null) {
                    val fitted = fitCrop(bgBmp, cutout.width, cutout.height)
                    canvas.drawBitmap(fitted, 0f, 0f, null)
                }
            }
            "Colour" -> {
                val color = bg as? Int ?: Color.WHITE
                canvas.drawColor(color)
            }
            else -> {
                // Transparent: draw nothing behind the cutout.
            }
        }
        canvas.drawBitmap(cutout, 0f, 0f, null)
        return out
    }
}
