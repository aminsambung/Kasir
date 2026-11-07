package com.tokosaya.kasir.ui.kasir

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tokosaya.kasir.util.formatRupiah

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDialog(vm: KasirViewModel, onDismiss: () -> Unit, onSuccess: (Long) -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var bayarText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pembayaran") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Total: ${formatRupiah(state.total)}", fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("TUNAI", "QRIS", "KARTU").forEach { m ->
                        FilterChip(
                            selected = state.metodeBayar == m,
                            onClick = { vm.setMetode(m) },
                            label = { Text(m) }
                        )
                    }
                }

                if (state.metodeBayar == "TUNAI") {
                    OutlinedTextField(
                        value = bayarText,
                        onValueChange = {
                            bayarText = it.filter { c -> c.isDigit() }
                            vm.setBayar(bayarText.toLongOrNull() ?: 0)
                        },
                        label = { Text("Uang Bayar") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Kembalian: ${formatRupiah(state.kembalian)}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.bayar { trxId ->
                    onSuccess(trxId)
                    onDismiss()
                }
            }) { Text("Konfirmasi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
