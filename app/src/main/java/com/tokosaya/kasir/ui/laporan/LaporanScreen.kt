package com.tokosaya.kasir.ui.laporan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tokosaya.kasir.data.repository.TransactionRepository
import com.tokosaya.kasir.util.formatRupiah
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaporanScreen(repo: TransactionRepository) {
    val harian by repo.laporanHarian().collectAsState(initial = emptyList())
    val terlaris by repo.produkTerlaris().collectAsState(initial = emptyList())

    Scaffold(topBar = { TopAppBar(title = { Text("Laporan") }) }) { pad ->
        LazyColumn(
            Modifier.padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Penjualan Harian", style = MaterialTheme.typography.titleMedium) }
            items(harian) { h ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(h.tanggal, fontWeight = FontWeight.Bold)
                            Text("${h.totalTrx} transaksi")
                        }
                        Text(formatRupiah(h.omzet), fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { Divider(Modifier.padding(vertical = 8.dp)) }
            item { Text("Produk Terlaris", style = MaterialTheme.typography.titleMedium) }
            items(terlaris) { p ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(p.namaProduk, Modifier.weight(1f))
                        Text("${p.totalQty}x", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(12.dp))
                        Text(formatRupiah(p.totalOmzet))
                    }
                }
            }
        }
    }
}
