# ProGuard/R8 rules — rilis (C4). minifyEnabled + shrinkResources aktif.
#
# Catatan: Room, Hilt/Dagger, Firebase (Auth/Firestore), dan Jetpack Compose sudah menyertakan
# consumer ProGuard rules masing-masing (di-merge otomatis), jadi kelas & anggota yang mereka
# butuhkan tetap terjaga. Aturan di bawah bersifat defensif untuk pola khusus aplikasi ini.

# --- Enum ---
# Nilai enum (JenisMutasi/StatusTransfer/KondisiBarang/SyncStatus) disimpan sebagai .name string
# ke Room & Firestore, lalu dibaca via valueOf(). Jaga values()/valueOf() (default optimize sudah,
# ditegaskan di sini agar aman).
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- Model & entity aplikasi ---
# Firestore di app ini di-(de)serialisasi MANUAL (toMap/getString), bukan via refleksi POJO,
# sehingga tak perlu @Keep. Namun jaga entity Room & domain model dari perubahan tak terduga.
-keep class com.ta.konstruksi.data.local.entity.** { *; }
-keep class com.ta.konstruksi.domain.model.** { *; }

# --- Coroutines ---
-dontwarn kotlinx.coroutines.**

# --- Baris nomor untuk stacktrace Crashlytics yang terbaca (C5) ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
