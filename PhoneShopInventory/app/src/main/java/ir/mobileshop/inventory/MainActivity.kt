package ir.mobileshop.inventory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import ir.mobileshop.inventory.ui.screens.*
import ir.mobileshop.inventory.ui.theme.PhoneShopTheme
import ir.mobileshop.inventory.viewmodel.InventoryViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: InventoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhoneShopTheme {
                AppNavigation(viewModel)
            }
        }
    }
}

@Composable
fun AppNavigation(viewModel: InventoryViewModel) {
    val navController = rememberNavController()

    val bottomItems = listOf(
        Triple("home", "محصولات", Icons.Default.Home),
        Triple("debtors", "بدهکاران", Icons.Default.People)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                bottomItems.forEach { (route, label, icon) ->
                    NavigationBarItem(
                        selected = currentRoute == route,
                        onClick = {
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = "home", Modifier.padding(innerPadding)) {
            composable("home") { HomeScreen(viewModel, onAddProduct = { navController.navigate("add_product") }) }
            composable("add_product") { AddProductScreen(viewModel, onBack = { navController.popBackStack() }) }
            composable("debtors") { DebtorScreen(viewModel) }
        }
    }
}
