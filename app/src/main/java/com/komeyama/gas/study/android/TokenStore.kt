package com.komeyama.gas.study.android

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

class TokenStore {

    private var keyStore: KeyStore? = null
    private var sp: OAEPParameterSpec =
        OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec("SHA-1"), PSource.PSpecified.DEFAULT)

    companion object {
        private const val KEY_STORE = "AndroidKeyStore"
        private const val ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        const val TOKEN_AREAS = "token"
        const val REFRESH_TOKEN_AREAS = "refresh token"
    }

    fun prepareKeyStore() {
        try {
            keyStore = KeyStore.getInstance(KEY_STORE)
            if (keyStore != null) {
                keyStore!!.load(null)
                createNewKey(keyStore!!, TOKEN_AREAS)
                createNewKey(keyStore!!, REFRESH_TOKEN_AREAS)
            }
        } catch (e: Exception) {
            Timber.d("prepare key store error: $e")
        }
    }

    private fun createNewKey(keyStore: KeyStore, alias: String) {
        try {
            if (!keyStore.containsAlias(alias)) {
                val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, KEY_STORE
                )
                keyPairGenerator.initialize(
                    KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_DECRYPT)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                        .build()
                )
                keyPairGenerator.generateKeyPair()
            }
        } catch (e: Exception) {
            Timber.d("create new key error: $e")
        }
    }

    fun encryptString(alias: String, plainText: String): String? {
        var encryptedText: String? = null
        try {
            val publicKey: PublicKey = keyStore?.getCertificate(alias)!!.publicKey
            val cipher: Cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, sp)
            val outputStream = ByteArrayOutputStream()
            val cipherOutputStream = CipherOutputStream(
                outputStream, cipher
            )
            cipherOutputStream.write(plainText.toByteArray(charset("UTF-8")))
            cipherOutputStream.close()
            val bytes: ByteArray = outputStream.toByteArray()
            encryptedText = Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            Timber.d("encrypt error: $e")
        }
        return encryptedText
    }

    fun decryptString(alias: String, encryptedText: String): String? {
        var plainText: String? = null
        try {
            val privateKey: PrivateKey = keyStore?.getKey(alias, null) as PrivateKey
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, privateKey, sp)
            val cipherInputStream = CipherInputStream(
                ByteArrayInputStream(Base64.decode(encryptedText, Base64.DEFAULT)), cipher
            )
            val outputStream = ByteArrayOutputStream()
            var b: Int
            while (cipherInputStream.read().also { b = it } != -1) {
                outputStream.write(b)
            }
            outputStream.close()
            plainText = outputStream.toString("UTF-8")
        } catch (e: java.lang.Exception) {
            Timber.d("decrypt error: $e")
        }
        return plainText
    }
}
