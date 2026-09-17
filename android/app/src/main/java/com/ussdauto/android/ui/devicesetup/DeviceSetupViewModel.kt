package com.ussdauto.android.ui.devicesetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ussdauto.android.data.local.DeviceCredentialsStore
import com.ussdauto.android.domain.sim.SimSlotManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Écran d'admin minimal pour provisionner deviceId/apiKey sur cet appareil dédié,
 *  en l'absence d'endpoint d'auto-enregistrement côté API (voir README). */
@HiltViewModel
class DeviceSetupViewModel @Inject constructor(
    private val deviceCredentialsStore: DeviceCredentialsStore,
    private val simSlotManager: SimSlotManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DeviceSetupUiState(
            deviceId = deviceCredentialsStore.deviceId.orEmpty(),
            apiKey = deviceCredentialsStore.apiKey.orEmpty()
        )
    )
    val uiState: StateFlow<DeviceSetupUiState> = _uiState.asStateFlow()

    fun onDeviceIdChange(value: String) {
        _uiState.value = _uiState.value.copy(deviceId = value, resultMessage = null)
    }

    fun onApiKeyChange(value: String) {
        _uiState.value = _uiState.value.copy(apiKey = value, resultMessage = null)
    }

    fun save() {
        val deviceId = _uiState.value.deviceId.trim()
        val apiKey = _uiState.value.apiKey.trim()
        if (deviceId.isEmpty() || apiKey.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                resultMessage = "deviceId et apiKey sont requis",
                resultIsError = true
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(saving = true, resultMessage = null)
            deviceCredentialsStore.deviceId = deviceId
            deviceCredentialsStore.apiKey = apiKey

            simSlotManager.syncFromRemote()
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        saving = false,
                        resultMessage = "Configuration SIM synchronisée",
                        resultIsError = false
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        saving = false,
                        resultMessage = "Identifiants enregistrés, échec de synchronisation : ${error.message}",
                        resultIsError = true
                    )
                }
        }
    }
}
