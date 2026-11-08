package com.novix.studio

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val NovixGreen = Color(0xFFA3E635)
val NovixBg = Color(0xFF0D0D0D)
val NovixCard = Color(0xFF1A1A1A)
val NovixText = Color(0xFFF5F5F5)
val NovixMuted = Color(0xFF9CA3AF)

const val EASYPAISA_NUMBER = "03231925745"
const val EASYPAISA_NAME = "Zahid Khan"
const val WHATSAPP_1 = "03231925745"
const val WHATSAPP_2 = "03155987970"

val FREE_FILTERS = listOf("Normal", "Warm", "Cool", "Vivid", "Retro", "Golden", "Ocean", "Soft Glow")
val PRO_FILTERS = listOf("Mono", "Sepia", "Fade", "Noir", "Vintage", "Cyberpunk", "Rose", "Forest")
val ALL_FILTERS = FREE_FILTERS + PRO_FILTERS
val CROP_RATIOS = listOf("Original", "1:1", "9:16", "4:5", "3:4", "16:9")
val EFFECTS = listOf("None", "Cartoon", "Anime", "Oil Art", "Vintage", "Cyberpunk")
val PRO_EFFECTS = listOf("Cartoon", "Anime")
val STICKER_EMOJIS = listOf("😍", "🔥", "✨", "🌸", "👑", "😎", "❤️", "⭐", "🎉")
val FRAMES = listOf("None", "Polaroid", "Classic", "Neon")
val TEMPLATES = listOf("Story", "Polaroid", "Photo Dump")

fun openWhatsApp(context: Context, number: String, message: String) {
    val digits = number.filter { it.isDigit() }
    val intl = if (digits.startsWith("0")) "92" + digits.substring(1) else digits
    val url = "https://wa.me/$intl?text=" + Uri.encode(message)
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    context.startActivity(intent)
}

fun copyText(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copy ho gaya", Toast.LENGTH_SHORT).show()
}

fun loadRecentPhotos(context: Context, scope: CoroutineScope, onLoaded: (List<Pair<Uri, Bitmap>>) -> Unit) {
    scope.launch(Dispatchers.IO) {
        val list = mutableListOf<Pair<Uri, Bitmap>>()
        try {
            val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Images.Media._ID)
            val sort = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            context.contentResolver.query(uri, projection, null, null, sort)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                var count = 0
                while (cursor.moveToNext() && count < 12) {
                    val id = cursor.getLong(idCol)
                    val imgUri = ContentUris.withAppendedId(uri, id)
                    try {
                        val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
                        context.contentResolver.openInputStream(imgUri)?.use { input ->
                            val bmp = BitmapFactory.decodeStream(input, null, opts)
                            if (bmp != null) {
                                list.add(imgUri to bmp)
                                count++
                            }
                        }
                    } catch (e: Exception) {
                        // Skip unreadable images.
                    }
                }
            }
        } catch (e: Exception) {
            // MediaStore unavailable — section stays hidden.
        }
        withContext(Dispatchers.Main) { onLoaded(list) }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        color = NovixText,
        fontSize = 17.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

