package com.novix.studio

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun filterDotColor(name: String): Color {
    return when (name) {
        "Warm" -> Color(0xFFFFB74D)
        "Cool" -> Color(0xFF64B5F6)
        "Vivid" -> Color(0xFFFF7043)
        "Mono" -> Color(0xFF9E9E9E)
        "Sepia" -> Color(0xFFA1887F)
        "Fade" -> Color(0xFFB0BEC5)
        "Noir" -> Color.Black
        "Retro" -> Color(0xFFD7CCC8)
        "Vintage" -> Color(0xFFBCAAA4)
        "Cyberpunk" -> Color(0xFFE040FB)
        "Soft Glow" -> Color(0xFFFFF9C4)
        "Golden" -> Color(0xFFFFD54F)
        "Rose" -> Color(0xFFF48FB1)
        "Ocean" -> Color(0xFF4DD0E1)
        "Forest" -> Color(0xFF66BB6A)
        else -> NovixGreen
    }
}

@Composable
fun EditorScreen(vm: EditorViewModel, isPro: Boolean, onGoPro: () -> Unit) {
    val context = LocalContext.current
    var tool by remember { mutableStateOf("Adjust") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.loadPhoto(context, uri)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(
                onClick = { picker.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = NovixGreen, contentColor = Color.Black)
            ) { Text("Import", fontWeight = FontWeight.Bold) }
            OutlinedButton(onClick = { vm.resetEdits() }) { Text("Reset", color = NovixText) }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Preview with before/after compare + draggable sticker handles
        val bmp = if (vm.showOriginal) vm.originalBitmap else vm.preview
        val density = LocalDensity.current
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NovixCard),
            contentAlignment = Alignment.Center
        ) {
            if (bmp != null) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Edited photo",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Sticker drag handles (only in Stickers tool): a ring over each
                    // sticker — drag to move, tap ✕ to remove. The emoji itself is
                    // already drawn into the rendered bitmap.
                    if (tool == "Stickers" && !vm.showOriginal) {
                        for ((i, st) in vm.stickers.withIndex()) {
                            val handleSize = maxWidth * st.size
                            val wPx = with(density) { maxWidth.toPx() }
                            val hPx = with(density) { maxHeight.toPx() }
                            Box(
                                modifier = Modifier
                                    .offset(x = maxWidth * st.x, y = maxHeight * st.y)
                                    .size(handleSize)
                                    .border(1.dp, NovixGreen, RoundedCornerShape(8.dp))
                                    .pointerInput(i) {
                                        detectDragGestures(
                                            onDragEnd = { vm.refreshPreview() }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            if (wPx > 0 && hPx > 0) {
                                                vm.moveSticker(
                                                    i,
                                                    dragAmount.x / wPx,
                                                    dragAmount.y / hPx
                                                )
                                            }
                                        }
                                    }
                            ) {
                                Text(
                                    "✕",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .background(NovixGreen, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                        .clickable { vm.removeSticker(i) }
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No photo selected\nImport Photo se shuru karein",
                        color = NovixMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Before / After toggle
        if (vm.hasPhoto) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { if (vm.showOriginal) vm.toggleOriginal() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (vm.showOriginal) NovixGreen else NovixCard,
                        contentColor = if (vm.showOriginal) Color.Black else NovixText
                    ),
                    modifier = Modifier.weight(1f)
                ) { Text("Pehle", fontWeight = FontWeight.Bold) }
                Button(
                    onClick = { if (!vm.showOriginal) vm.toggleOriginal() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!vm.showOriginal) NovixGreen else NovixCard,
                        contentColor = if (!vm.showOriginal) Color.Black else NovixText
                    ),
                    modifier = Modifier.weight(1f)
                ) { Text("Ab", fontWeight = FontWeight.Bold) }
            }
        }
        Text(vm.status, color = NovixMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        if (vm.isBusy) {
            Text("Kaam ho raha hai...", color = NovixGreen, fontSize = 13.sp)
        }

        if (vm.hasPhoto) {
            Spacer(modifier = Modifier.height(8.dp))
            // Tool tabs
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (t in listOf("Adjust", "Filters", "Effects", "Text", "Stickers", "Crop", "Frames", "Blur")) {
                    val selected = tool == t
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) NovixGreen else NovixCard)
                            .clickable { tool = t }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Text(
                            t,
                            color = if (selected) Color.Black else NovixText,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            when (tool) {
                "Adjust" -> AdjustPanel(vm)
                "Filters" -> FiltersPanel(vm, isPro, onGoPro)
                "Effects" -> EffectsPanel(vm, isPro, onGoPro)
                "Text" -> TextPanel(vm)
                "Stickers" -> StickersPanel(vm)
                "Crop" -> CropPanel(vm)
                "Frames" -> FramesPanel(vm)
                "Blur" -> BlurPanel(vm)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { vm.autoEnhance() },
                    colors = ButtonDefaults.buttonColors(containerColor = NovixGreen, contentColor = Color.Black),
                    modifier = Modifier.weight(1f)
                ) { Text("Auto Enhance", fontWeight = FontWeight.Bold) }
                Button(
                    onClick = { vm.savePhoto(context, isPro) },
                    colors = ButtonDefaults.buttonColors(containerColor = NovixCard, contentColor = NovixText),
                    modifier = Modifier.weight(1f)
                ) { Text(if (isPro) "Save HD" else "Save", fontWeight = FontWeight.Bold) }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { vm.sharePhoto(context) },
                colors = ButtonDefaults.buttonColors(containerColor = NovixCard, contentColor = NovixText),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Share karein", fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AdjustPanel(vm: EditorViewModel) {
    SectionTitle("Roshni aur Rang")
    EditSlider("Brightness", vm.settings.brightness, 0, 200) {
        vm.updateSettings(vm.settings.copy(brightness = it))
    }
    EditSlider("Contrast", vm.settings.contrast, 0, 200) {
        vm.updateSettings(vm.settings.copy(contrast = it))
    }
    EditSlider("Saturation", vm.settings.saturation, 0, 200) {
        vm.updateSettings(vm.settings.copy(saturation = it))
    }
    EditSlider("Warmth", vm.settings.warmth, 0, 200) {
        vm.updateSettings(vm.settings.copy(warmth = it))
    }
    EditSlider("Highlights", vm.settings.highlights, 0, 200) {
        vm.updateSettings(vm.settings.copy(highlights = it))
    }
    EditSlider("Shadows", vm.settings.shadows, 0, 200) {
        vm.updateSettings(vm.settings.copy(shadows = it))
    }
    EditSlider("Sharpness", vm.settings.sharpness, 0, 100) {
        vm.updateSettings(vm.settings.copy(sharpness = it))
    }
    EditSlider("Vignette", vm.settings.vignette, 0, 100) {
        vm.updateSettings(vm.settings.copy(vignette = it))
    }
    EditSlider("Grayscale", vm.settings.grayscale, 0, 100) {
        vm.updateSettings(vm.settings.copy(grayscale = it))
    }
}

@Composable
fun FiltersPanel(vm: EditorViewModel, isPro: Boolean, onGoPro: () -> Unit) {
    val context = LocalContext.current
    SectionTitle("Filters — 16")
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (name in ALL_FILTERS) {
            val locked = name in PRO_FILTERS && !isPro
            val selected = vm.settings.filter == name
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(filterDotColor(name))
                        .border(
                            if (selected) 3.dp else 1.dp,
                            if (selected) NovixGreen else Color(0xFF555555),
                            RoundedCornerShape(24.dp)
                        )
                        .clickable {
                            if (locked) {
                                Toast.makeText(context, "Ye filter PRO mein hai", Toast.LENGTH_SHORT).show()
                                onGoPro()
                            } else {
                                vm.updateSettings(vm.settings.copy(filter = name))
                            }
                        }
                ) { }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (locked) "$name 👑" else name,
                    color = if (selected) NovixGreen else NovixText,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun EffectsPanel(vm: EditorViewModel, isPro: Boolean, onGoPro: () -> Unit) {
    val context = LocalContext.current
    SectionTitle("AI Effects")
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (name in EFFECTS) {
            val locked = name in PRO_EFFECTS && !isPro
            val selected = vm.effect == name
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) NovixGreen else NovixCard)
                    .border(1.dp, if (selected) NovixGreen else Color(0xFF333333), RoundedCornerShape(20.dp))
                    .clickable {
                        if (locked) {
                            Toast.makeText(context, "Ye effect PRO mein hai", Toast.LENGTH_SHORT).show()
                            onGoPro()
                        } else {
                            vm.updateEffect(name)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    if (locked) "$name  •  PRO" else name,
                    color = if (selected) Color.Black else NovixText,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
    Text("Cartoon: rang 4 level mein • Anime: tez rang • Oil Art: painting jaisa", color = NovixMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun TextPanel(vm: EditorViewModel) {
    SectionTitle("Photo par Text")
    var text by remember { mutableStateOf(vm.overlayText) }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it
            vm.updateText(it)
        },
        label = { Text("Text likhein", color = NovixMuted) },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((label, color) in listOf(
            "White" to Color.White,
            "Yellow" to Color.Yellow,
            "Cyan" to Color.Cyan,
            "Pink" to Color(0xFFFF4D8D),
            "Green" to NovixGreen
        )) {
            val selected = vm.textColor == color.toArgb()
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(color)
                    .border(
                        if (selected) 3.dp else 1.dp,
                        if (selected) NovixGreen else Color(0xFF555555),
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { vm.updateTextColor(color.toArgb()) }
            ) { }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    EditSlider("Text Size", vm.textSize, 50, 200) { vm.updateTextSize(it) }
}

@Composable
fun StickersPanel(vm: EditorViewModel) {
    SectionTitle("Stickers — tap karein, photo par drag karein")
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (emoji in STICKER_EMOJIS) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(NovixCard)
                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp))
                    .clickable { vm.addSticker(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 26.sp)
            }
        }
    }
    if (vm.stickers.isNotEmpty()) {
        Text(
            "${vm.stickers.size} sticker lage hain — photo par green box ko drag karein, ✕ se hatayein.",
            color = NovixMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun CropPanel(vm: EditorViewModel) {
    SectionTitle("Crop")
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (ratio in CROP_RATIOS) {
            val selected = vm.cropRatio == ratio
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) NovixGreen else NovixCard)
                    .clickable { vm.updateCrop(ratio) }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    ratio,
                    color = if (selected) Color.Black else NovixText,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = { vm.rotate90() },
        colors = ButtonDefaults.buttonColors(containerColor = NovixCard, contentColor = NovixText),
        modifier = Modifier.fillMaxWidth()
    ) { Text("🔄  Rotate 90°", fontWeight = FontWeight.Bold) }
    Text("Abhi rotation: ${vm.rotation}°", color = NovixMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun FramesPanel(vm: EditorViewModel) {
    SectionTitle("Frames")
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (name in FRAMES) {
            val selected = vm.frame == name
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) NovixGreen else NovixCard)
                    .border(1.dp, if (selected) NovixGreen else Color(0xFF333333), RoundedCornerShape(20.dp))
                    .clickable { vm.updateFrame(name) }
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    name,
                    color = if (selected) Color.Black else NovixText,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
    Text("Polaroid: safed border • Classic: patla kala border • Neon: lime glow", color = NovixMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
}

@Composable
fun BlurPanel(vm: EditorViewModel) {
    val context = LocalContext.current
    SectionTitle("Blur")
    EditSlider("Portrait Blur", vm.blurStrength, 0, 100) { vm.updateBlurStrength(it) }
    Text("Background blur hoga, beech wala hissa saaf rahe ga.", color = NovixMuted, fontSize = 12.sp)
    Spacer(modifier = Modifier.height(12.dp))
    Button(
        onClick = {
            if (!vm.bgRemoved) {
                Toast.makeText(context, "Pehle AI tab se background hatayein", Toast.LENGTH_SHORT).show()
            } else {
                vm.setBgMode("Blur")
                Toast.makeText(context, "Background blur lag gaya", Toast.LENGTH_SHORT).show()
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = NovixCard, contentColor = NovixText),
        modifier = Modifier.fillMaxWidth()
    ) { Text("BG Blur (Background Remover ke baad)", fontWeight = FontWeight.Bold) }
}
