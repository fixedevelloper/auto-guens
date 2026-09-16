package com.ussdauto.android.di

import android.content.Context
import androidx.room.Room
import com.ussdauto.android.data.local.db.AppDatabase
import com.ussdauto.android.data.local.db.SimSlotConfigLocalDao
import com.ussdauto.android.data.local.db.TransactionLocalDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "ussd_automation.db").build()

    @Provides
    fun provideTransactionLocalDao(db: AppDatabase): TransactionLocalDao = db.transactionLocalDao()

    @Provides
    fun provideSimSlotConfigLocalDao(db: AppDatabase): SimSlotConfigLocalDao = db.simSlotConfigLocalDao()
}
