package com.ussdauto.android.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SimSlotConfigLocalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<SimSlotConfigLocalEntity>)

    @Query("DELETE FROM sim_slot_config_local")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(entities: List<SimSlotConfigLocalEntity>) {
        clear()
        upsertAll(entities)
    }

    @Query("SELECT * FROM sim_slot_config_local ORDER BY slotIndex ASC")
    suspend fun getAll(): List<SimSlotConfigLocalEntity>

    @Query("SELECT * FROM sim_slot_config_local ORDER BY slotIndex ASC")
    fun observeAll(): Flow<List<SimSlotConfigLocalEntity>>
}
