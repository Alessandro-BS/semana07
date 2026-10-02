// security-crypto está obsoleta, pero se usa en este ejercicio (ver Conclusiones del documento)
@file:Suppress("DEPRECATION")

package com.example.geocheckin

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

class SecureStorageManager(context: Context) {

    // Clave maestra AES-256 almacenada en el Android Keystore
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCheckInToken(token: String) {
        sharedPreferences.edit { putString(KEY_CHECKIN_TOKEN, token) }
    }

    fun getCheckInToken(): String? {
        return sharedPreferences.getString(KEY_CHECKIN_TOKEN, null)
    }

    // Firma HMAC-SHA256 con una clave que nunca sale del Android Keystore
    fun signPayload(payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(getOrCreateSigningKey())
        val signature = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(signature, Base64.NO_WRAP)
    }

    private fun getOrCreateSigningKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(SIGNING_KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
            ANDROID_KEYSTORE
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(SIGNING_KEY_ALIAS, KeyProperties.PURPOSE_SIGN).build()
        )
        return keyGenerator.generateKey()
    }

    companion object {
        private const val PREFS_NAME = "secure_geo_prefs"
        private const val KEY_CHECKIN_TOKEN = "KEY_CHECKIN_TOKEN"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val SIGNING_KEY_ALIAS = "geocheckin_hmac_key"
    }
}
