package com.ussdauto.android.data.repository

import com.ussdauto.android.data.local.db.TransactionLocalDao
import com.ussdauto.android.data.local.db.TransactionLocalEntity
import com.ussdauto.android.domain.model.LocalTransaction
import com.ussdauto.android.domain.model.OperationType
import com.ussdauto.android.domain.model.PendingCommand
import com.ussdauto.android.domain.model.TransactionStatusLocal
import com.ussdauto.android.domain.repository.TransactionLocalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Instant
import javax.inject.Inject

class TransactionLocalRepositoryImpl @Inject constructor(
    private val dao: TransactionLocalDao
) : TransactionLocalRepository {

    override suspend fun insertIfNew(command: PendingCommand): Boolean {
        val entity = TransactionLocalEntity(
            id = command.transactionId,
            operationType = command.operationType.name,
            phone = command.phone,
            operator = command.operator,
            amount = command.amount.toPlainString(),
            senderName = command.senderName,
            countryCode = command.countryCode,
            description = command.description,
            simSlotIndex = command.simSlotIndex,
            ussdCode = command.ussdCode,
            statutLocal = TransactionStatusLocal.RECEIVED_BY_DEVICE.name,
            smsContentBrut = null,
            createdAt = System.currentTimeMillis(),
            lastStatusSentAt = null
        )
        return dao.insert(entity) != -1L
    }

    override suspend fun updateStatus(transactionId: String, status: TransactionStatusLocal, smsContentBrut: String?) {
        dao.updateStatus(transactionId, status.name, smsContentBrut)
    }

    override suspend fun markStatusSent(transactionId: String, sentAt: Instant) {
        dao.markStatusSent(transactionId, sentAt.toEpochMilli())
    }

    override suspend fun findById(transactionId: String): LocalTransaction? =
        dao.findById(transactionId)?.toDomain()

    override fun observeAll(): Flow<List<LocalTransaction>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    private fun TransactionLocalEntity.toDomain() = LocalTransaction(
        transactionId = id,
        operationType = OperationType.valueOf(operationType),
        phone = phone,
        operator = operator,
        amount = BigDecimal(amount),
        senderName = senderName,
        countryCode = countryCode,
        description = description,
        simSlotIndex = simSlotIndex,
        ussdCode = ussdCode,
        statutLocal = TransactionStatusLocal.valueOf(statutLocal),
        smsContentBrut = smsContentBrut,
        createdAt = Instant.ofEpochMilli(createdAt),
        lastStatusSentAt = lastStatusSentAt?.let(Instant::ofEpochMilli)
    )
}
