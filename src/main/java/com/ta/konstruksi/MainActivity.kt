package com.ta.konstruksi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ta.konstruksi.presentation.navigation.AppNavigation
import com.ta.konstruksi.presentation.ui.theme.KonstruksiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KonstruksiTheme {
                AppNavigation()
            }
        }
    }
}
