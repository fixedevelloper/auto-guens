package com.ussdauto.android.ui.devicesetup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.ussdauto.android.data.local.DeviceCredentialsStore
import com.ussdauto.android.domain.repository.DeviceRegistrationRepository
import com.ussdauto.android.domain.sim.SimSlotManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume

/** Écran d'admin minimal pour provisionner deviceId/apiKey sur cet appareil dédié — l'id
 *  et la clé sont obtenus via POST /api/devices (voir README de l'API) et saisis ici. */
@HiltViewModel
class DeviceSetupViewModel @Inject constructor(
    private val deviceCredentialsStore: DeviceCredentialsStore,
    private val simSlotManager: SimSlotManager,
    private val deviceRegistrationRepository: DeviceRegistrationRepository
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

            // Le token FCM peut déjà avoir été généré avant la saisie des identifiants
            // (onNewToken ne se redéclenche pas forcément) : on l'enregistre ici aussi.
            currentFcmToken()?.let { token ->
                deviceRegistrationRepository.registerFcmToken(token)
                    .onFailure { Timber.w(it, "Échec de l'enregistrement du token FCM depuis l'écran de setup") }
            }

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

    private suspend fun currentFcmToken(): String? = suspendCancellableCoroutine { continuation ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> continuation.resume(token) }
            .addOnFailureListener { error ->
                Timber.w(error, "Impossible de récupérer le token FCM courant")
                continuation.resume(null)
            }
    }
}
