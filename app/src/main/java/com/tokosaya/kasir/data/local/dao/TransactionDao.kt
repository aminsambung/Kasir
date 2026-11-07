package com.tokosaya.kasir.data.local.dao

import androidx.room.*
import com.tokosaya.kasir.data.local.entity.*
import kotlinx.coroutines.flow.Flow

data class LaporanHarian(
    val tanggal: String,
    val totalTrx: Int,
    val omzet: Long
)

data class ProdukTerlaris(
    val namaProduk: String,
    val totalQty: Int,
    val totalOmzet: Long
)

@Dao
interface TransactionDao {

    @Insert
    suspend fun insertTransaction(trx: TransactionEntity): Long

    @Insert
    suspend fun insertItems(items: List<TransactionItemEntity>)

    @Insert
    suspend fun insertStockMovement(m: StockMovementEntity)

    @Query("UPDATE products SET stok = stok - :qty WHERE id = :productId")
    suspend fun kurangiStok(productId: Long, qty: Int)

    @Transaction
    suspend fun simpanTransaksi(
        trx: TransactionEntity,
        items: List<TransactionItemEntity>
    ): Long {
        val trxId = insertTransaction(trx)
        val fixed = items.map { it.copy(trxId = trxId) }
        insertItems(fixed)
        fixed.forEach {
            kurangiStok(it.productId, it.qty)
            insertStockMovement(
                StockMovementEntity(
                    productId = it.productId,
                    tipe = "SALE",
                    qty = -it.qty,
                    keterangan = "Penjualan ${trx.nomorTrx}",
                    refId = trxId
                )
            )
        }
        return trxId
    }

    @Query("SELECT * FROM transactions ORDER BY createdAt DESC LIMIT 100")
    fun getRecent(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT strftime('%Y-%m-%d', datetime(createdAt/1000, 'unixepoch', 'localtime')) AS tanggal,
               COUNT(*) AS totalTrx,
               SUM(total) AS omzet
        FROM transactions
        WHERE status = 'SELESAI'
        GROUP BY tanggal
        ORDER BY tanggal DESC
        LIMIT 30
    """)
    fun laporanHarian(): Flow<List<LaporanHarian>>

    @Query("""
        SELECT ti.namaProduk AS namaProduk,
               SUM(ti.qty) AS totalQty,
               SUM(ti.subtotal) AS totalOmzet
        FROM transaction_items ti
        JOIN transactions t ON t.id = ti.trxId
        WHERE t.status = 'SELESAI'
        GROUP BY ti.namaProduk
        ORDER BY totalQty DESC
        LIMIT 20
    """)
    fun produkTerlaris(): Flow<List<ProdukTerlaris>>
}
