package ir.mobileshop.inventory.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.mobileshop.inventory.data.db.*
import ir.mobileshop.inventory.viewmodel.FilterState

@Composable
fun FilterBar(
    filter: FilterState,
    onFilterChange: (FilterState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Search
        OutlinedTextField(
            value = filter.query,
            onValueChange = { onFilterChange(filter.copy(query = it)) },
            placeholder = { Text("جستجو...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Brand chips
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.FilterList, null, tint = MaterialTheme.colorScheme.primary)
            ProductBrand.values().filter { it != ProductBrand.ALL }.let { brands ->
                listOf(ProductBrand.ALL) + brands
            }.forEach { brand ->
                FilterChip(
                    selected = filter.brand == brand,
                    onClick = { onFilterChange(filter.copy(brand = brand)) },
                    label = { Text(brand.displayName) }
                )
            }
        }

        // Category + Sort chips
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductCategory.values().forEach { cat ->
                FilterChip(
                    selected = filter.category == cat,
                    onClick = { onFilterChange(filter.copy(category = cat)) },
                    label = { Text(cat.displayName) }
                )
            }
            Spacer(Modifier.width(8.dp))
            SortOrder.values().forEach { s ->
                FilterChip(
                    selected = filter.sort == s,
                    onClick = { onFilterChange(filter.copy(sort = s)) },
                    label = { Text(if (s == SortOrder.NEWEST) "جدیدترین" else "قدیمی‌ترین") }
                )
            }
        }
    }
}
