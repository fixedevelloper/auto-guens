package com.ussdauto.android.data.repository

import com.google.common.truth.Truth.assertThat
import com.ussdauto.android.data.local.db.TransactionLocalDao
import com.ussdauto.android.data.local.db.TransactionLocalEntity
import com.ussdauto.android.domain.model.OperationType
import com.ussdauto.android.domain.model.PendingCommand
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

/** Couvre l'idempotence de l'insertion locale : une commande dupliquée (même
 *  transactionId reçu deux fois via FCM) ne doit pas relancer une exécution. */
class TransactionLocalRepositoryImplTest {

    private val dao: TransactionLocalDao = mock()
    private lateinit var repository: TransactionLocalRepositoryImpl

    @Before
    fun setUp() {
        repository = TransactionLocalRepositoryImpl(dao)
    }

    @Test
    fun `insertIfNew returns true for a transaction not yet known locally`() = runTest {
        whenever(dao.insert(any())).thenReturn(1L)

        val isNew = repository.insertIfNew(command())

        assertThat(isNew).isTrue()
    }

    @Test
    fun `insertIfNew returns false when the transaction id already exists (duplicate FCM delivery)`() = runTest {
        whenever(dao.insert(any())).thenReturn(-1L)

        val isNew = repository.insertIfNew(command())

        assertThat(isNew).isFalse()
    }

    @Test
    fun `findById maps the persisted entity back to the domain model`() = runTest {
        whenever(dao.findById("tx-1")).thenReturn(
            TransactionLocalEntity(
                id = "tx-1",
                operationType = "DEPOSIT",
                phone = "677000000",
                operator = "MTN",
                amount = "5000",
                senderName = "Jean Dupont",
                countryCode = "CM",
                description = null,
                simSlotIndex = 0,
                ussdCode = "*126*1*5000*677000000#",
                statutLocal = "RECEIVED_BY_DEVICE",
                smsContentBrut = null,
                createdAt = 1_000L,
                lastStatusSentAt = null
            )
        )

        val result = repository.findById("tx-1")

        assertThat(result).isNotNull()
        assertThat(result!!.operationType).isEqualTo(OperationType.DEPOSIT)
        assertThat(result.amount).isEqualTo(BigDecimal("5000"))
    }

    private fun command() = PendingCommand(
        transactionId = "tx-1",
        operationType = OperationType.DEPOSIT,
        phone = "677000000",
        operator = "MTN",
        amount = BigDecimal("5000"),
        senderName = "Jean Dupont",
        countryCode = "CM",
        description = null,
        simSlotIndex = 0,
        ussdCode = "*126*1*5000*677000000#"
    )
}
