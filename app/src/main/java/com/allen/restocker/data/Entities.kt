package com.allen.restocker.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One product (e.g. "Knorr"). [sortIndex] is the EXPORT order; the screen is always A-Z. */
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val sortIndex: Int,
    /** true = lines are written as "<qty> <type>" without the item name (the old "School Supplies" rule). */
    val typeOnlyExport: Boolean = false,
    val hidden: Boolean = false
)

/** One type/variant of an item. Items without real types get one type named after the item. */
@Entity(tableName = "types", indices = [Index("itemId")])
data class TypeEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val name: String,
    val sortIndex: Int,
    /** Tag applied automatically the first time a quantity is entered ("" = none). */
    val defaultFlag: String = ""
)

/** The section tags (Buo, Chichirya, Promo). Data, not code, so they can be renamed. */
@Entity(tableName = "flags")
data class FlagEntity(
    @PrimaryKey val id: String,
    val label: String,
    val sortIndex: Int
)

/** An order. sentAt == null means it is the current draft. */
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val sentAt: Long? = null,
    val text: String = "",
    val lineCount: Int = 0
)

/** One row of an order. typeId == null means a custom type typed in by hand ([customName]). */
@Entity(tableName = "lines", indices = [Index("orderId")])
data class LineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val itemId: String,
    val typeId: String? = null,
    val customName: String = "",
    val qty: String = "",
    val flag: String = ""
)
