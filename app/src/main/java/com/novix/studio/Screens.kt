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

val NovixGreen = Color(0xFFA3E635)
val NovixBg = Color(0xFF0D0D0D)
val NovixCard = Color(0xFF1A1A1A)
val NovixText = Color(0xFFF5F5F5)
val NovixMuted = Color(0xFF9CA3AF)

const val EASYPAISA_NUMBER = "03231925745"
const val EASYPAISA_NAME = "Zahid Khan"
const val WHATSAPP_1 = "03231925745"
const val WHATSAPP_2 = "03155987970"

val FREE_FILTERS = listOf("Normal", "Warm", "Cool", "Vivid")
val PRO_FILTERS = listOf("Mono", "Sepia", "Fade", "Noir")
val ALL_FILTERS = FREE_FILTERS + PRO_FILTERS
val CROP_RATIOS = listOf("Original", "1:1", "9:16", "4:5", "3:4", "16:9")

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
fun HomeScreen(vm: EditorViewModel, onGoEditor: () -> Unit, onGoPro: () -> Unit) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.loadPhoto(context, uri)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("NOVIX STUDIO", color = NovixGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Your Creative AI Workspace", color = NovixMuted, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Make every photo look extraordinary.",
            color = NovixText,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = { picker.launch("image/*") },
            colors = ButtonDefaults.buttonColors(containerColor = NovixGreen, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("+  IMPORT PHOTO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (vm.hasPhoto) {
            Button(
                onClick = onGoEditor,
                colors = ButtonDefaults.buttonColors(containerColor = NovixCard, contentColor = NovixText),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Editor mein jayein", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Text(vm.status, color = NovixMuted, fontSize = 14.sp)
        if (vm.isBusy) {
            Text("Kaam ho raha hai...", color = NovixGreen, fontSize = 14.sp)
        }
        SectionTitle("FREE mein kya hai")
        Text("• Brightness, Contrast, Saturation, Grayscale sliders", color = NovixText, fontSize = 14.sp)
        Text("• 4 filters: Normal, Warm, Cool, Vivid", color = NovixText, fontSize = 14.sp)
        Text("• Crop: 1:1, 9:16, 4:5, 3:4, 16:9", color = NovixText, fontSize = 14.sp)
        Text("• Photo par text likhna", color = NovixText, fontSize = 14.sp)
        Text("• Auto Enhance (aik tap mein behtar photo)", color = NovixText, fontSize = 14.sp)
        Text("• Gallery mein save + share", color = NovixText, fontSize = 14.sp)
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
                Text("NOVIX PRO", color = NovixGreen, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Rs 100 / mahina  •  Rs 1200 / saal  •  Rs 5000 / lifetime", color = NovixText, fontSize = 14.sp)
                Text("HD save, saare filters, AI tools. Tap kar ke dekhein.", color = NovixMuted, fontSize = 13.sp)
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
        for (tab in listOf("Home", "Editor", "AI", "Pro")) {
            val selected = current == tab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) NovixGreen else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    tab,
                    color = if (selected) Color.Black else NovixText,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
