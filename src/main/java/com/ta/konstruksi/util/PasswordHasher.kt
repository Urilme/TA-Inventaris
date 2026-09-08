package com.ta.konstruksi.util

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Verifier kata sandi untuk login LURING (C1). Menyimpan hash PBKDF2 ber-salt — bukan kata sandi —
 * sehingga login luring dapat memverifikasi tanpa jaringan tanpa pernah menyimpan sandi terbaca.
 *
 * Dipakai: saat login DARING berhasil, [hash] menghasilkan (saltB64, hashB64) yang disimpan di sesi
 * Room; saat login LURING, [verify] mencocokkan sandi yang dimasukkan terhadap keduanya.
 */
object PasswordHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    data class Verifier(val saltB64: String, val hashB64: String)

    /** Bangun verifier baru (salt acak) untuk [password]. */
    fun hash(password: String): Verifier {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(password, salt)
        return Verifier(saltB64 = encode(salt), hashB64 = encode(hash))
    }

    /**
     * Verifikasi [password] terhadap verifier tersimpan. Perbandingan konstan-waktu untuk
     * menghindari timing attack. Mengembalikan false bila verifier kosong/rusak.
     */
    fun verify(password: String, saltB64: String, hashB64: String): Boolean {
        if (saltB64.isBlank() || hashB64.isBlank()) return false
        val salt = runCatching { decode(saltB64) }.getOrNull() ?: return false
        val expected = runCatching { decode(hashB64) }.getOrNull() ?: return false
        val actual = pbkdf2(password, salt)
        return constantTimeEquals(expected, actual)
    }

    private fun pbkdf2(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    private fun decode(s: String): ByteArray = Base64.getDecoder().decode(s)
}
