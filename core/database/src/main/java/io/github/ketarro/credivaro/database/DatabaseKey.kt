package io.github.ketarro.credivaro.database

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Single-process initial slice; no automatic reset, recovery or silent key replacement. */
internal object DatabaseKey {
    private const val PREFIX = "credivaro.db."
    fun alias(name: String) = PREFIX + name
    fun envelope(context: Context, name: String) = File(context.noBackupFilesDir, "$name.key.v1")
    private fun aad(name: String) = ("credivaro-db-key-v1:" + name).toByteArray(Charsets.UTF_8)

    @Synchronized
    fun loadOrCreate(context: Context, name: String): ByteArray {
        require(name.matches(Regex("[a-zA-Z0-9_.-]{1,100}")) && name != "." && name != "..")
        val path = envelope(context, name)
        val atomic = AtomicFile(path)
        val db = context.getDatabasePath(name)
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (path.exists() || File(path.path + ".bak").exists()) {
            val blob = atomic.readFully()
            require(blob.size == 61 && blob[0] == 1.toByte()) { "Invalid key envelope" }
            val key = store.getKey(alias(name), null) as? SecretKey ?: error("Database wrapping key unavailable")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, blob.copyOfRange(1, 13)))
            cipher.updateAAD(aad(name))
            val secret = cipher.doFinal(blob.copyOfRange(13, blob.size))
            return try { check(secret.size == 32); Base64.encode(secret, Base64.NO_WRAP) }
                finally { secret.fill(0) }
        }
        check(listOf("", "-wal", "-shm", "-journal").none { File(db.path + it).exists() }) {
            "Database exists without key envelope; recovery required"
        }
        // Interrupted first creation may have left an alias but no database or envelope.
        val key = if (store.containsAlias(alias(name))) {
            store.getKey(alias(name), null) as? SecretKey ?: error("Wrapping key unavailable")
        } else KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias(name), KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build())
        }.generateKey()
        val secret = ByteArray(32).also { SecureRandom().nextBytes(it) }
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            cipher.updateAAD(aad(name))
            check(cipher.iv.size == 12)
            val blob = byteArrayOf(1) + cipher.iv + cipher.doFinal(secret)
            val out = atomic.startWrite()
            try { out.write(blob); atomic.finishWrite(out) }
            catch (e: Throwable) { atomic.failWrite(out); throw e }
            return Base64.encode(secret, Base64.NO_WRAP)
        } finally { secret.fill(0) }
    }
}
