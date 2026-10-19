package ir.mobileshop.inventory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.mobileshop.inventory.data.db.Customer
import ir.mobileshop.inventory.viewmodel.InventoryViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtorScreen(viewModel: InventoryViewModel) {
    val customers by viewModel.customers.collectAsState()
    val totalDebt by viewModel.totalDebt.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    val fmt = NumberFormat.getNumberInstance(Locale("fa", "IR"))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("لیست بدهکاران") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "افزودن بدهکار")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            // Total debt summary card
            Card(
                Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("جمع کل بدهی:", style = MaterialTheme.typography.titleMedium)
                    Text("${fmt.format(totalDebt)} تومان",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error)
                }
            }

            if (customers.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("هیچ بدهکاری ثبت نشده", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(customers, key = { it.id }) { customer ->
                        CustomerCard(customer = customer, onDelete = { viewModel.deleteCustomer(it) })
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddDebtorDialog(
            onDismiss = { showDialog = false },
            onConfirm = { viewModel.addCustomer(it); showDialog = false }
        )
    }
}

@Composable
fun CustomerCard(customer: Customer, onDelete: (Customer) -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    val fmt = NumberFormat.getNumberInstance(Locale("fa", "IR"))

    Card(
        Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(customer.name, style = MaterialTheme.typography.titleMedium)
                if (customer.phone.isNotBlank())
                    Text(customer.phone, style = MaterialTheme.typography.bodySmall)
                Text("${fmt.format(customer.debtAmount)} تومان",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error)
                if (customer.description.isNotBlank())
                    Text(customer.description, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { showConfirm = true }) {
                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("حذف بدهکار") },
            text = { Text("آیا از حذف \"${customer.name}\" مطمئن هستید؟") },
            confirmButton = {
                TextButton(onClick = { onDelete(customer); showConfirm = false }) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("لغو") } }
        )
    }
}

@Composable
fun AddDebtorDialog(onDismiss: () -> Unit, onConfirm: (Customer) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var debt by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن بدهکار") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("نام مشتری") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it },
                    label = { Text("شماره تلفن") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(value = debt, onValueChange = { debt = it },
                    label = { Text("مبلغ بدهی (تومان)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = desc, onValueChange = { desc = it },
                    label = { Text("شرح بدهی") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onConfirm(Customer(name = name, phone = phone,
                        debtAmount = debt.toLongOrNull() ?: 0L, description = desc))
                }
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("لغو") } }
    )
}
