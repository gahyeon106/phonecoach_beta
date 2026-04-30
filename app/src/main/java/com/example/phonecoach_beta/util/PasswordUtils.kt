package com.example.phonecoach_beta.util  // ✅ 패키지명 확인!

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object PasswordUtils {

    fun generateSalt(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun hashPassword(password: String, salt: String): String {
        val input = password + salt
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(hashBytes, Base64.NO_WRAP)
    }
}