package com.tokosaya.kasir.ui.produk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tokosaya.kasir.data.local.entity.ProductEntity
import com.tokosaya.kasir.data.repository.ProductRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProdukViewModel(private val repo: ProductRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val products: StateFlow<List<ProductEntity>> = _query
        .flatMapLatest { q ->
            if (q.isBlank()) repo.getAll() else repo.search(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun search(q: String) { _query.value = q }

    fun simpan(p: ProductEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            if (p.id == 0L) repo.insert(p) else repo.update(p)
            onDone()
        }
    }

    fun hapus(p: ProductEntity) {
        viewModelScope.launch { repo.delete(p) }
    }
}