@Composable
fun HomeScreen(
    vm: EditorViewModel,
    onGoEditor: () -> Unit,
    onGoPro: () -> Unit,
    onGoCollage: () -> Unit,
    onGoAI: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var recent by remember { mutableStateOf(listOf<Pair<Uri, Bitmap>>()) }
    var permissionGranted by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) loadRecentPhotos(context, scope) { recent = it }
    }
    LaunchedEffect(Unit) {
        val perm = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val has = ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        if (has) {
            permissionGranted = true
            loadRecentPhotos(context, scope) { recent = it }
        } else {
            permissionLauncher.launch(perm)
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            vm.loadPhoto(context, uri)
            onGoEditor()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Hero import card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(NovixGreen)
                .padding(20.dp)
        ) {
            Column {
                Text("NOVIX STUDIO ✨", color = Color.Black, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("Your Creative AI Workspace", color = Color(0xFF1A2E05), fontSize = 15.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Make every photo look extraordinary.", color = Color.Black, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { picker.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = NovixGreen),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("📷  IMPORT PHOTO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                if (vm.hasPhoto) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onGoEditor,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Editor mein jayein →", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Text(vm.status, color = NovixMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 10.dp))
        if (vm.isBusy) {
            Text("Kaam ho raha hai...", color = NovixGreen, fontSize = 14.sp)
        }

        // Tools grid (4 x 2)
        SectionTitle("Tools")
        val tools = listOf(
            Triple("🎨", "Editor", onGoEditor),
            Triple("🌈", "Filters", onGoEditor),
            Triple("✨", "AI", onGoAI),
            Triple("🖼️", "Collage", onGoCollage),
            Triple("📋", "Templates", onGoAI),
            Triple("💄", "Retouch", onGoAI),
            Triple("✂️", "BG Remover", onGoAI),
            Triple("👑", "PRO", onGoPro)
        )
        for (row in tools.chunked(4)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for ((emoji, label, action) in row) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(NovixCard)
                            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(14.dp))
                            .clickable { action() }
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(emoji, fontSize = 26.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(label, color = NovixText, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        // Templates row
        SectionTitle("Templates — aik tap mein tayyar")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val templateInfo = listOf(
                Triple("Story", "📱", "9:16 • lime frame"),
                Triple("Polaroid", "🖼️", "Classic white frame"),
                Triple("Photo Dump", "🗂️", "4 photos • 2x2 grid")
            )
            for ((name, emoji, desc) in templateInfo) {
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(NovixCard)
                        .border(1.dp, NovixGreen, RoundedCornerShape(14.dp))
                        .clickable {
                            if (!vm.hasPhoto) {
                                Toast.makeText(context, "Pehle photo import karein", Toast.LENGTH_SHORT).show()
                            } else if (name == "Photo Dump") {
                                onGoCollage()
                            } else {
                                vm.applyTemplate(name)
                                onGoEditor()
                            }
                        }
                        .padding(14.dp)
                ) {
                    Column {
                        Text(emoji, fontSize = 28.sp)
                        Text(name, color = NovixText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(desc, color = NovixMuted, fontSize = 12.sp)
                    }
                }
            }
        }

        // Recent photos grid (hidden if permission denied)
        if (permissionGranted && recent.isNotEmpty()) {
            SectionTitle("Recent Photos")
            for (row in recent.chunked(3)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for ((uri, bmp) in row) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Recent photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    vm.loadPhoto(context, uri)
                                    onGoEditor()
                                }
                        )
                    }
                    // Fill the row if fewer than 3 items.
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // PRO banner
        SectionTitle("PRO")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NovixCard)
                .border(1.dp, NovixGreen, RoundedCornerShape(12.dp))
                .clickable { onGoPro() }
                .padding(16.dp)
        ) {
            Column {
                Text("NOVIX PRO 👑", color = NovixGreen, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Rs 100 / mahina  •  Rs 1200 / saal  •  Rs 5000 / lifetime", color = NovixText, fontSize = 14.sp)
                Text("HD save, saare filters, AI effects, BG remover. Tap kar ke dekhein.", color = NovixMuted, fontSize = 13.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun EditSlider(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = NovixText, fontSize = 14.sp)
            Text("$value%", color = NovixMuted, fontSize = 14.sp)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = NovixGreen,
                activeTrackColor = NovixGreen,
                inactiveTrackColor = Color(0xFF333333)
            )
        )
    }
}

@Composable
fun BottomNav(current: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NovixCard)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        for (tab in listOf("Home", "Editor", "Collage", "AI", "Pro")) {
            val selected = current == tab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) NovixGreen else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Text(
                    tab,
                    color = if (selected) Color.Black else NovixText,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
        }
    }
}
