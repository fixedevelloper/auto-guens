package com.ussdauto.android.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Identité de ce device côté API (Device.id / Device.apiKey côté backend).
 * Provisionnée manuellement lors de la mise en service de l'appareil dédié (pas
 * d'endpoint d'auto-enregistrement dans le périmètre actuel de l'API).
 */
@Singleton
class DeviceCredentialsStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("device_credentials", Context.MODE_PRIVATE)

    var apiKey: String?
        get() = prefs.getString(KEY_API_KEY, null)
        set(value) = prefs.edit { putString(KEY_API_KEY, value) }

    var deviceId: String?
        get() = prefs.getString(KEY_DEVICE_ID, null)
        set(value) = prefs.edit { putString(KEY_DEVICE_ID, value) }

    private companion object {
        const val KEY_API_KEY = "device_api_key"
        const val KEY_DEVICE_ID = "device_id"
    }
}
