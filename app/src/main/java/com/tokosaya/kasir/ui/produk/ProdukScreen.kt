package com.tokosaya.kasir.ui.produk

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tokosaya.kasir.data.local.entity.ProductEntity
import com.tokosaya.kasir.util.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdukScreen(vm: ProdukViewModel) {
    val products by vm.products.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    var editProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Produk") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editProduct = null
                showDialog = true
            }) { Icon(Icons.Default.Add, "Tambah") }
        }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::search,
                label = { Text("Cari produk...") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(products, key = { it.id }) { p ->
                    Card(
                        onClick = {
                            editProduct = p
                            showDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(p.nama, fontWeight = FontWeight.Bold)
                                Text(
                                    "SKU: ${p.sku} | Barcode: ${p.barcode ?: "-"}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatRupiah(p.hargaJual), fontWeight = FontWeight.Bold)
                                Text(
                                    "Stok: ${p.stok}",
                                    color = if (p.stok <= p.stokMin) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        ProductFormDialog(
            initial = editProduct,
            onDismiss = { showDialog = false },
            onSave = { p ->
                vm.simpan(p) { showDialog = false }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    initial: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (ProductEntity) -> Unit
) {
    var nama by remember { mutableStateOf(initial?.nama ?: "") }
    var sku by remember { mutableStateOf(initial?.sku ?: "") }
    var barcode by remember { mutableStateOf(initial?.barcode ?: "") }
    var kategori by remember { mutableStateOf(initial?.kategori ?: "Umum") }
    var hargaBeli by remember { mutableStateOf(initial?.hargaBeli?.toString() ?: "0") }
    var hargaJual by remember { mutableStateOf(initial?.hargaJual?.toString() ?: "0") }
    var stok by remember { mutableStateOf(initial?.stok?.toString() ?: "0") }
    var stokMin by remember { mutableStateOf(initial?.stokMin?.toString() ?: "5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Produk" else "Edit Produk") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nama, { nama = it }, label = { Text("Nama") })
                OutlinedTextField(sku, { sku = it }, label = { Text("SKU") })
                OutlinedTextField(barcode, { barcode = it }, label = { Text("Barcode") })
                OutlinedTextField(kategori, { kategori = it }, label = { Text("Kategori") })
                OutlinedTextField(hargaBeli, { hargaBeli = it.filter { c -> c.isDigit() } }, label = { Text("Harga Beli") })
                OutlinedTextField(hargaJual, { hargaJual = it.filter { c -> c.isDigit() } }, label = { Text("Harga Jual") })
                OutlinedTextField(stok, { stok = it.filter { c -> c.isDigit() } }, label = { Text("Stok") })
                OutlinedTextField(stokMin, { stokMin = it.filter { c -> c.isDigit() } }, label = { Text("Stok Minimum") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (nama.isNotBlank() && hargaJual.isNotBlank()) {
                    onSave(
                        (initial ?: ProductEntity(sku = sku, nama = nama, hargaJual = 0)).copy(
                            nama = nama,
                            sku = sku.ifBlank { nama.take(3).uppercase() + System.currentTimeMillis() % 1000 },
                            barcode = barcode.ifBlank { null },
                            kategori = kategori,
                            hargaBeli = hargaBeli.toLongOrNull() ?: 0,
                            hargaJual = hargaJual.toLongOrNull() ?: 0,
                            stok = stok.toIntOrNull() ?: 0,
                            stokMin = stokMin.toIntOrNull() ?: 5
                        )
                    )
                }
            }) { Text("Simpan") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
