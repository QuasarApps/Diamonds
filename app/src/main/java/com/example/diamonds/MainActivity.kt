package com.example.diamonds

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.diamonds.ui.navigation.DiamondsNavHost
import com.example.diamonds.ui.theme.DiamondsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Switch from the splash launch theme to the real app theme before
        // Compose draws, so system bars adopt the correct colours.
        setTheme(R.style.Theme_Diamonds)

        enableEdgeToEdge()
        setContent {
            DiamondsTheme {
                DiamondsNavHost()
            }
        }
    }
}
