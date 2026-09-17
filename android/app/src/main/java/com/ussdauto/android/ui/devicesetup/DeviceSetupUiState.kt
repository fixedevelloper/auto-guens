package com.ussdauto.android.ui.devicesetup

data class DeviceSetupUiState(
    val deviceId: String = "",
    val apiKey: String = "",
    val saving: Boolean = false,
    val resultMessage: String? = null,
    val resultIsError: Boolean = false
)
