package com.novix.studio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorScreen(vm: EditorViewModel, isPro: Boolean, onGoPro: () -> Unit) {
    val context = LocalContext.current
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NovixCard),
            contentAlignment = Alignment.Center
        ) {
            val bmp = vm.preview
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Edited photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("No photo selected\nImport Photo se shuru karein", color = NovixMuted, textAlign = TextAlign.Center)
            }
        }
        Text(vm.status, color = NovixMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        if (vm.isBusy) {
            Text("Kaam ho raha hai...", color = NovixGreen, fontSize = 13.sp)
        }
        if (vm.hasPhoto) {
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
            EditSlider("Grayscale", vm.settings.grayscale, 0, 100) {
                vm.updateSettings(vm.settings.copy(grayscale = it))
            }
            SectionTitle("Filters")
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (name in ALL_FILTERS) {
                    val locked = name in PRO_FILTERS && !isPro
                    val selected = vm.settings.filter == name
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) NovixGreen else NovixCard)
                            .border(1.dp, if (selected) NovixGreen else Color(0xFF333333), RoundedCornerShape(20.dp))
                            .clickable {
                                if (locked) {
                                    Toast.makeText(context, "Ye filter PRO mein hai", Toast.LENGTH_SHORT).show()
                                    onGoPro()
                                } else {
                                    vm.updateSettings(vm.settings.copy(filter = name))
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
