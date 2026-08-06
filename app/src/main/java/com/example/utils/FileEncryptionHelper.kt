package com.example.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object FileEncryptionHelper {
    private const val PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "AppLockVaultKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12 // GCM recommended IV size is 12 bytes
    private const val TAG_SIZE_BITS = 128

    init {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    /**
     * Encrypts the contents of the [inputStream] and writes the IV (12 bytes) followed by the ciphertext to [outputStream].
     */
    fun encrypt(inputStream: InputStream, outputStream: OutputStream) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        
        // Write IV first
        val iv = cipher.iv
        outputStream.write(iv)
        
        // Encrypt stream
        val buffer = ByteArray(4096)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val encryptedChunk = cipher.update(buffer, 0, bytesRead)
            if (encryptedChunk != null) {
                outputStream.write(encryptedChunk)
            }
        }
        val finalChunk = cipher.doFinal()
        if (finalChunk != null) {
            outputStream.write(finalChunk)
        }
    }

    /**
     * Decrypts the contents of the [inputStream] (first reading the 12-byte IV) and returns the plain bytes.
     */
    fun decrypt(inputStream: InputStream): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        
        // Read 12-byte IV
        val iv = ByteArray(IV_SIZE)
        val read = inputStream.read(iv)
        if (read != IV_SIZE) {
            throw IllegalArgumentException("Invalid encrypted file (IV missing or corrupt)")
        }
        
        val spec = GCMParameterSpec(TAG_SIZE_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        
        // Decrypt stream
        val buffer = ByteArray(4096)
        val outBytes = java.io.ByteArrayOutputStream()
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val decryptedChunk = cipher.update(buffer, 0, bytesRead)
            if (decryptedChunk != null) {
                outBytes.write(decryptedChunk)
            }
        }
        val finalChunk = cipher.doFinal()
        if (finalChunk != null) {
            outBytes.write(finalChunk)
        }
        return outBytes.toByteArray()
    }
}
