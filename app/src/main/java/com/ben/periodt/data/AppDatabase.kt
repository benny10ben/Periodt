package com.ben.periodt.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.ben.periodt.security.DbKeyManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.util.UUID

@Database(
    entities = [
        PeriodCycleEntity::class,
        PillPackEntity::class,
        DailyCycleLogEntity::class,
        ProfileEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun periodCycleDao(): PeriodCycleDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE period_cycles ADD COLUMN painLevel INTEGER NOT NULL DEFAULT 5")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS pill_packs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        startDate TEXT NOT NULL,
                        pillCount INTEGER NOT NULL,
                        endDate TEXT
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_cycle_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        cycleId INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        bleeding TEXT NOT NULL,
                        bloodColor TEXT NOT NULL,
                        painLevel INTEGER NOT NULL DEFAULT 5,
                        FOREIGN KEY (cycleId) REFERENCES period_cycles(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_cycle_logs_cycleId ON daily_cycle_logs(cycleId)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS profiles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        profileUuid TEXT NOT NULL,
                        name TEXT NOT NULL,
                        avatarColor TEXT NOT NULL DEFAULT '#D89046',
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                val uuid      = UUID.randomUUID().toString()
                val now       = System.currentTimeMillis()
                db.execSQL(
                    "INSERT INTO profiles (profileUuid, name, avatarColor, createdAt) VALUES (?, 'Me', 'avatar_1', ?)",
                    arrayOf(uuid, now)
                )

                db.execSQL("ALTER TABLE period_cycles ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1")

                db.execSQL("ALTER TABLE pill_packs ADD COLUMN profileId INTEGER NOT NULL DEFAULT 1")
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
                    return super.create(configuration).also { passphrase.fill(0) }
                }
            }
            return Room.databaseBuilder(ctx, AppDatabase::class.java, "period_db")
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                // ✨ THIS IS THE FIX FOR FRESH INSTALLS ✨
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // When the DB is created for the very first time, seed the default profile.
                        // This guarantees that profileId 1 exists before any cycles can be added.
                        val uuid = UUID.randomUUID().toString()
                        val now = System.currentTimeMillis()
                        db.execSQL(
                            "INSERT INTO profiles (profileUuid, name, avatarColor, createdAt) VALUES (?, 'Me', 'avatar_1', ?)",
                            arrayOf(uuid, now)
                        )
                    }
                })
                .build()
        }
    }
}