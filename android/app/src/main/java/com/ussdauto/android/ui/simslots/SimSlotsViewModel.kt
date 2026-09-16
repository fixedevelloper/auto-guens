package com.ussdauto.android.ui.simslots

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ussdauto.android.domain.sim.SimSlotManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Config gérée depuis l'API : cet écran est en lecture seule, l'app ne fait qu'afficher/synchroniser. */
@HiltViewModel
class SimSlotsViewModel @Inject constructor(
    private val simSlotManager: SimSlotManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SimSlotsUiState())
    val uiState: StateFlow<SimSlotsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loading = true)
            val slots = simSlotManager.currentConfig()
            val mismatches = simSlotManager.validatePhysicalSims()
            _uiState.value = SimSlotsUiState(loading = false, slots = slots, mismatches = mismatches)
        }
    }
}
