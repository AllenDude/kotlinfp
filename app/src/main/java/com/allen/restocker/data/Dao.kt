package com.allen.restocker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RestockerDao {

    // ---- catalog ----
    @Query("SELECT * FROM items ORDER BY sortIndex, name")
    fun observeItems(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM types ORDER BY sortIndex")
    fun observeTypes(): Flow<List<TypeEntity>>

    @Query("SELECT * FROM flags ORDER BY sortIndex")
    fun observeFlags(): Flow<List<FlagEntity>>

    @Query("SELECT * FROM items ORDER BY sortIndex, name")
    suspend fun getItems(): List<ItemEntity>

    @Query("SELECT * FROM types ORDER BY sortIndex")
    suspend fun getTypes(): List<TypeEntity>

    @Query("SELECT * FROM flags ORDER BY sortIndex")
    suspend fun getFlags(): List<FlagEntity>

    @Query("SELECT COUNT(*) FROM items")
    suspend fun countItems(): Int

    @Query("SELECT COUNT(*) FROM flags")
    suspend fun countFlags(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTypes(types: List<TypeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFlags(flags: List<FlagEntity>)

    @Query("UPDATE flags SET label = :label WHERE id = :id")
    suspend fun updateFlagLabel(id: String, label: String)

    @Query("DELETE FROM types WHERE itemId = :itemId")
    suspend fun deleteTypesOf(itemId: String)

    @Query("DELETE FROM types WHERE id = :id")
    suspend fun deleteType(id: String)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("UPDATE items SET sortIndex = :sortIndex WHERE id = :id")
    suspend fun setItemSort(id: String, sortIndex: Int)

    // ---- orders ----
    @Query("SELECT * FROM orders WHERE sentAt IS NULL ORDER BY id DESC LIMIT 1")
    fun observeDraft(): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE sentAt IS NULL ORDER BY id DESC LIMIT 1")
    suspend fun getDraft(): OrderEntity?

    @Query("SELECT * FROM orders WHERE sentAt IS NOT NULL ORDER BY sentAt DESC")
    fun observeHistory(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE sentAt IS NOT NULL ORDER BY sentAt DESC LIMIT 1")
    suspend fun getLatestSent(): OrderEntity?

    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun deleteOrder(id: Long)

    // ---- lines ----
    @Query("SELECT * FROM lines WHERE orderId = :orderId ORDER BY id")
    fun observeLines(orderId: Long): Flow<List<LineEntity>>

    @Query("SELECT * FROM lines WHERE orderId = :orderId ORDER BY id")
    suspend fun getLines(orderId: Long): List<LineEntity>

    @Query("SELECT * FROM lines WHERE orderId = :orderId AND itemId = :itemId AND typeId = :typeId LIMIT 1")
    suspend fun findLine(orderId: Long, itemId: String, typeId: String): LineEntity?

    @Insert
    suspend fun insertLine(line: LineEntity): Long

    @Insert
    suspend fun insertLines(lines: List<LineEntity>)

    @Query("UPDATE lines SET qty = :qty WHERE id = :id")
    suspend fun setLineQty(id: Long, qty: String)

    @Query("UPDATE lines SET flag = :flag WHERE id = :id")
    suspend fun setLineFlag(id: Long, flag: String)

    @Query("UPDATE lines SET customName = :name WHERE id = :id")
    suspend fun setLineCustomName(id: Long, name: String)

    @Query("DELETE FROM lines WHERE id = :id")
    suspend fun deleteLine(id: Long)

    @Query("DELETE FROM lines WHERE orderId = :orderId")
    suspend fun deleteLinesOf(orderId: Long)

    @Query("DELETE FROM lines WHERE itemId = :itemId AND orderId IN (SELECT id FROM orders WHERE sentAt IS NULL)")
    suspend fun deleteDraftLinesOfItem(itemId: String)
}
