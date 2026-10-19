package ir.mobileshop.inventory.viewmodel

import android.app.Application
import androidx.lifecycle.*
import ir.mobileshop.inventory.data.db.*
import ir.mobileshop.inventory.data.repository.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class FilterState(
    val brand: ProductBrand = ProductBrand.ALL,
    val category: ProductCategory = ProductCategory.ALL,
    val sort: SortOrder = SortOrder.NEWEST,
    val query: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val productRepo = ProductRepository(db.productDao())
    private val customerRepo = CustomerRepository(db.customerDao())

    // Filter state -------------------------------------------------------------
    private val _filter = MutableStateFlow(FilterState())
    val filter: StateFlow<FilterState> = _filter.asStateFlow()

    val products: StateFlow<List<Product>> = _filter.flatMapLatest { f ->
        productRepo.getProducts(f.brand, f.category, f.sort, f.query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(f: FilterState) { _filter.value = f }

    // Customers ----------------------------------------------------------------
    val customers: StateFlow<List<Customer>> = customerRepo.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalDebt: StateFlow<Long> = customerRepo.getTotalDebt()
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    // CRUD ---------------------------------------------------------------------
    fun addProduct(p: Product) = viewModelScope.launch { productRepo.insert(p) }
    fun updateProduct(p: Product) = viewModelScope.launch { productRepo.update(p) }
    fun deleteProduct(p: Product) = viewModelScope.launch { productRepo.delete(p) }

    fun addCustomer(c: Customer) = viewModelScope.launch { customerRepo.insert(c) }
    fun updateCustomer(c: Customer) = viewModelScope.launch { customerRepo.update(c) }
    fun deleteCustomer(c: Customer) = viewModelScope.launch { customerRepo.delete(c) }
}
