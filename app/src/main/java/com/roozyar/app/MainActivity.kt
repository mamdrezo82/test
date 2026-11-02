package com.roozyar.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import android.app.AlarmManager

class MainActivity : ComponentActivity() {
    private val vm: VM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        setContent {
            val sysDark = isSystemInDarkTheme()
            var dark by remember { mutableStateOf(prefs.getBoolean("dark", sysDark)) }
            var accent by remember { mutableIntStateOf(prefs.getInt("accent", 0)) }

            val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= 33) perm.launch(Manifest.permission.POST_NOTIFICATIONS)
                if (Build.VERSION.SDK_INT >= 31 && !prefs.getBoolean("askedAlarm", false) &&
                    !getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
                ) {
                    prefs.edit().putBoolean("askedAlarm", true).apply()
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
                }
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                RoozTheme(dark, accent) {
                    RoozScreen(vm, dark, { dark = it; prefs.edit().putBoolean("dark", it).apply() },
                        accent, { accent = it; prefs.edit().putInt("accent", it).apply() })
                }
            }
        }
    }
}
