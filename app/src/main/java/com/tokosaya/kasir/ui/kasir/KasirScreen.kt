package com.tokosaya.kasir.ui.kasir

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tokosaya.kasir.util.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasirScreen(
    vm: KasirViewModel,
    onScanClick: () -> Unit,
    onBayarSuccess: (Long) -> Unit
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Kasir") }) },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize()) {
            Button(
                onClick = onScanClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.QrCodeScanner, null)
                Spacer(Modifier.width(8.dp))
                Text("Scan Barcode")
            }
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.cart, key = { it.productId }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.nama, fontWeight = FontWeight.Bold)
                                Text(formatRupiah(item.harga))
                            }
                            IconButton(onClick = { vm.ubahQty(item.productId, -1) }) {
                                Icon(Icons.Default.Remove, null)
                            }
                            Text("${item.qty}", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { vm.ubahQty(item.productId, 1) }) {
                                Icon(Icons.Default.Add, null)
                            }
                            Text(
                                formatRupiah(item.subtotal),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(110.dp)
                            )
                        }
                    }
                }
            }

            Divider(Modifier.padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal")
                Text(formatRupiah(state.subtotal), fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Diskon")
                Text(formatRupiah(state.diskon))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Pajak ${state.pajakPersen}%")
                Text(formatRupiah(state.pajak))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TOTAL", fontWeight = FontWeight.Bold)
                Text(formatRupiah(state.total), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { vm.bayar(onBayarSuccess) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.cart.isNotEmpty()
            ) {
                Icon(Icons.Default.Payment, null)
                Spacer(Modifier.width(8.dp))
                Text("Bayar ${formatRupiah(state.total)}")
            }
        }
    }
}
