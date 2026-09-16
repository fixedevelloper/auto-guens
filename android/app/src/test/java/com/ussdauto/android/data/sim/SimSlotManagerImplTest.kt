package com.ussdauto.android.data.sim

import com.google.common.truth.Truth.assertThat
import com.ussdauto.android.data.local.DeviceCredentialsStore
import com.ussdauto.android.data.local.db.SimSlotConfigLocalDao
import com.ussdauto.android.data.local.db.SimSlotConfigLocalEntity
import com.ussdauto.android.data.remote.ApiService
import com.ussdauto.android.domain.model.SimSlotMismatch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/** Couvre la résolution du subscriptionId Android à partir d'un slotIndex logique,
 *  et la détection d'écarts avec les SIM physiquement présentes. */
class SimSlotManagerImplTest {

    private val simSlotConfigLocalDao: SimSlotConfigLocalDao = mock()
    private val apiService: ApiService = mock()
    private val deviceCredentialsStore: DeviceCredentialsStore = mock()
    private val activeSimProvider: ActiveSimProvider = mock()

    private lateinit var simSlotManager: SimSlotManagerImpl

    @Before
    fun setUp() {
        simSlotManager = SimSlotManagerImpl(simSlotConfigLocalDao, apiService, deviceCredentialsStore, activeSimProvider)
    }

    @Test
    fun `subscriptionIdFor resolves the Android subscriptionId matching the logical slot`() {
        whenever(activeSimProvider.activeSims()).thenReturn(
            listOf(
                ActiveSimInfo(slotIndex = 0, subscriptionId = 11, carrierName = "MTN Cameroon"),
                ActiveSimInfo(slotIndex = 1, subscriptionId = 22, carrierName = "Orange Cameroun")
            )
        )

        assertThat(simSlotManager.subscriptionIdFor(1)).isEqualTo(22)
    }

    @Test
    fun `subscriptionIdFor returns null when no physical SIM is present on that slot`() {
        whenever(activeSimProvider.activeSims()).thenReturn(
            listOf(ActiveSimInfo(slotIndex = 0, subscriptionId = 11, carrierName = "MTN Cameroon"))
        )

        assertThat(simSlotManager.subscriptionIdFor(1)).isNull()
    }

    @Test
    fun `validatePhysicalSims reports no mismatch when the carrier matches the configured operator`() = runTest {
        whenever(simSlotConfigLocalDao.getAll()).thenReturn(
            listOf(SimSlotConfigLocalEntity(slotIndex = 0, operator = "MTN", phoneNumberOnSim = null, actif = true))
        )
        whenever(activeSimProvider.activeSims()).thenReturn(
            listOf(ActiveSimInfo(slotIndex = 0, subscriptionId = 11, carrierName = "MTN Cameroon"))
        )

        assertThat(simSlotManager.validatePhysicalSims()).isEmpty()
    }

    @Test
    fun `validatePhysicalSims reports a mismatch when the physical carrier differs from the configured operator`() = runTest {
        whenever(simSlotConfigLocalDao.getAll()).thenReturn(
            listOf(SimSlotConfigLocalEntity(slotIndex = 0, operator = "MTN", phoneNumberOnSim = null, actif = true))
        )
        whenever(activeSimProvider.activeSims()).thenReturn(
            listOf(ActiveSimInfo(slotIndex = 0, subscriptionId = 11, carrierName = "Orange Cameroun"))
        )

        val mismatches = simSlotManager.validatePhysicalSims()

        assertThat(mismatches).containsExactly(SimSlotMismatch(0, "MTN", "Orange Cameroun"))
    }

    @Test
    fun `validatePhysicalSims reports a mismatch when the configured slot has no physical SIM at all`() = runTest {
        whenever(simSlotConfigLocalDao.getAll()).thenReturn(
            listOf(SimSlotConfigLocalEntity(slotIndex = 1, operator = "ORANGE", phoneNumberOnSim = null, actif = true))
        )
        whenever(activeSimProvider.activeSims()).thenReturn(emptyList())

        val mismatches = simSlotManager.validatePhysicalSims()

        assertThat(mismatches).containsExactly(SimSlotMismatch(1, "ORANGE", null))
    }
}
