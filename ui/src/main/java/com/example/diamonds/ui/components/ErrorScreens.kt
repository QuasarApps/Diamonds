package com.example.diamonds.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.diamonds.ui.R

/**
 * Full-screen generic error with a retry button.
 */
@Composable
fun GenericErrorScreen(
    message: String = stringResource(R.string.generic_error_message),
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚠️", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.oops), fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 15.sp
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth(0.6f)) {
            Text(stringResource(R.string.try_again))
        }
    }
}

/**
 * Full-screen "no internet" indicator with a retry button.
 */
@Composable
fun NoInternetScreen(
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📡", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.no_internet), fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.no_internet_detail),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 15.sp
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth(0.6f)) {
            Text(stringResource(R.string.retry))
        }
    }
}

/**
 * Full-screen "not found" / 404 screen with a back button.
 */
@Composable
fun NotFoundScreen(
    message: String = stringResource(R.string.not_found_message),
    onGoBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔍", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.not_found), fontWeight = FontWeight.Bold, fontSize = 22.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 15.sp
        )
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onGoBack, modifier = Modifier.fillMaxWidth(0.6f)) {
            Text(stringResource(R.string.go_back))
        }
    }
}

/**
 * A subtle top banner indicating the device is offline.
 * Use this inside a scaffold (e.g., as the first item) rather than a full-screen blocker,
 * since the app is offline-first and cached reads still work.
 */
@Composable
fun OfflineBanner() {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("📡", fontSize = 14.sp)
        Spacer(Modifier.padding(horizontal = 4.dp))
        Text(
            stringResource(R.string.offline_showing_cached),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
