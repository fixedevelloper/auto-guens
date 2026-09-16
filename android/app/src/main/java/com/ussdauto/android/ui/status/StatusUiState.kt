package com.ussdauto.android.ui.status

import com.ussdauto.android.domain.model.LocalTransaction

data class StatusUiState(
    val deviceOnline: Boolean = true,
    val transactions: List<LocalTransaction> = emptyList()
)
