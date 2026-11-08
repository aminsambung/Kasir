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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.tokosaya.kasir.data.local.AppDatabase
import com.tokosaya.kasir.data.repository.ProductRepository
import com.tokosaya.kasir.data.repository.TransactionRepository
import com.tokosaya.kasir.ui.kasir.KasirScreen
import com.tokosaya.kasir.ui.kasir.KasirViewModel
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

        // Minta izin Bluetooth untuk Android 12+ (printer thermal)
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

// ─────────────────────────────────────────────
// Factory ViewModel sederhana
// ─────────────────────────────────────────────
class SimpleVmFactory<T : ViewModel>(private val creator: () -> T) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = creator() as VM
}

// ─────────────────────────────────────────────
// Item bottom navigation
// ─────────────────────────────────────────────
private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem("kasir", "Kasir", Icons.Default.ShoppingCart),
    BottomNavItem("produk", "Produk", Icons.Default.Inventory2),
    BottomNavItem("laporan", "Laporan", Icons.Default.BarChart)
)

// ─────────────────────────────────────────────
// Root Composable
// ─────────────────────────────────────────────
@Composable
fun KasirApp(
    productRepo: ProductRepository,
    trxRepo: TransactionRepository
) {
    val nav = rememberNavController()

    // SATU ViewModel kasir, dishare antara layar kasir & scan
    val kasirVm: KasirViewModel = viewModel(
        factory = SimpleVmFactory { KasirViewModel(productRepo, trxRepo) }
    )

    // ViewModel produk
    val produkVm: ProdukViewModel = viewModel(
        factory = SimpleVmFactory { ProdukViewModel(productRepo) }
    )

    // Cek apakah bottom bar perlu ditampilkan (tidak tampil di layar scan)
    val navBackStackEntry by nav.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                nav.navigate(item.route) {
                                    popUpTo(nav.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPad ->
        NavHost(
            navController = nav,
            startDestination = "kasir",
            modifier = Modifier.padding(innerPad)
        ) {
            // ── Layar Kasir ──
            composable("kasir") {
                KasirScreen(
                    vm = kasirVm,
                    onScanClick = { nav.navigate("scan") },
                    onBayarSuccess = { trxId ->
                        // TODO: navigasi ke struk / tampil snackbar
                        // Contoh: nav.navigate("struk/$trxId")
                    }
                )
            }

            // ── Layar Scan Barcode (full screen, tanpa bottom bar) ──
            composable("scan") {
                BarcodeScanScreen(
                    onBarcode = { barcode ->
                        kasirVm.tambahByBarcode(barcode)
                        nav.popBackStack()
                    },
                    onClose = { nav.popBackStack() }
                )
            }

            // ── Layar Produk ──
            composable("produk") {
                ProdukScreen(produkVm)
            }

            // ── Layar Laporan ──
            composable("laporan") {
                LaporanScreen(trxRepo)
            }
        }
    }
}
