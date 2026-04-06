package com.example.diamonds.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.diamonds.ui.navigation.Screen

/** Deep-navy brand background, matches Theme.Diamonds.Splash windowBackground. */
private val SplashBackground = Color(0xFF0D1B2A)

/** Diamond / cyan accent. */
private val DiamondAccent = Color(0xFF00C4CC)

@Composable
fun SplashScreen(
    onNavigateTo: (Screen) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val destination by viewModel.destination.collectAsState()

    // Kick off navigation as soon as the VM signals a destination
    LaunchedEffect(destination) {
        destination?.let { onNavigateTo(it) }
    }

    // ── Animations ──────────────────────────────────────────────────────────
    val logoScale = remember { Animatable(0.3f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val taglineAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 1. Pop logo in
        logoScale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        logoAlpha.animateTo(1f, animationSpec = tween(400))

        // 2. Fade app name in
        textAlpha.animateTo(1f, animationSpec = tween(400))

        // 3. Fade tagline in
        taglineAlpha.animateTo(1f, animationSpec = tween(400))
    }

    // ── UI ──────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Diamond logo (Unicode ◆ as a stand-in until a real vector is added)
            Text(
                text = "◆",
                color = DiamondAccent,
                fontSize = 80.sp,
                modifier = Modifier
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App name
            Text(
                text = "Diamonds",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Your home, sparkling clean",
                color = DiamondAccent.copy(alpha = 0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha.value)
            )
        }
    }
}
