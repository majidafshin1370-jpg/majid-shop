package ir.mobileshop.inventory.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY debtAmount DESC")
    fun getAll(): Flow<List<Customer>>

    @Query("SELECT SUM(debtAmount) FROM customers")
    fun getTotalDebt(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: Customer)

    @Update
    suspend fun update(customer: Customer)

    @Delete
    suspend fun delete(customer: Customer)
}
