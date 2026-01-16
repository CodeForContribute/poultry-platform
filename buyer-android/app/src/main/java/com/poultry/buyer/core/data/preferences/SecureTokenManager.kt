package com.poultry.buyer.core.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure token storage using EncryptedSharedPreferences.
 * Tokens are encrypted at rest using AES-256.
 */
@Singleton
class SecureTokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "secure_auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_FCM_TOKEN_SENT = "fcm_token_sent"
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // In-memory cache for fast access (avoids decryption on every read)
    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var cachedRefreshToken: String? = null

    init {
        // Load tokens into cache on initialization
        cachedAccessToken = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
        cachedRefreshToken = encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        encryptedPrefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
        cachedAccessToken = accessToken
        cachedRefreshToken = refreshToken
    }

    fun getAccessToken(): String? = cachedAccessToken

    fun getRefreshToken(): String? = cachedRefreshToken

    fun saveUser(userId: String, userName: String?) {
        encryptedPrefs.edit()
            .putString(KEY_USER_ID, userId)
            .apply {
                userName?.let { putString(KEY_USER_NAME, it) }
            }
            .apply()
    }

    fun getUserId(): String? = encryptedPrefs.getString(KEY_USER_ID, null)

    fun getUserName(): String? = encryptedPrefs.getString(KEY_USER_NAME, null)

    fun isLoggedIn(): Boolean = cachedAccessToken != null

    fun getDeviceId(): String {
        var deviceId = encryptedPrefs.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = java.util.UUID.randomUUID().toString()
            encryptedPrefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }

    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
        cachedAccessToken = null
        cachedRefreshToken = null
    }

    // FCM Token management
    fun saveFcmToken(token: String) {
        encryptedPrefs.edit()
            .putString(KEY_FCM_TOKEN, token)
            .putBoolean(KEY_FCM_TOKEN_SENT, false)
            .apply()
    }

    fun getFcmToken(): String? = encryptedPrefs.getString(KEY_FCM_TOKEN, null)

    fun markFcmTokenSent() {
        encryptedPrefs.edit()
            .putBoolean(KEY_FCM_TOKEN_SENT, true)
            .apply()
    }

    fun isFcmTokenSent(): Boolean = encryptedPrefs.getBoolean(KEY_FCM_TOKEN_SENT, false)

    fun hasPendingFcmToken(): Boolean {
        val token = getFcmToken()
        return token != null && !isFcmTokenSent()
    }
}
