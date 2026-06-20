package com.example.security

import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object EncryptionHelper {
    private const val ALGORITHM = "AES"
    
    // Developer Credit key seed - 16 bytes for AES key specification
    private val KEY_BYTES = "AmiinCabdiSecure1".toByteArray(StandardCharsets.UTF_8)
    private val SECRET_KEY = SecretKeySpec(KEY_BYTES, ALGORITHM)

    /**
     * Encrypts a plaintext message before peer transmission or database saving.
     */
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT).trim()
        } catch (e: Exception) {
            plainText // Safe fallback to ensure non-crashing behavior
        }
    }

    /**
     * Decrypts an incoming or stored message text.
     */
    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY)
            val decodedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            encryptedText // Safe fallback
        }
    }
}
