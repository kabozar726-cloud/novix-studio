package com.novix.studio

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AiScreen(
    vm: EditorViewModel,
    isPro: Boolean,
    onGoPro: () -> Unit,
    onGoEditor: () -> Unit,
    onGoCollage: () -> Unit
) {
    val context = LocalContext.current
    val bgPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.pickBgPhoto(context, uri)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("AI Tools ✨", color = NovixText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("AI se photo ko behtar banayein — sab on-device", color = NovixMuted, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(16.dp))

        AiCard(
            title = "AI Enhance",
            desc = "Aik tap mein photo behtar — roshni, rang aur contrast khud theek ho jate hain.",
            buttonText = "Enhance karein",
            enabled = true
        ) {
            if (!vm.hasPhoto) {
                Toast.makeText(context, "Pehle Editor mein photo import karein", Toast.LENGTH_SHORT).show()
            } else {
                vm.autoEnhance()
                Toast.makeText(context, "AI Enhance lag gaya", Toast.LENGTH_SHORT).show()
            }
        }

        // Background Remover — REAL, ML Kit on-device
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NovixCard)
                .padding(16.dp)
        ) {
            Column {
                Text("Background Remover ✂️", color = NovixText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Insaan ko photo se alag karein — asli AI, phone par hi chalta hai. Pehli dafa model download ke liye internet chahiye.",
                    color = NovixMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )
                if (vm.hasPhoto && vm.preview != null) {
                    Image(
                        bitmap = vm.preview!!.asImageBitmap(),
                        contentDescription = "BG remover preview",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF111111))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
                if (!vm.bgRemoved) {
                    Button(
                        onClick = {
                            if (!vm.hasPhoto) {
                                Toast.makeText(context, "Pehle Editor mein photo import karein", Toast.LENGTH_SHORT).show()
                            } else {
                                vm.startBgRemoval(context)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NovixGreen, contentColor = Color.Black)
                    ) { Text("Background hatayein", fontWeight = FontWeight.Bold) }
                } else {
                    Text("Background hat gaya! Neeche mode chunein:", color = NovixGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (mode in listOf("Transparent", "Blur", "Colour", "Photo")) {
                            val selected = vm.bgMode == mode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selected) NovixGreen else Color(0xFF2A2A2A))
                                    .clickable {
                                        if (mode == "Photo") {
                                            bgPicker.launch("image/*")
                                        } else {
                                            vm.setBgMode(mode)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    mode,
                                    color = if (selected) Color.Black else NovixText,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    if (vm.bgMode == "Colour") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for ((label, color) in listOf(
                                "White" to Color.White,
                                "Black" to Color.Black,
                                "Lime" to NovixGreen,
                                "Pink" to Color(0xFFFF4D8D),
                                "Blue" to Color(0xFF2196F3)
                            )) {
                                val selected = vm.bgColor == color.toArgb()
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
                                        .clickable { vm.setBgMode("Colour", color.toArgb()) }
                                ) { }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { vm.savePhoto(context, isPro) },
                        colors = ButtonDefaults.buttonColors(containerColor = NovixGreen, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (vm.bgMode == "Transparent") "Save PNG (Transparent)" else "Save karein", fontWeight = FontWeight.Bold) }
                }
                Text(vm.status, color = NovixMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        }

        // AI Retouch — local sliders
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(NovixCard)
                .padding(16.dp)
        ) {
            Column {
                Text("AI Retouch 💄", color = NovixText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Skin smooth, roshni aur detail — sab phone par, bina internet.",
                    color = NovixMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )
                if (!vm.hasPhoto) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Pehle Editor mein photo import karein", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NovixGreen, contentColor = Color.Black)
                    ) { Text("Photo import karein", fontWeight = FontWeight.Bold) }
                } else {
                    EditSlider("Smooth", vm.retouchSmooth, 0, 100) {
                        vm.updateRetouch(smooth = it)
                    }
                    EditSlider("Brighten", vm.retouchBrighten, 0, 100) {
                        vm.updateRetouch(brighten = it)
                    }
                    EditSlider("Detail", vm.retouchDetail, 0, 100) {
                        vm.updateRetouch(detail = it)
                    }
                }
            }
        }

        AiCard(
            title = "AI Avatar",
            desc = "Apni photo se AI avatar banayein. PRO + AI connection jald aa raha hai.",
            buttonText = "PRO lein",
            enabled = true
        ) { onGoPro() }

        AiCard(
            title = "Collage Maker",
            desc = "Kai photos aik frame mein — 2, 3 ya 4 photo layouts.",
            buttonText = "Collage banayein",
            enabled = true
        ) { onGoCollage() }

        AiCard(
            title = "Text & Stickers",
            desc = "Photo par text aur stickers lagayein — Editor mein tayyar hai.",
            buttonText = "Editor mein jayein",
            enabled = true
        ) { onGoEditor() }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Note: AI Avatar ke liye AI connection chahiye — agle version mein aaye ga. Baaki sab tools on-device kaam karte hain.",
            color = NovixMuted,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun AiCard(title: String, desc: String, buttonText: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(NovixCard)
            .padding(16.dp)
    ) {
        Column {
            Text(title, color = NovixText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(desc, color = NovixMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
            Button(
                onClick = onClick,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(containerColor = NovixGreen, contentColor = Color.Black)
            ) { Text(buttonText, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun ProScreen(onProChanged: () -> Unit) {
    val context = LocalContext.current
    var selectedPlan by remember { mutableStateOf("Monthly") }
    var code by remember { mutableStateOf("") }
    var unlockMsg by remember { mutableStateOf("") }
    var isProNow by remember { mutableStateOf(ProManager.isPro(context)) }
    val plans = listOf(
        Triple("Monthly", "Rs 100", "Har mahina"),
        Triple("Yearly", "Rs 1200", "Har saal — 2 mahine muft jaisa"),
        Triple("Lifetime", "Rs 5000", "Aik dafa — hamesha ke liye")
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("NOVIX PRO", color = NovixGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (isProNow) {
            Text("PRO ACTIVE hai — shukriya!", color = NovixGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        } else {
            Text("FREE use kar rahe hain — PRO se zyada payein", color = NovixMuted, fontSize = 14.sp)
        }
        SectionTitle("Plan chunein")
        for ((key, price, note) in plans) {
            val selected = selectedPlan == key
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (selected) Color(0xFF24330A) else NovixCard)
                    .border(1.dp, if (selected) NovixGreen else Color(0xFF333333), RoundedCornerShape(12.dp))
                    .clickable { selectedPlan = key }
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(key, color = NovixText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(note, color = NovixMuted, fontSize = 13.sp)
                    }
                    Text(price, color = NovixGreen, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            }
        }
        SectionTitle("Payment — Easypaisa")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NovixCard)
                .padding(16.dp)
        ) {
            Column {
                Text("Easypaisa Number", color = NovixMuted, fontSize = 13.sp)
                Text(EASYPAISA_NUMBER, color = NovixText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Naam: $EASYPAISA_NAME", color = NovixText, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(onClick = { copyText(context, "Easypaisa number", EASYPAISA_NUMBER) }) {
                    Text("Number Copy karein", color = NovixText)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("Steps:", color = NovixText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text("1. Easypaisa app se upar wale number par payment bhejein.", color = NovixText, fontSize = 14.sp)
        Text("2. Neeche WhatsApp par payment ka screenshot + Transaction ID bhejein.", color = NovixText, fontSize = 14.sp)
        Text("3. Confirm hote hi aap ko unlock code mile ga — wo neeche likh kar PRO on karein.", color = NovixText, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = {
                val planPrice = plans.first { it.first == selectedPlan }.second
                openWhatsApp(
                    context, WHATSAPP_1,
                    "Assalam o Alaikum, maine NOVIX STUDIO PRO ($selectedPlan - $planPrice) ke liye Easypaisa payment bhej di hai. Transaction ID: "
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = NovixGreen, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text("WhatsApp par bhejein ($WHATSAPP_1)", fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                val planPrice = plans.first { it.first == selectedPlan }.second
                openWhatsApp(
                    context, WHATSAPP_2,
                    "Assalam o Alaikum, maine NOVIX STUDIO PRO ($selectedPlan - $planPrice) ke liye Easypaisa payment bhej di hai. Transaction ID: "
                )
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text("WhatsApp par bhejein ($WHATSAPP_2)", color = NovixText) }
        SectionTitle("Unlock Code")
        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("Code likhein", color = NovixMuted) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (ProManager.unlock(context, code)) {
                    isProNow = true
                    unlockMsg = "PRO on ho gaya! Shukriya."
                    onProChanged()
                } else {
                    unlockMsg = "Code ghalat hai. Sahi code WhatsApp par mile ga."
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = NovixGreen, contentColor = Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Unlock karein", fontWeight = FontWeight.Bold) }
        if (unlockMsg.isNotBlank()) {
            Text(unlockMsg, color = NovixGreen, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
        }
        SectionTitle("Madad chahiye?")
        Text("WhatsApp: $WHATSAPP_1", color = NovixText, fontSize = 14.sp)
        Text("WhatsApp: $WHATSAPP_2", color = NovixText, fontSize = 14.sp)
        Text("Novix Studio v2.0.0", color = NovixMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(16.dp))
    }
}
