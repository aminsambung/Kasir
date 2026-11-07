package com.tokosaya.kasir.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sku: String,
    val barcode: String? = null,
    val nama: String,
    val kategori: String = "Umum",
    val hargaBeli: Long = 0,
    val hargaJual: Long,
    val stok: Int = 0,
    val stokMin: Int = 5,
    val aktif: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nomorTrx: String,
    val subtotal: Long,
    val diskon: Long = 0,
    val pajak: Long = 0,
    val total: Long,
    val metodeBayar: String,
    val bayar: Long,
    val kembalian: Long,
    val status: String = "SELESAI",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transaction_items")
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trxId: Long,
    val productId: Long,
    val namaProduk: String,
    val hargaSatuan: Long,
    val qty: Int,
    val subtotal: Long
)

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val tipe: String,
    val qty: Int,
    val keterangan: String? = null,
    val refId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
