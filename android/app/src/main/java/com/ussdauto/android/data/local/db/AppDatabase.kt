package com.ussdauto.android.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TransactionLocalEntity::class, SimSlotConfigLocalEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionLocalDao(): TransactionLocalDao
    abstract fun simSlotConfigLocalDao(): SimSlotConfigLocalDao
}
