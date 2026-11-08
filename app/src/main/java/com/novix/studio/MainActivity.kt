package com.novix.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = NovixGreen,
                    background = NovixBg,
                    surface = NovixCard
                )
            ) {
                val vm: EditorViewModel = viewModel()
                var tab by remember { mutableStateOf("Home") }
                var isPro by remember { mutableStateOf(ProManager.isPro(this)) }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(NovixBg)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (tab) {
                            "Home" -> HomeScreen(
                                vm,
                                onGoEditor = { tab = "Editor" },
                                onGoPro = { tab = "Pro" },
                                onGoCollage = { tab = "Collage" },
                                onGoAI = { tab = "AI" }
                            )
                            "Editor" -> EditorScreen(vm, isPro, onGoPro = { tab = "Pro" })
                            "Collage" -> CollageScreen(vm, isPro)
                            "AI" -> AiScreen(
                                vm,
                                isPro,
                                onGoPro = { tab = "Pro" },
                                onGoEditor = { tab = "Editor" },
                                onGoCollage = { tab = "Collage" }
                            )
                            "Pro" -> ProScreen(onProChanged = { isPro = ProManager.isPro(this@MainActivity) })
                        }
                    }
                    BottomNav(tab) { tab = it }
                }
            }
        }
    }
}
