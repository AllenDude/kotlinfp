package com.allen.restocker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// If you ever change the tables, bump `version` and add a Migration.
// Do NOT use fallbackToDestructiveMigration(): it would wipe saved orders on update.
@Database(
    entities = [
        ItemEntity::class,
        TypeEntity::class,
        FlagEntity::class,
        OrderEntity::class,
        LineEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dao(): RestockerDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "restocker.db"
                ).build().also { instance = it }
            }
    }
}
