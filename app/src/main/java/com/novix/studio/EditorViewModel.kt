package com.novix.studio

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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
    var textSize by mutableStateOf(100)
        private set
    var cropRatio by mutableStateOf("Original")
        private set
    var rotation by mutableStateOf(0)
        private set
    var isBusy by mutableStateOf(false)
        private set
    var lastSavedUri by mutableStateOf<Uri?>(null)
        private set
    var showOriginal by mutableStateOf(false)
        private set

    // v2: effects, retouch, stickers, frames, background swap, collage, templates
    var effect by mutableStateOf("None")
        private set
    var retouchSmooth by mutableStateOf(0)
        private set
    var retouchBrighten by mutableStateOf(0)
        private set
    var retouchDetail by mutableStateOf(0)
        private set
    var stickers by mutableStateOf(listOf<StickerState>())
        private set
    var frame by mutableStateOf("None")
        private set
    var blurStrength by mutableStateOf(0)
        private set
    var bgMode by mutableStateOf("None")
        private set
    var bgColor by mutableStateOf(Color.WHITE)
        private set
    var bgRemoved by mutableStateOf(false)
        private set
    var cutout by mutableStateOf<Bitmap?>(null)
        private set
    var collagePhotos by mutableStateOf(listOf<Bitmap>())
        private set
    var collageLayout by mutableStateOf("4")
        private set
    var collageGutter by mutableStateOf(Color.WHITE)
        private set
    var collageResult by mutableStateOf<Bitmap?>(null)
        private set
    var template by mutableStateOf("None")
        private set

    private var original: Bitmap? = null
    private var rendered: Bitmap? = null
    private var blurredOriginal: Bitmap? = null
    private var bgPhoto: Bitmap? = null

    val originalBitmap: Bitmap?
        get() = original

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
                resetEditState()
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

    private fun resetEditState() {
        settings = EditSettings()
        overlayText = ""
        textColor = -1
        textSize = 100
        cropRatio = "Original"
        rotation = 0
        showOriginal = false
        effect = "None"
        retouchSmooth = 0
        retouchBrighten = 0
        retouchDetail = 0
        stickers = listOf()
        frame = "None"
        blurStrength = 0
        bgMode = "None"
        bgColor = Color.WHITE
        bgRemoved = false
        cutout = null
        blurredOriginal = null
        bgPhoto = null
        template = "None"
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

    /**
     * Compose order: crop -> rotate -> base edits -> effect -> retouch ->
     * portrait blur -> background swap -> text -> stickers -> story frame -> frame.
     * (BG swap must come before text/stickers so they stay visible on top.)
     */
    private suspend fun renderPreview() {
        val src = original ?: return
        val result = withContext(Dispatchers.IO) {
            var bmp = cropToRatio(src, cropRatio)
            bmp = rotateBitmap(bmp, rotation)
            bmp = applyEdits(bmp, settings)
            bmp = applyEffect(bmp, effect)
            bmp = applyRetouch(bmp, retouchSmooth, retouchBrighten, retouchDetail)
            if (blurStrength > 0 && !(bgRemoved && bgMode != "None")) {
                bmp = portraitBlur(bmp, blurStrength)
            }
            if (bgRemoved && cutout != null && bgMode != "None") {
                val bgParam: Any? = when (bgMode) {
                    "Blur" -> blurredOriginal
                    "Photo" -> bgPhoto
                    "Colour" -> bgColor
                    else -> null
                }
                bmp = BgRemover.applyBackground(cutout!!, bgMode, bgParam)
            }
            bmp = drawTextOverlay(bmp, overlayText, textColor, textSize)
            bmp = drawStickers(bmp, stickers)
            if (template == "Story") bmp = drawStoryFrame(bmp, overlayText)
            bmp = drawFrame(bmp, frame)
            bmp
        }
        preview = result
        rendered = result
    }

    fun refreshPreview() {
        viewModelScope.launch { renderPreview() }
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

    fun updateTextSize(size: Int) {
        textSize = size
        viewModelScope.launch { renderPreview() }
    }

    fun updateCrop(ratio: String) {
        cropRatio = ratio
        viewModelScope.launch { renderPreview() }
    }

    fun rotate90() {
        rotation = (rotation + 90) % 360
        viewModelScope.launch { renderPreview() }
    }

    fun toggleOriginal() {
        showOriginal = !showOriginal
    }

    fun updateEffect(name: String) {
        effect = name
        viewModelScope.launch { renderPreview() }
    }

    fun updateRetouch(smooth: Int = retouchSmooth, brighten: Int = retouchBrighten, detail: Int = retouchDetail) {
        retouchSmooth = smooth
        retouchBrighten = brighten
        retouchDetail = detail
        viewModelScope.launch { renderPreview() }
    }

    fun addSticker(emoji: String) {
        stickers = stickers + StickerState(emoji, 0.4f, 0.4f, 0.16f)
        viewModelScope.launch { renderPreview() }
    }

    /** Move during drag: state only, no re-render (render on drag end). */
    fun moveSticker(index: Int, dx: Float, dy: Float) {
        if (index !in stickers.indices) return
        stickers = stickers.mapIndexed { idx, s ->
            if (idx == index) {
                s.copy(
                    x = (s.x + dx).coerceIn(0f, 0.9f),
                    y = (s.y + dy).coerceIn(0f, 0.9f)
                )
            } else s
        }
    }

    fun removeSticker(index: Int) {
        if (index !in stickers.indices) return
        stickers = stickers.filterIndexed { idx, _ -> idx != index }
        viewModelScope.launch { renderPreview() }
    }

    fun updateFrame(name: String) {
        frame = name
        viewModelScope.launch { renderPreview() }
    }

    fun updateBlurStrength(value: Int) {
        blurStrength = value
        viewModelScope.launch { renderPreview() }
    }

    fun startBgRemoval(context: Context) {
        val src = original ?: return
        isBusy = true
        status = "Background hataya ja raha hai... (pehli dafa thoda waqt lag sakta hai)"
        BgRemover.removeBackground(context, src) { cut, err ->
            isBusy = false
            if (cut != null) {
                cutout = cut
                bgRemoved = true
                bgMode = "Transparent"
                blurredOriginal = blurBitmap(src, 14)
                status = "Background hat gaya! Mode chunein. Edit badle to dobara remove karein."
                viewModelScope.launch { renderPreview() }
            } else {
                status = err ?: "Background remove nahi hua."
            }
        }
    }

    fun setBgMode(mode: String, color: Int? = null) {
        bgMode = mode
        if (color != null) bgColor = color
        viewModelScope.launch { renderPreview() }
    }

    fun pickBgPhoto(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bmp = context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
                if (bmp != null) {
                    bgPhoto = scaleDown(bmp)
                    bgMode = "Photo"
                    withContext(Dispatchers.Main) { refreshPreview() }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { status = "Background photo load nahi hui." }
            }
        }
    }

    fun applyTemplate(name: String) {
        template = name
        when (name) {
            "Story" -> {
                cropRatio = "9:16"
                frame = "None"
            }
            "Polaroid" -> {
                frame = "Polaroid"
                cropRatio = "4:5"
            }
            "Photo Dump" -> {
                // Navigation to Collage tab is handled by the screen.
            }
        }
        viewModelScope.launch { renderPreview() }
    }

    fun addCollagePhoto(context: Context, uri: Uri) {
        if (collagePhotos.size >= 4) {
            status = "Collage mein zyada se zyada 4 photos."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bmp = context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
                if (bmp != null) {
                    val scaled = scaleDown(bmp)
                    withContext(Dispatchers.Main) {
                        collagePhotos = collagePhotos + scaled
                        renderCollage()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { status = "Photo add nahi hui." }
            }
        }
    }

    fun removeCollagePhoto(index: Int) {
        if (index !in collagePhotos.indices) return
        collagePhotos = collagePhotos.filterIndexed { idx, _ -> idx != index }
        renderCollage()
    }

    fun clearCollage() {
        collagePhotos = listOf()
        collageResult = null
    }

    fun renderCollage(layout: String = collageLayout, gutterColor: Int = collageGutter) {
        collageLayout = layout
        collageGutter = gutterColor
        val photos = collagePhotos
        if (photos.isEmpty()) {
            collageResult = null
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result = buildCollage(photos, layout, gutterColor)
            withContext(Dispatchers.Main) { collageResult = result }
        }
    }

    fun autoEnhance() {
        settings = settings.copy(brightness = 104, contrast = 110, saturation = 115)
        viewModelScope.launch { renderPreview() }
        status = "Auto Enhance lag gaya."
    }

    fun resetEdits() {
        resetEditState()
        viewModelScope.launch { renderPreview() }
        status = "Edits reset ho gayin."
    }

    fun savePhoto(context: Context, pro: Boolean) {
        val bmp = rendered ?: return
        viewModelScope.launch(Dispatchers.IO) {
            isBusy = true
            status = "Photo save ho rahi hai..."
            val uri = if (bgRemoved && bgMode == "Transparent") {
                savePngToGallery(context, bmp)
            } else {
                saveToGallery(context, bmp, if (pro) 100 else 85)
            }
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

    fun saveCollage(context: Context, pro: Boolean) {
        val bmp = collageResult ?: return
        viewModelScope.launch(Dispatchers.IO) {
            isBusy = true
            status = "Collage save ho raha hai..."
            val uri = saveToGallery(context, bmp, if (pro) 100 else 85)
            withContext(Dispatchers.Main) {
                isBusy = false
                if (uri != null) {
                    lastSavedUri = uri
                    status = "Collage save ho gaya. Gallery mein dekhein."
                } else {
                    status = "Save nahi hua. Dobara try karein."
                }
            }
        }
    }

    fun sharePhoto(context: Context) {
        val bmp = rendered ?: return
        shareBitmap(context, bmp)
    }

    fun shareCollage(context: Context) {
        val bmp = collageResult ?: return
        shareBitmap(context, bmp)
    }

    private fun shareBitmap(context: Context, bmp: Bitmap) {
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
