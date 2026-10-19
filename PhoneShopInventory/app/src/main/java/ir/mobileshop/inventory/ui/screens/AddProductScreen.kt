package ir.mobileshop.inventory.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ir.mobileshop.inventory.data.db.*
import ir.mobileshop.inventory.viewmodel.InventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(viewModel: InventoryViewModel, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var description by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf(ProductBrand.OTHER) }
    var category by remember { mutableStateOf(ProductCategory.PHONE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("افزودن محصول") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(value = name, onValueChange = { name = it },
                label = { Text("نام محصول") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            // Brand dropdown
            var brandExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = brandExpanded, onExpandedChange = { brandExpanded = it }) {
                OutlinedTextField(
                    value = brand.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("برند") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = brandExpanded, onDismissRequest = { brandExpanded = false }) {
                    ProductBrand.values().filter { it != ProductBrand.ALL }.forEach { b ->
                        DropdownMenuItem(text = { Text(b.displayName) }, onClick = { brand = b; brandExpanded = false })
                    }
                }
            }

            // Category dropdown
            var catExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                OutlinedTextField(
                    value = category.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("دسته‌بندی") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    ProductCategory.values().filter { it != ProductCategory.ALL }.forEach { c ->
                        DropdownMenuItem(text = { Text(c.displayName) }, onClick = { category = c; catExpanded = false })
                    }
                }
            }

            OutlinedTextField(value = price, onValueChange = { price = it },
                label = { Text("قیمت (تومان)") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

            OutlinedTextField(value = quantity, onValueChange = { quantity = it },
                label = { Text("تعداد") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

            OutlinedTextField(value = description, onValueChange = { description = it },
                label = { Text("توضیحات") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.addProduct(Product(
                            name = name,
                            brand = brand,
                            category = category,
                            price = price.toLongOrNull() ?: 0L,
                            quantity = quantity.toIntOrNull() ?: 1,
                            description = description
                        ))
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ذخیره")
            }
        }
    }
}
