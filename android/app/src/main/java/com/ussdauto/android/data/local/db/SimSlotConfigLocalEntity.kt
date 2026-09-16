package com.ussdauto.android.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sim_slot_config_local")
data class SimSlotConfigLocalEntity(
    @PrimaryKey val slotIndex: Int,
    val operator: String,
    val phoneNumberOnSim: String?,
    val actif: Boolean
)
