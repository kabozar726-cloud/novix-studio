package com.novix.studio

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorViewModel : ViewModel() {
    var status by mutableStateOf("Photo import karein")
        private set
    var hasPhoto by mutableStateOf(false)
        private set
    var preview by mutableStateOf<Bitmap?>(null)
        private set
    var settings by mutableStateOf(EditSettings())
        private set
    var overlayText by mutableStateOf("")
        private set
    var textColor by mutableStateOf(-1)
        private set
    var cropRatio by mutableStateOf("Original")
        private set
    var isBusy by mutableStateOf(false)
        private set
    var lastSavedUri by mutableStateOf<Uri?>(null)
        private set

    private var original: Bitmap? = null
    private var rendered: Bitmap? = null

    fun loadPhoto(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            isBusy = true
            status = "Photo load ho rahi hai..."
            try {
                val bmp = context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
                if (bmp == null) {
                    withContext(Dispatchers.Main) { status = "Photo load nahi hui. Dobara try karein." }
                    return@launch
                }
                val scaled = scaleDown(bmp)
                original = scaled
                settings = EditSettings()
                overlayText = ""
                cropRatio = "Original"
                hasPhoto = true
                renderPreview()
                withContext(Dispatchers.Main) { status = "Photo tayyar hai. Edit karein." }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { status = "Photo load nahi hui: ${e.message}" }
            } finally {
                isBusy = false
            }
        }
    }

    private fun scaleDown(bmp: Bitmap): Bitmap {
        val maxSide = 2048
        val w = bmp.width
        val h = bmp.height
        if (w <= maxSide && h <= maxSide) return bmp
        val scale = maxSide.toFloat() / maxOf(w, h)
        val nw = (w * scale).toInt().coerceAtLeast(1)
        val nh = (h * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bmp, nw, nh, true)
    }

    private suspend fun renderPreview() {
        val src = original ?: return
        val result = withContext(Dispatchers.IO) {
            val cropped = cropToRatio(src, cropRatio)
            applyEdits(cropped, settings, overlayText, textColor)
        }
        preview = result
        rendered = result
    }

    fun updateSettings(newSettings: EditSettings) {
        settings = newSettings
        viewModelScope.launch { renderPreview() }
    }

    fun updateText(text: String) {
        overlayText = text
        viewModelScope.launch { renderPreview() }
    }

    fun updateTextColor(color: Int) {
        textColor = color
        viewModelScope.launch { renderPreview() }
    }

    fun updateCrop(ratio: String) {
        cropRatio = ratio
        viewModelScope.launch { renderPreview() }
    }

    fun autoEnhance() {
        settings = settings.copy(brightness = 104, contrast = 110, saturation = 115)
        viewModelScope.launch { renderPreview() }
        status = "Auto Enhance lag gaya."
    }

    fun resetEdits() {
        settings = EditSettings()
        overlayText = ""
        cropRatio = "Original"
        viewModelScope.launch { renderPreview() }
        status = "Edits reset ho gayin."
    }

    fun savePhoto(context: Context, pro: Boolean) {
        val bmp = rendered ?: return
        viewModelScope.launch(Dispatchers.IO) {
            isBusy = true
            status = "Photo save ho rahi hai..."
            val quality = if (pro) 100 else 85
            val uri = saveToGallery(context, bmp, quality)
            withContext(Dispatchers.Main) {
                isBusy = false
                if (uri != null) {
                    lastSavedUri = uri
                    status = "Photo save ho gayi. Gallery mein dekhein."
                } else {
                    status = "Save nahi hui. Dobara try karein."
                }
            }
        }
    }

    fun sharePhoto(context: Context) {
        val bmp = rendered ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = saveTempShareFile(context, bmp)
                val uri = FileProvider.getUriForFile(context, "com.novix.studio.fileprovider", file)
                withContext(Dispatchers.Main) {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(share, "Photo share karein"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { status = "Share nahi hua: ${e.message}" }
            }
        }
    }
}
