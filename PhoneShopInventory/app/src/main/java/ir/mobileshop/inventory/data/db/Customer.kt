package ir.mobileshop.inventory.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val debtAmount: Long = 0,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
