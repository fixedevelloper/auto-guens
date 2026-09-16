package com.ussdauto.android.ui.simslots

import com.ussdauto.android.domain.model.SimSlotConfig
import com.ussdauto.android.domain.model.SimSlotMismatch

data class SimSlotsUiState(
    val loading: Boolean = true,
    val slots: List<SimSlotConfig> = emptyList(),
    val mismatches: List<SimSlotMismatch> = emptyList()
)
