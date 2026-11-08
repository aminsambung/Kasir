package com.tokosaya.kasir.ui.kasir

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tokosaya.kasir.data.local.entity.ProductEntity
import com.tokosaya.kasir.data.local.entity.TransactionEntity
import com.tokosaya.kasir.data.local.entity.TransactionItemEntity
import com.tokosaya.kasir.data.repository.ProductRepository
import com.tokosaya.kasir.data.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────
// Model item keranjang
// ─────────────────────────────────────────────
data class CartItem(
    val productId: Long,
    val nama: String,
    val harga: Long,
    val qty: Int = 1
) {
    val subtotal: Long get() = harga * qty
}

// ─────────────────────────────────────────────
// State UI kasir
// ─────────────────────────────────────────────
data class KasirUiState(
    val cart: List<CartItem> = emptyList(),
    val diskon: Long = 0,
    val pajakPersen: Int = 0,
    val metodeBayar: String = "TUNAI",
    val bayar: Long = 0,
    val error: String? = null
) {
    val subtotal: Long get() = cart.sumOf { it.subtotal }
    val pajak: Long get() = ((subtotal - diskon).coerceAtLeast(0)) * pajakPersen / 100
    val total: Long get() = (subtotal - diskon).coerceAtLeast(0) + pajak
    val kembalian: Long get() = (bayar - total).coerceAtLeast(0)
    val isBayarCukup: Boolean get() = metodeBayar != "TUNAI" || bayar >= total
}

// ─────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────
class KasirViewModel(
    private val productRepo: ProductRepository,
    private val trxRepo: TransactionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(KasirUiState())
    val state: StateFlow<KasirUiState> = _state.asStateFlow()

    // ── Tambah produk via barcode ──
    fun tambahByBarcode(barcode: String) {
        viewModelScope.launch {
            val p = productRepo.findBarcode(barcode)
            if (p == null) {
                _state.value = _state.value.copy(
                    error = "Produk dengan barcode $barcode tidak ditemukan"
                )
                return@launch
            }
            tambahProduk(p)
        }
    }

    // ── Tambah produk ke keranjang (dari barcode / pilih manual) ──
    fun tambahProduk(p: ProductEntity) {
        val current = _state.value
        val existing = current.cart.find { it.productId == p.id }
        val newCart = if (existing != null) {
            current.cart.map {
                if (it.productId == p.id) it.copy(qty = it.qty + 1) else it
            }
        } else {
            current.cart + CartItem(p.id, p.nama, p.hargaJual, 1)
        }
        _state.value = current.copy(cart = newCart, error = null)
    }

    // ── Ubah qty (+1 / -1) ──
    fun ubahQty(productId: Long, delta: Int) {
        val current = _state.value
        val newCart = current.cart.mapNotNull {
            if (it.productId == productId) {
                val newQty = it.qty + delta
                if (newQty <= 0) null else it.copy(qty = newQty)
            } else it
        }
        _state.value = current.copy(cart = newCart)
    }

    // ── Hapus item dari keranjang ──
    fun removeItem(productId: Long) {
        val current = _state.value
        _state.value = current.copy(
            cart = current.cart.filterNot { it.productId == productId }
        )
    }

    // ── Set diskon ──
    fun setDiskon(d: Long) {
        val max = _state.value.subtotal
        _state.value = _state.value.copy(diskon = d.coerceIn(0, max))
    }

    // ── Set pajak (%) ──
    fun setPajak(p: Int) {
        _state.value = _state.value.copy(pajakPersen = p.coerceIn(0, 100))
    }

    // ── Set metode bayar ──
    fun setMetode(m: String) {
        _state.value = _state.value.copy(
            metodeBayar = m,
            // reset nominal bayar kalau bukan tunai
            bayar = if (m == "TUNAI") _state.value.bayar else _state.value.total
        )
    }

    // ── Set nominal uang bayar ──
    fun setBayar(b: Long) {
        _state.value = _state.value.copy(bayar = b.coerceAtLeast(0))
    }

    // ── Bayar & simpan transaksi ──
    fun bayar(onDone: (Long) -> Unit) {
        val s = _state.value

        if (s.cart.isEmpty()) {
            _state.value = s.copy(error = "Keranjang kosong")
            return
        }
        if (s.metodeBayar == "TUNAI" && s.bayar < s.total) {
            _state.value = s.copy(error = "Uang bayar kurang")
            return
        }

        viewModelScope.launch {
            try {
                val nomorTrx = generateNomorTrx()
                val trx = TransactionEntity(
                    nomorTrx = nomorTrx,
                    subtotal = s.subtotal,
                    diskon = s.diskon,
                    pajak = s.pajak,
                    total = s.total,
                    metodeBayar = s.metodeBayar,
                    bayar = if (s.metodeBayar == "TUNAI") s.bayar else s.total,
                    kembalian = if (s.metodeBayar == "TUNAI") s.kembalian else 0
                )
                val items = s.cart.map {
                    TransactionItemEntity(
                        trxId = 0,
                        productId = it.productId,
                        namaProduk = it.nama,
                        hargaSatuan = it.harga,
                        qty = it.qty,
                        subtotal = it.subtotal
                    )
                }
                val trxId = trxRepo.simpan(trx, items)
                _state.value = KasirUiState() // reset keranjang
                onDone(trxId)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Gagal menyimpan transaksi: ${e.message}"
                )
            }
        }
    }

    // ── Reset keranjang manual ──
    fun reset() {
        _state.value = KasirUiState()
    }

    // ── Hapus error (dipanggil setelah snackbar tampil) ──
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    // ── Generate nomor transaksi ──
    private fun generateNomorTrx(): String {
        val sdf = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault())
        return "TRX-" + sdf.format(Date())
    }
}
