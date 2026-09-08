package com.ta.konstruksi.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ta.konstruksi.MainActivity
import com.ta.konstruksi.R
import com.ta.konstruksi.domain.model.TransferProyek
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notifikasi lokal "aset masuk" — dipicu dari [com.ta.konstruksi.sync.worker.SyncWorker] setelah
 * sinkronisasi berhasil menarik transfer IN_TRANSIT baru menuju proyek aktif. TANPA FCM/Cloud
 * Function (sesuai batasan BaaS penelitian): murni notifikasi sistem dari hasil delta sync.
 *
 * Anti-duplikat: id transfer yang sudah pernah dinotifikasi disimpan di SharedPreferences, sehingga
 * siklus sync berikutnya tidak memberi notifikasi ulang untuk transfer yang sama.
 */
@Singleton
class TransferNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Transfer Masuk",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Pemberitahuan saat ada aset dikirim menuju proyek Anda." }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    /**
     * Beri notifikasi untuk transfer masuk yang BELUM pernah dinotifikasi. Mengembalikan jumlah
     * transfer baru yang diberitahukan (0 bila tak ada yang baru / notifikasi dimatikan pengguna).
     */
    fun notifyIncoming(incoming: List<TransferProyek>): Int {
        if (incoming.isEmpty()) return 0
        // Bila pengguna menolak izin/menonaktifkan notifikasi, jangan tandai sudah-dinotifikasi —
        // agar begitu izin diberikan, siklus berikutnya tetap memberi tahu.
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return 0

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val sudah = prefs.getStringSet(KEY_IDS, emptySet())!!.toMutableSet()
        val baru = incoming.filter { it.id_transfer !in sudah }
        if (baru.isEmpty()) return 0

        val (judul, isi) = if (baru.size == 1) {
            val t = baru.first()
            "Aset masuk" to "${t.nama_barang.ifBlank { "Barang" }} ×${t.jumlah} dari ${t.proyek_asal} — ketuk untuk konfirmasi"
        } else {
            "Aset masuk" to "${baru.size} aset baru menuju proyek Anda — ketuk untuk konfirmasi penerimaan"
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_OPEN_TRANSFER_MASUK, true)
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(judul)
            .setContentText(isi)
            .setStyle(NotificationCompat.BigTextStyle().bigText(isi))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIF_ID, notif)

        baru.forEach { sudah.add(it.id_transfer) }
        prefs.edit().putStringSet(KEY_IDS, sudah).apply()
        return baru.size
    }

    companion object {
        const val CHANNEL_ID = "transfer_masuk"
        private const val NOTIF_ID = 1001
        private const val PREFS = "transfer_notif"
        private const val KEY_IDS = "notified_ids"
    }
}
