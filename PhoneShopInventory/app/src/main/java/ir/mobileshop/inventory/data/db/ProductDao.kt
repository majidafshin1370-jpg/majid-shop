package ir.mobileshop.inventory.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun getAllNewest(): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY createdAt ASC")
    fun getAllOldest(): Flow<List<Product>>

    @Query("""SELECT * FROM products WHERE
        (:brand = 'ALL' OR brand = :brand) AND
        (:category = 'ALL' OR category = :category) AND
        (name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        ORDER BY createdAt DESC""")
    fun filterNewest(brand: String, category: String, query: String): Flow<List<Product>>

    @Query("""SELECT * FROM products WHERE
        (:brand = 'ALL' OR brand = :brand) AND
        (:category = 'ALL' OR category = :category) AND
        (name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        ORDER BY createdAt ASC""")
    fun filterOldest(brand: String, category: String, query: String): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product)

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)
}
