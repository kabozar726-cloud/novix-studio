package com.novix.studio

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
fun CollageScreen(vm: EditorViewModel, isPro: Boolean) {
    val context = LocalContext.current
    var layout by remember { mutableStateOf("4") }
    var gutter by remember { mutableStateOf(Color.White) }
    val multiPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val room = 4 - vm.collagePhotos.size
        if (room <= 0) {
            Toast.makeText(context, "Collage mein zyada se zyada 4 photos.", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        uris.take(room).forEach { vm.addCollagePhoto(context, it) }
    }
    LaunchedEffect(layout, gutter, vm.collagePhotos) {
        vm.renderCollage(layout, gutter.toArgb())
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Collage Maker 🖼️", color = NovixText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("4 photos tak chunein, layout badlein, save karein.", color = NovixMuted, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))

        // Collage preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(NovixCard),
            contentAlignment = Alignment.Center
        ) {
            val result = vm.collageResult
            if (result != null) {
                Image(
                    bitmap = result.asImageBitmap(),
                    contentDescription = "Collage preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    "Abhi koi photo nahi\nNeeche se photos chunein",
                    color = NovixMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Chosen photo thumbnails
        if (vm.collagePhotos.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                vm.collagePhotos.forEachIndexed { i, bmp ->
                    Box {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Collage photo ${i + 1}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Text(
                            "✕",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .background(Color(0xFFCC0000), RoundedCornerShape(8.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                .clickable { vm.removeCollagePhoto(i) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = { multiPicker.launch("image/*") },
            colors = ButtonDefaults.buttonColors(containerColor = NovixGreen, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("＋  Photos chunein (${vm.collagePhotos.size}/4)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        if (vm.collagePhotos.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { vm.clearCollage() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Saaf karein", color = NovixText) }
        }

        SectionTitle("Layout")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val layouts = listOf(
                "2V" to "2 Upar-Neeche",
                "2H" to "2 Side by Side",
                "3" to "3 Grid",
                "4" to "4 Grid"
            )
            for ((key, label) in layouts) {
                val selected = layout == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) NovixGreen else NovixCard)
                        .clickable { layout = key }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        label,
                        color = if (selected) Color.Black else NovixText,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        SectionTitle("Gutter Rang")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((label, color) in listOf("White" to Color.White, "Black" to Color.Black)) {
                val selected = gutter == color
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) NovixGreen else NovixCard)
                        .border(1.dp, if (selected) NovixGreen else Color(0xFF333333), RoundedCornerShape(20.dp))
                        .clickable { gutter = color }
                        .padding(horizontal = 16.dp, vertical = 9.dp)
                ) {
                    Text(
                        label,
                        color = if (selected) Color.Black else NovixText,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(vm.status, color = NovixMuted, fontSize = 13.sp)
        if (vm.isBusy) {
            Text("Kaam ho raha hai...", color = NovixGreen, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { vm.saveCollage(context, isPro) },
                colors = ButtonDefaults.buttonColors(containerColor = NovixGreen, contentColor = Color.Black),
                modifier = Modifier.weight(1f),
                enabled = vm.collageResult != null
            ) { Text(if (isPro) "Save HD" else "Save", fontWeight = FontWeight.Bold) }
            Button(
                onClick = { vm.shareCollage(context) },
                colors = ButtonDefaults.buttonColors(containerColor = NovixCard, contentColor = NovixText),
                modifier = Modifier.weight(1f),
                enabled = vm.collageResult != null
            ) { Text("Share", fontWeight = FontWeight.Bold) }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
