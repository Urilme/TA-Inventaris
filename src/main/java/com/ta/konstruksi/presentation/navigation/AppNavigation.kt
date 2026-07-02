package com.ta.konstruksi.presentation.navigation

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ta.konstruksi.presentation.ui.auth.LoginScreen
import com.ta.konstruksi.presentation.ui.dashboard.DashboardScreen
import com.ta.konstruksi.presentation.ui.katalog.KatalogScreen
import com.ta.konstruksi.presentation.ui.katalog.TambahKatalogScreen
import com.ta.konstruksi.presentation.ui.mutasi.MutasiScreen
import com.ta.konstruksi.presentation.ui.riwayat.RiwayatScreen
import com.ta.konstruksi.presentation.ui.transfer.TransferDetailScreen
import com.ta.konstruksi.presentation.ui.transfer.TransferMasukScreen
import com.ta.konstruksi.presentation.ui.transfer.TransferScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Dashboard : Screen("dashboard")
    object Katalog : Screen("katalog")
    object TambahKatalog : Screen("tambah_katalog")
    object Mutasi : Screen("mutasi")
    object Transfer : Screen("transfer")
    object TransferMasuk : Screen("transfer_masuk")
    object TransferDetail : Screen("transfer_detail/{transferId}") {
        fun createRoute(transferId: String) = "transfer_detail/$transferId"
    }
    object Riwayat : Screen("riwayat")
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val isOnline = rememberIsOnline()

    NavHost(navController = navController, startDestination = Screen.Login.route) {

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                isOnline = isOnline
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(navController = navController, isOnline = isOnline)
        }

        composable(Screen.Katalog.route) {
            KatalogScreen(navController = navController)
        }

        composable(Screen.TambahKatalog.route) {
            TambahKatalogScreen(
                onBack = { navController.popBackStack() },
                isOnline = isOnline
            )
        }

        composable(Screen.Mutasi.route) {
            MutasiScreen(
                onBack = { navController.popBackStack() },
                isOnline = isOnline
            )
        }

        composable(Screen.Transfer.route) {
            TransferScreen(
                onBack = { navController.popBackStack() },
                navController = navController,
                isOnline = isOnline
            )
        }

        composable(Screen.TransferMasuk.route) {
            TransferMasukScreen(navController = navController, isOnline = isOnline)
        }

        composable(
            route = Screen.TransferDetail.route,
            arguments = listOf(navArgument("transferId") { type = NavType.StringType })
        ) { backStackEntry ->
            val transferId = backStackEntry.arguments?.getString("transferId") ?: ""
            TransferDetailScreen(
                idTransfer = transferId,
                onBack = { navController.popBackStack() },
                isOnline = isOnline
            )
        }

        composable(Screen.Riwayat.route) {
            RiwayatScreen(navController = navController, isOnline = isOnline)
        }
    }
}

/**
 * Memantau konektivitas jaringan secara reaktif lewat ConnectivityManager.
 * Mengembalikan true hanya jika ada jaringan dengan internet yang tervalidasi.
 */
@Composable
private fun rememberIsOnline(): Boolean {
    val context = LocalContext.current
    val connectivityManager = remember {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    fun currentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    var isOnline by remember { mutableStateOf(currentlyOnline()) }

    DisposableEffect(connectivityManager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isOnline = currentlyOnline() }
            override fun onLost(network: Network) { isOnline = currentlyOnline() }
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                isOnline = currentlyOnline()
            }
        }
        connectivityManager.registerDefaultNetworkCallback(callback)
        onDispose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    return isOnline
}
