package ir.mobileshop.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ProductBrand(val displayName: String) {
    ALL("همه"), SAMSUNG("سامسونگ"), APPLE("اپل"),
    NOKIA("نوکیا"), XIAOMI("شیائومی"), HUAWEI("هواوی"),
    LG("ال‌جی"), OTHER("سایر")
}

enum class ProductCategory(val displayName: String) {
    ALL("همه"), PHONE("گوشی"), ACCESSORY("لوازم جانبی"), PART("قطعات")
}

enum class SortOrder { NEWEST, OLDEST }

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: ProductBrand = ProductBrand.OTHER,
    val category: ProductCategory = ProductCategory.PHONE,
    val price: Long = 0,
    val quantity: Int = 1,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
