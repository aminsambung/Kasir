package com.tokosaya.kasir.ui.kasir

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // State dialog
    var showPayment by remember { mutableStateOf(false) }
    var showDiskonDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Tampilkan error via snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kasir") },
                actions = {
                    IconButton(onClick = onScanClick) {
                        Icon(Icons.Default.QrCodeScanner, "Scan")
                    }
                    if (state.cart.isNotEmpty()) {
                        IconButton(onClick = { showResetDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, "Kosongkan")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total", style = MaterialTheme.typography.titleMedium)
                        Text(
                            formatRupiah(state.total),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showPayment = true },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = state.cart.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Payment, null)
                        Spacer(Modifier.width(8.dp))
                        Text("BAYAR", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(horizontal = 12.dp)
        ) {
            // Tombol scan besar di atas
            Button(
                onClick = onScanClick,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Icon(Icons.Default.QrCodeScanner, null)
                Spacer(Modifier.width(8.dp))
                Text("Scan Barcode")
            }

            // Daftar item keranjang / placeholder
            if (state.cart.isEmpty()) {
                Box(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Keranjang kosong\nScan barcode untuk mulai",
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(state.cart, key = { it.productId }) { item ->
                        CartItemRow(
                            nama = item.nama,
                            harga = item.harga,
                            qty = item.qty,
                            subtotal = item.subtotal,
                            onMinus = { vm.ubahQty(item.productId, -1) },
                            onPlus = { vm.ubahQty(item.productId, 1) },
                            onHapus = { vm.removeItem(item.productId) }
                        )
                    }
                }
            }

            // Ringkasan bawah
            Divider()
            Column(Modifier.padding(vertical = 8.dp)) {
                SummaryRow("Subtotal", formatRupiah(state.subtotal))
                SummaryRow(
                    "Diskon",
                    if (state.diskon > 0) "- " + formatRupiah(state.diskon) else formatRupiah(0),
                    trailingContent = {
                        IconButton(
                            onClick = { showDiskonDialog = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Edit, "Edit diskon", Modifier.size(16.dp))
                        }
                    }
                )
                SummaryRow("Pajak ${state.pajakPersen}%", formatRupiah(state.pajak))
            }
        }
    }

    // ── Dialog Diskon ──
    if (showDiskonDialog) {
        DiskonDialog(
            current = state.diskon,
            maxDiskon = state.subtotal,
            onDismiss = { showDiskonDialog = false },
            onApply = { d ->
                vm.setDiskon(d)
                showDiskonDialog = false
            }
        )
    }

    // ── Dialog Reset Keranjang ──
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Kosongkan Keranjang?") },
            text = { Text("Semua item di keranjang akan dihapus.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.reset()
                    showResetDialog = false
                }) { Text("Ya, Kosongkan") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Batal") }
            }
        )
    }

    // ── Dialog Pembayaran ──
    if (showPayment) {
        PaymentDialog(
            state = state,
            onMetodeChange = vm::setMetode,
            onBayarChange = vm::setBayar,
            onDismiss = { showPayment = false },
            onKonfirmasi = {
                vm.bayar { trxId ->
                    showPayment = false
                    onBayarSuccess(trxId)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────
// Komponen: Baris item keranjang
// ─────────────────────────────────────────────
@Composable
private fun CartItemRow(
    nama: String,
    harga: Long,
    qty: Int,
    subtotal: Long,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onHapus: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(nama, fontWeight = FontWeight.Bold, maxLines = 2)
                Text(
                    formatRupiah(harga) + " / item",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    formatRupiah(subtotal),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onMinus) {
                    Icon(Icons.Default.RemoveCircleOutline, "Kurangi")
                }
                Text("$qty", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onPlus) {
                    Icon(Icons.Default.AddCircleOutline, "Tambah")
                }
                IconButton(onClick = onHapus) {
                    Icon(
                        Icons.Default.Delete,
                        "Hapus",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Komponen: Baris ringkasan
// ─────────────────────────────────────────────
@Composable
private fun SummaryRow(
    label: String,
    value: String,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, fontWeight = FontWeight.SemiBold)
            trailingContent?.invoke()
        }
    }
}

// ─────────────────────────────────────────────
// Dialog: Diskon (nominal / persen)
// ─────────────────────────────────────────────
@Composable
private fun DiskonDialog(
    current: Long,
    maxDiskon: Long,
    onDismiss: () -> Unit,
    onApply: (Long) -> Unit
) {
    var text by remember { mutableStateOf(if (current > 0) current.toString() else "") }
    var mode by remember { mutableStateOf("NOMINAL") } // NOMINAL / PERSEN

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Diskon") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == "NOMINAL",
                        onClick = { mode = "NOMINAL"; text = "" },
                        label = { Text("Rp") }
                    )
                    FilterChip(
                        selected = mode == "PERSEN",
                        onClick = { mode = "PERSEN"; text = "" },
                        label = { Text("%") }
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() } },
                    label = {
                        Text(if (mode == "NOMINAL") "Nominal Diskon (Rp)" else "Diskon (%)")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                val preview = if (mode == "NOMINAL") {
                    text.toLongOrNull() ?: 0
                } else {
                    maxDiskon * (text.toLongOrNull() ?: 0) / 100
                }
                Text(
                    "Diskon: ${formatRupiah(preview.coerceAtMost(maxDiskon))}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val d = if (mode == "NOMINAL") {
                    text.toLongOrNull() ?: 0
                } else {
                    maxDiskon * (text.toLongOrNull() ?: 0) / 100
                }
                onApply(d.coerceIn(0, maxDiskon))
            }) { Text("Terapkan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

// ─────────────────────────────────────────────
// Dialog: Pembayaran
// ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentDialog(
    state: KasirUiState,
    onMetodeChange: (String) -> Unit,
    onBayarChange: (Long) -> Unit,
    onDismiss: () -> Unit,
    onKonfirmasi: () -> Unit
) {
    var bayarText by remember {
        mutableStateOf(if (state.bayar > 0) state.bayar.toString() else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pembayaran") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Kartu total
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("TOTAL", style = MaterialTheme.typography.labelSmall)
                        Text(
                            formatRupiah(state.total),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Metode bayar
                Text("Metode Bayar", style = MaterialTheme.typography.labelMedium)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("TUNAI", "QRIS", "KARTU").forEach { m ->
                        FilterChip(
                            selected = state.metodeBayar == m,
                            onClick = { onMetodeChange(m) },
                            label = { Text(m, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Input nominal khusus tunai
                if (state.metodeBayar == "TUNAI") {
                    OutlinedTextField(
                        value = bayarText,
                        onValueChange = {
                            bayarText = it.filter { c -> c.isDigit() }
                            onBayarChange(bayarText.toLongOrNull() ?: 0)
                        },
                        label = { Text("Uang Diterima") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Tombol cepat
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                bayarText = state.total.toString()
                                onBayarChange(state.total)
                            },
                            label = { Text("Uang Pas") }
                        )
                        listOf(50_000L, 100_000L).forEach { nominal ->
                            AssistChip(
                                onClick = {
                                    bayarText = nominal.toString()
                                    onBayarChange(nominal)
                                },
                                label = { Text(formatRupiah(nominal)) }
                            )
                        }
                    }

                    // Kembalian
                    val kembalian = (state.bayar - state.total).coerceAtLeast(0)
                    val cukup = state.bayar >= state.total
                    Surface(
                        color = if (cukup) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Kembalian", fontWeight = FontWeight.Medium)
                            Text(
                                formatRupiah(kembalian),
                                fontWeight = FontWeight.Bold,
                                color = if (cukup) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onKonfirmasi,
                enabled = state.isBayarCukup
            ) {
                Text("Konfirmasi & Simpan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
