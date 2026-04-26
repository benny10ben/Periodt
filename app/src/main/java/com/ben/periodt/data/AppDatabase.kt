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
    version = 6,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun periodCycleDao(): PeriodCycleDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // migrations ──────────────────────────────────

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
                    CREATE TABLE IF NOT EXISTS `profiles` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `profileUuid` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `avatarColor` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                """.trimIndent())
                val uuid = java.util.UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                db.execSQL(
                    "INSERT INTO `profiles` (`id`, `profileUuid`, `name`, `avatarColor`, `createdAt`) VALUES (1, ?, 'Me', 'avatar_1', ?)",
                    arrayOf(uuid, now)
                )
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `period_cycles_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `profileId` INTEGER NOT NULL,
                        `startDate` TEXT NOT NULL,
                        `endDate` TEXT NOT NULL,
                        `bleeding` TEXT NOT NULL,
                        `bloodColor` TEXT NOT NULL,
                        `painLevel` INTEGER NOT NULL,
                        FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `period_cycles_new` (`id`, `profileId`, `startDate`, `endDate`, `bleeding`, `bloodColor`, `painLevel`)
                    SELECT `id`, 1, `startDate`, `endDate`, `bleeding`, `bloodColor`, `painLevel` FROM `period_cycles`
                """.trimIndent())
                db.execSQL("DROP TABLE `period_cycles`")
                db.execSQL("ALTER TABLE `period_cycles_new` RENAME TO `period_cycles`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_period_cycles_profileId` ON `period_cycles` (`profileId`)")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pill_packs_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `profileId` INTEGER NOT NULL,
                        `startDate` TEXT NOT NULL,
                        `pillCount` INTEGER NOT NULL,
                        `endDate` TEXT,
                        FOREIGN KEY(`profileId`) REFERENCES `profiles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO `pill_packs_new` (`id`, `profileId`, `startDate`, `pillCount`, `endDate`)
                    SELECT `id`, 1, `startDate`, `pillCount`, `endDate` FROM `pill_packs`
                """.trimIndent())
                db.execSQL("DROP TABLE `pill_packs`")
                db.execSQL("ALTER TABLE `pill_packs_new` RENAME TO `pill_packs`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_pill_packs_profileId` ON `pill_packs` (`profileId`)")
            }
        }

        // ── NEW: Migration 5 → 6 ─────────────────────────────────────────────
        //
        // Goal: Add 5 sync-tracking columns to the three entity tables.
        // ProfileEntity is excluded — it already has `profileUuid` as its
        // stable cloud identity.

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {

                // ── Helper: the UUID generation expression ────────────────────
                // SQLite has no UUID() function. We build one from randomblob():
                //   randomblob(4)  → 4 random bytes → 8 hex chars  (xxxxxxxx)
                //   randomblob(2)  → 2 random bytes → 4 hex chars  (xxxx)
                //   The '4' prefix on the third group marks this as a v4 UUID.
                //   'substr(...)' on the fourth group sets the variant bits.
                val uuidExpr = """
                    lower(hex(randomblob(4))) || '-' ||
                    lower(hex(randomblob(2))) || '-' ||
                    '4' || substr(lower(hex(randomblob(2))), 2) || '-' ||
                    substr('89ab', abs(random()) % 4 + 1, 1) ||
                    substr(lower(hex(randomblob(2))), 2) || '-' ||
                    lower(hex(randomblob(6)))
                """.trimIndent()

                // ── period_cycles ─────────────────────────────────────────────

                // Step 1: Add each column individually.
                // SQLite only supports ADD COLUMN one at a time — no multi-column ALTER.
                db.execSQL("ALTER TABLE `period_cycles` ADD COLUMN `syncUuid`      TEXT    NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `period_cycles` ADD COLUMN `serverVersion` INTEGER")          // nullable
                db.execSQL("ALTER TABLE `period_cycles` ADD COLUMN `updatedAt`     INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `period_cycles` ADD COLUMN `isSynced`      INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `period_cycles` ADD COLUMN `isDeleted`     INTEGER NOT NULL DEFAULT 0")

                // Step 2: Backfill syncUuid for all existing rows.
                // Without this, every existing cycle would have syncUuid = '',
                // which is useless as a cloud identity.
                db.execSQL("UPDATE `period_cycles` SET `syncUuid` = $uuidExpr WHERE `syncUuid` = ''")

                // Step 3: Add indexes for the sync engine's most frequent queries.
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_cycles_unsynced` ON `period_cycles` (`isSynced`, `isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_cycles_syncuuid` ON `period_cycles` (`syncUuid`)")

                // ── pill_packs ────────────────────────────────────────────────

                db.execSQL("ALTER TABLE `pill_packs` ADD COLUMN `syncUuid`      TEXT    NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `pill_packs` ADD COLUMN `serverVersion` INTEGER")
                db.execSQL("ALTER TABLE `pill_packs` ADD COLUMN `updatedAt`     INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `pill_packs` ADD COLUMN `isSynced`      INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `pill_packs` ADD COLUMN `isDeleted`     INTEGER NOT NULL DEFAULT 0")

                db.execSQL("UPDATE `pill_packs` SET `syncUuid` = $uuidExpr WHERE `syncUuid` = ''")

                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_packs_unsynced` ON `pill_packs` (`isSynced`, `isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_packs_syncuuid` ON `pill_packs` (`syncUuid`)")

                // ── daily_cycle_logs ──────────────────────────────────────────

                db.execSQL("ALTER TABLE `daily_cycle_logs` ADD COLUMN `syncUuid`      TEXT    NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `daily_cycle_logs` ADD COLUMN `serverVersion` INTEGER")
                db.execSQL("ALTER TABLE `daily_cycle_logs` ADD COLUMN `updatedAt`     INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `daily_cycle_logs` ADD COLUMN `isSynced`      INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `daily_cycle_logs` ADD COLUMN `isDeleted`     INTEGER NOT NULL DEFAULT 0")

                db.execSQL("UPDATE `daily_cycle_logs` SET `syncUuid` = $uuidExpr WHERE `syncUuid` = ''")

                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_logs_unsynced` ON `daily_cycle_logs` (`isSynced`, `isDeleted`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `idx_logs_syncuuid` ON `daily_cycle_logs` (`syncUuid`)")
            }
        }

        // ── Database builder ─────────────────────────────────────────────────

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
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
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