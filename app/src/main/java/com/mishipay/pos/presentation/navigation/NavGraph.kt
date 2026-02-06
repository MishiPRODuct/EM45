package com.mishipay.pos.presentation.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mishipay.pos.presentation.ui.screens.BasketScreen
import com.mishipay.pos.presentation.ui.screens.ScanScreen
import com.mishipay.pos.presentation.ui.screens.WelcomeScreen
import com.mishipay.pos.presentation.viewmodel.BasketViewModel
import com.mishipay.pos.presentation.viewmodel.RfidViewModel

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object Basket : Screen("basket")
    data object Scan : Screen("scan")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    rfidViewModel: RfidViewModel = viewModel(),
    basketViewModel: BasketViewModel = viewModel()
) {
    val basket by basketViewModel.basket.collectAsState()
    val sessionTags by basketViewModel.sessionTags.collectAsState()
    val readerState by rfidViewModel.readerState.collectAsState()

    // Collect tag events and add to session
    LaunchedEffect(Unit) {
        rfidViewModel.tagFlow.collect { tag ->
            basketViewModel.addTagToSession(tag.epc)
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onStartClicked = {
                    navController.navigate(Screen.Basket.route)
                }
            )
        }

        composable(Screen.Basket.route) {
            BasketScreen(
                basket = basket,
                onScanItemsClicked = {
                    navController.navigate(Screen.Scan.route)
                },
                onDeleteItem = { rfidTag ->
                    basketViewModel.removeItem(rfidTag)
                }
            )
        }

        composable(Screen.Scan.route) {
            ScanScreen(
                readerState = readerState,
                scannedTags = sessionTags,
                onDoneClicked = {
                    // Stop scanning
                    rfidViewModel.stopScanning()
                    // Commit scanned tags to basket
                    basketViewModel.commitSessionToBasket()
                    // Navigate back to basket
                    navController.popBackStack()
                },
                onConnectReader = {
                    rfidViewModel.connectReader()
                },
                onStartScanning = {
                    rfidViewModel.startScanning()
                }
            )
        }
    }
}
