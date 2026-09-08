package com.ta.konstruksi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.ta.konstruksi.presentation.navigation.AppNavigation
import com.ta.konstruksi.presentation.ui.theme.KonstruksiTheme
import com.ta.konstruksi.sync.SyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var syncScheduler: SyncScheduler

    // Sinyal untuk membuka layar Transfer Masuk saat notifikasi diketuk (dibaca AppNavigation).
    private var openTransferMasuk by mutableStateOf(false)

    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* hasil diabaikan */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mintaIzinNotifikasi()
        openTransferMasuk = intent?.getBooleanExtra(EXTRA_OPEN_TRANSFER_MASUK, false) == true
        setContent {
            KonstruksiTheme {
                AppNavigation(
                    openTransferMasuk = openTransferMasuk,
                    onOpenTransferMasukConsumed = { openTransferMasuk = false }
                )
            }
        }
    }

    // Notifikasi diketuk saat aplikasi sudah berjalan (SINGLE_TOP) — perbarui sinyal.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_TRANSFER_MASUK, false)) openTransferMasuk = true
    }

    // Pemicu sinkronisasi sekali-jalan tiap aplikasi kembali ke latar depan (A2 #3) —
    // mempersingkat jeda terlihatnya transfer masuk tanpa menunggu siklus periodik 15 menit.
    override fun onStart() {
        super.onStart()
        syncScheduler.requestSync()
    }

    private fun mintaIzinNotifikasi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!granted) requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_OPEN_TRANSFER_MASUK = "open_transfer_masuk"
    }
}
