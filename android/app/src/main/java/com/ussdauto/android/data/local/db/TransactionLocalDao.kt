package com.ussdauto.android.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionLocalDao {

    /** IGNORE : si l'id existe déjà, l'insertion est silencieusement ignorée (idempotence
     *  face à une livraison FCM dupliquée). Retourne -1 dans ce cas, sinon l'id de la ligne. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: TransactionLocalEntity): Long

    @Query("UPDATE transaction_local SET statutLocal = :statut, smsContentBrut = COALESCE(:smsContentBrut, smsContentBrut) WHERE id = :id")
    suspend fun updateStatus(id: String, statut: String, smsContentBrut: String?)

    @Query("UPDATE transaction_local SET lastStatusSentAt = :sentAt WHERE id = :id")
    suspend fun markStatusSent(id: String, sentAt: Long)

    @Query("SELECT * FROM transaction_local WHERE id = :id")
    suspend fun findById(id: String): TransactionLocalEntity?

    @Query("SELECT * FROM transaction_local ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TransactionLocalEntity>>
}
