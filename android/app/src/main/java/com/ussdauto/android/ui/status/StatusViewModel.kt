package com.ussdauto.android.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ussdauto.android.domain.repository.TransactionLocalRepository
import com.ussdauto.android.domain.sim.SimSlotManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class StatusViewModel @Inject constructor(
    transactionLocalRepository: TransactionLocalRepository,
    private val simSlotManager: SimSlotManager
) : ViewModel() {

    val uiState: StateFlow<StatusUiState> = transactionLocalRepository.observeAll()
        .map { StatusUiState(deviceOnline = true, transactions = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatusUiState())

    init {
        // "Au démarrage" pour cette app mono-écran dédiée = démarrage de l'activité de statut.
        viewModelScope.launch {
            simSlotManager.syncFromRemote()
                .onFailure { Timber.w(it, "Échec de la synchronisation initiale de la config SIM") }
        }
    }
}
