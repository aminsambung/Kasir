package com.tokosaya.kasir.data.repository

import com.tokosaya.kasir.data.local.dao.ProductDao
import com.tokosaya.kasir.data.local.dao.TransactionDao
import com.tokosaya.kasir.data.local.entity.*

class ProductRepository(private val dao: ProductDao) {
    fun getAll() = dao.getAll()
    fun search(q: String) = dao.search(q)
    fun stokMinim() = dao.getStokMinim()
    suspend fun findBarcode(b: String) = dao.findByBarcode(b)
    suspend fun findById(id: Long) = dao.findById(id)
    suspend fun insert(p: ProductEntity) = dao.insert(p)
    suspend fun update(p: ProductEntity) = dao.update(p)
    suspend fun delete(p: ProductEntity) = dao.delete(p)
    suspend fun updateStok(id: Long, delta: Int) = dao.updateStok(id, delta)
}

class TransactionRepository(private val dao: TransactionDao) {
    fun recent() = dao.getRecent()
    fun laporanHarian() = dao.laporanHarian()
    fun produkTerlaris() = dao.produkTerlaris()
    suspend fun simpan(trx: TransactionEntity, items: List<TransactionItemEntity>) =
        dao.simpanTransaksi(trx, items)
}
