package com.ta.konstruksi.auth

import com.google.common.truth.Truth.assertThat
import com.ta.konstruksi.util.PasswordHasher
import org.junit.Test

/** Uji verifier kata sandi login luring (C1): PBKDF2 ber-salt, satu-arah, konstan-waktu. */
class PasswordHasherTest {

    @Test
    fun `sandi benar terverifikasi`() {
        val v = PasswordHasher.hash("rahasia123")
        assertThat(PasswordHasher.verify("rahasia123", v.saltB64, v.hashB64)).isTrue()
    }

    @Test
    fun `sandi salah ditolak`() {
        val v = PasswordHasher.hash("rahasia123")
        assertThat(PasswordHasher.verify("rahasiaSALAH", v.saltB64, v.hashB64)).isFalse()
    }

    @Test
    fun `verifier kosong ditolak`() {
        assertThat(PasswordHasher.verify("apa pun", "", "")).isFalse()
    }

    @Test
    fun `salt acak - hash dua sandi sama tidak identik`() {
        val a = PasswordHasher.hash("sama")
        val b = PasswordHasher.hash("sama")
        assertThat(a.saltB64).isNotEqualTo(b.saltB64)   // salt acak per verifier
        assertThat(a.hashB64).isNotEqualTo(b.hashB64)   // → hash berbeda meski sandi sama
    }
}
