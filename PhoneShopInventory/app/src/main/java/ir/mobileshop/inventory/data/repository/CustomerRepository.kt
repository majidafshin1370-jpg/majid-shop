package ir.mobileshop.inventory.data.repository

import ir.mobileshop.inventory.data.db.Customer
import ir.mobileshop.inventory.data.db.CustomerDao
import kotlinx.coroutines.flow.Flow

class CustomerRepository(private val dao: CustomerDao) {
    fun getAll(): Flow<List<Customer>> = dao.getAll()
    fun getTotalDebt(): Flow<Long?> = dao.getTotalDebt()
    suspend fun insert(c: Customer) = dao.insert(c)
    suspend fun update(c: Customer) = dao.update(c)
    suspend fun delete(c: Customer) = dao.delete(c)
}
