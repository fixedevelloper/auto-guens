package com.ussdauto.android.data.sim

import com.ussdauto.android.data.local.DeviceCredentialsStore
import com.ussdauto.android.data.local.db.SimSlotConfigLocalDao
import com.ussdauto.android.data.local.db.SimSlotConfigLocalEntity
import com.ussdauto.android.data.remote.ApiService
import com.ussdauto.android.domain.model.SimSlotConfig
import com.ussdauto.android.domain.model.SimSlotMismatch
import com.ussdauto.android.domain.sim.SimSlotManager
import timber.log.Timber
import javax.inject.Inject

class SimSlotManagerImpl @Inject constructor(
    private val simSlotConfigLocalDao: SimSlotConfigLocalDao,
    private val apiService: ApiService,
    private val deviceCredentialsStore: DeviceCredentialsStore,
    private val activeSimProvider: ActiveSimProvider
) : SimSlotManager {

    override suspend fun syncFromRemote(): Result<Unit> {
        val deviceId = deviceCredentialsStore.deviceId
            ?: return Result.failure(IllegalStateException("deviceId non configuré"))
        val apiKey = deviceCredentialsStore.apiKey
            ?: return Result.failure(IllegalStateException("apiKey non configurée"))

        return try {
            val response = apiService.getSimSlots(deviceId, apiKey)
            if (!response.isSuccessful) {
                return Result.failure(IllegalStateException("HTTP ${response.code()}"))
            }

            val slots = response.body().orEmpty()
                .filter { it.actif }
                .map { SimSlotConfigLocalEntity(it.slotIndex, it.operator, it.phoneNumberOnSim, it.actif) }
            simSlotConfigLocalDao.replaceAll(slots)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.w(e, "Échec de synchronisation de la config SIM")
            Result.failure(e)
        }
    }

    override suspend fun currentConfig(): List<SimSlotConfig> =
        simSlotConfigLocalDao.getAll().map { it.toDomain() }

    override fun subscriptionIdFor(slotIndex: Int): Int? =
        activeSimProvider.activeSims().firstOrNull { it.slotIndex == slotIndex }?.subscriptionId

    override suspend fun validatePhysicalSims(): List<SimSlotMismatch> {
        val expectedSlots = simSlotConfigLocalDao.getAll()
        val physicalByIndex = activeSimProvider.activeSims().associateBy { it.slotIndex }

        return expectedSlots.mapNotNull { slot ->
            val physical = physicalByIndex[slot.slotIndex]
            val carrierName = physical?.carrierName
            val matches = carrierName != null && carrierName.contains(slot.operator, ignoreCase = true)
            if (matches) null else SimSlotMismatch(slot.slotIndex, slot.operator, carrierName)
        }
    }

    private fun SimSlotConfigLocalEntity.toDomain() = SimSlotConfig(slotIndex, operator, phoneNumberOnSim, actif)
}
