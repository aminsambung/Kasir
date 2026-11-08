package com.tokosaya.kasir

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.tokosaya.kasir.data.local.AppDatabase
import com.tokosaya.kasir.data.repository.ProductRepository
import com.tokosaya.kasir.data.repository.TransactionRepository
import com.tokosaya.kasir.ui.kasir.KasirScreen
import com.tokosaya.kasir.ui.kasir.KasirViewModel
import com.tokosaya.kasir.ui.kasir.PaymentDialog
import com.tokosaya.kasir.ui.laporan.LaporanScreen
import com.tokosaya.kasir.ui.produk.ProdukScreen
import com.tokosaya.kasir.ui.produk.ProdukViewModel
import com.tokosaya.kasir.ui.scan.BarcodeScanScreen
import com.tokosaya.kasir.ui.theme.KasirTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val REQ_BT = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Minta izin Bluetooth (Android 12+) ──
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                REQ_BT
            )
        }

        val db = AppDatabase.get(this)
        val productRepo = ProductRepository(db.productDao())
        val trxRepo = TransactionRepository(db.transactionDao())

        setContent {
            KasirTheme {
                KasirApp(productRepo, trxRepo)
            }
        }
    }
}

// Factory sederhana untuk ViewModel
class SimpleVmFactory<T : ViewModel>(private val creator: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = creator() as VM
}

@Composable
fun KasirApp(
    productRepo: ProductRepository,
    trxRepo: TransactionRepository
) {
    val nav = rememberNavController()
    val kasirVm: KasirViewModel = viewModel(
        factory = SimpleVmFactory { KasirViewModel(productRepo, trxRepo) }
    )
    var showPayment by remember { mutableStateOf(false) }

    NavHost(navController = nav, startDestination = "kasir") {
        composable("kasir") {
            KasirScreen(
                vm = kasirVm,
                onScanClick = { nav.navigate("scan") },
                onBayarSuccess = { /* tampil struk / toast */ }
            )
            if (showPayment) {
                PaymentDialog(
                    vm = kasirVm,
                    onDismiss = { showPayment = false },
                    onSuccess = { showPayment = false }
                )
            }
        }
        composable("scan") {
            BarcodeScanScreen(
                onBarcode = { barcode ->
                    kasirVm.tambahByBarcode(barcode)
                    nav.popBackStack()
                },
                onClose = { nav.popBackStack() }
            )
        }
        composable("produk") {
            val vm: ProdukViewModel = viewModel(
                factory = SimpleVmFactory { ProdukViewModel(productRepo) }
            )
            ProdukScreen(vm)
        }
        composable("laporan") { LaporanScreen(trxRepo) }
    }
}
