package ir.mobileshop.inventory.data.repository

import ir.mobileshop.inventory.data.db.*
import kotlinx.coroutines.flow.Flow

class ProductRepository(private val dao: ProductDao) {

    fun getProducts(
        brand: ProductBrand,
        category: ProductCategory,
        sort: SortOrder,
        query: String
    ): Flow<List<Product>> {
        val b = brand.name
        val c = category.name
        return if (sort == SortOrder.NEWEST)
            dao.filterNewest(b, c, query)
        else
            dao.filterOldest(b, c, query)
    }

    suspend fun insert(product: Product) = dao.insert(product)
    suspend fun update(product: Product) = dao.update(product)
    suspend fun delete(product: Product) = dao.delete(product)
}
