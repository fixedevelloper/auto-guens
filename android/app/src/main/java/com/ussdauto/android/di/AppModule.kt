package com.ussdauto.android.di

import com.ussdauto.android.data.repository.StatusCallbackRepositoryImpl
import com.ussdauto.android.data.repository.TransactionLocalRepositoryImpl
import com.ussdauto.android.data.sim.ActiveSimProvider
import com.ussdauto.android.data.sim.AndroidActiveSimProvider
import com.ussdauto.android.data.sim.SimSlotManagerImpl
import com.ussdauto.android.data.sms.parser.MtnSmsParser
import com.ussdauto.android.data.sms.parser.OrangeSmsParser
import com.ussdauto.android.data.status.StatusNotifierImpl
import com.ussdauto.android.data.ussd.UssdExecutorImpl
import com.ussdauto.android.domain.repository.StatusCallbackRepository
import com.ussdauto.android.domain.repository.TransactionLocalRepository
import com.ussdauto.android.domain.sim.SimSlotManager
import com.ussdauto.android.domain.sms.SmsParser
import com.ussdauto.android.domain.status.StatusNotifier
import com.ussdauto.android.domain.ussd.UssdExecutor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindTransactionLocalRepository(impl: TransactionLocalRepositoryImpl): TransactionLocalRepository

    @Binds
    abstract fun bindStatusCallbackRepository(impl: StatusCallbackRepositoryImpl): StatusCallbackRepository

    @Binds
    abstract fun bindStatusNotifier(impl: StatusNotifierImpl): StatusNotifier

    @Binds
    abstract fun bindSimSlotManager(impl: SimSlotManagerImpl): SimSlotManager

    @Binds
    abstract fun bindActiveSimProvider(impl: AndroidActiveSimProvider): ActiveSimProvider

    @Binds
    abstract fun bindUssdExecutor(impl: UssdExecutorImpl): UssdExecutor

    @Binds
    @IntoSet
    abstract fun bindMtnSmsParser(impl: MtnSmsParser): SmsParser

    @Binds
    @IntoSet
    abstract fun bindOrangeSmsParser(impl: OrangeSmsParser): SmsParser
}
