package ir.mobileshop.inventory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.mobileshop.inventory.data.db.Product
import ir.mobileshop.inventory.ui.components.FilterBar
import ir.mobileshop.inventory.util.DateUtil
import ir.mobileshop.inventory.viewmodel.FilterState
import ir.mobileshop.inventory.viewmodel.InventoryViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: InventoryViewModel, onAddProduct: () -> Unit) {
    val products by viewModel.products.collectAsState()
    val filter by viewModel.filter.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("موبایل بلاد شاپور") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddProduct) {
                Icon(Icons.Default.Add, contentDescription = "افزودن محصول")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            FilterBar(filter = filter, onFilterChange = { viewModel.setFilter(it) })
            if (products.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("هیچ محصولی یافت نشد", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(products, key = { it.id }) { product ->
                        ProductCard(product = product, onDelete = { viewModel.deleteProduct(it) })
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(product: Product, onDelete: (Product) -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    val fmt = NumberFormat.getNumberInstance(Locale("fa", "IR"))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${product.brand.displayName} • ${product.category.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "قیمت: ${fmt.format(product.price)} تومان",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "تعداد: ${product.quantity}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    DateUtil.both(product.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("حذف محصول") },
            text = { Text("آیا از حذف \"${product.name}\" مطمئن هستید؟") },
            confirmButton = {
                TextButton(onClick = { onDelete(product); showConfirm = false }) { Text("حذف", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("لغو") }
            }
        )
    }
}
