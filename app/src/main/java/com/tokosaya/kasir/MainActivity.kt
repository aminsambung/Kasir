package com.tokosaya.kasir

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    NavHost(navController = nav, startDestination = "kasir") {
        composable("kasir") {
            val vm: KasirViewModel = viewModel(
                factory = SimpleVmFactory { KasirViewModel(productRepo, trxRepo) }
            )
            var showPayment by remember { mutableStateOf(false) }

            KasirScreen(
                vm = vm,
                onScanClick = { nav.navigate("scan") },
                onBayarSuccess = { /* bisa tambah navigasi struk */ }
            )

            // tombol bayar dari bawah — kita panggil dialog dari sini
            // karena KasirScreen sudah punya tombol bayar sendiri,
            // kita perlu expose callback. Simpelnya: tambah tombol di FAB
            // (lihat catatan di bawah)
        }
        composable("scan") {
            val vm: KasirViewModel = viewModel(
                factory = SimpleVmFactory { KasirViewModel(productRepo, trxRepo) }
            )
            BarcodeScanScreen(
                onBarcode = { barcode ->
                    vm.tambahByBarcode(barcode)
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
        composable("laporan") {
            LaporanScreen(trxRepo)
        }
    }
}
