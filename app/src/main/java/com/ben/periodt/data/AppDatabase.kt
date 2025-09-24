// com/ben/periodt/data/AppDatabase.kt
package com.ben.periodt.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.ben.periodt.security.DbKeyManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory  // ✅ New import & class

@Database(
    entities = [PeriodCycleEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun periodCycleDao(): PeriodCycleDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // Keep migrations for future schema changes
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE period_cycles ADD COLUMN painLevel INTEGER NOT NULL DEFAULT 5")
            }
        }

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context.applicationContext).also { INSTANCE = it }
            }

        private fun build(ctx: Context): AppDatabase {
            val passphrase = DbKeyManager.getOrCreateDbPassphrase(ctx)
            val factory = object : SupportOpenHelperFactory(passphrase) {
                override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
                    passphrase.fill(0)  // ✅ Zero out bytes after factory creation
                    return super.create(configuration)
                }
            }
            return Room.databaseBuilder(ctx, AppDatabase::class.java, "period_db")
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}