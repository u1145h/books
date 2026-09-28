package com.u1145h.books

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.u1145h.books.ui.KavitaApp
import com.u1145h.books.ui.RootViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity architecture. All UI is Compose.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val rootViewModel: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !rootViewModel.isReady.value }
        super.onCreate(savedInstanceState)
        setContent {
            KavitaApp()
        }
    }
}
