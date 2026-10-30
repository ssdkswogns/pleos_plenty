package app.plenty.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PlantEntity::class,
        InventoryEntity::class,
        JourneyEntity::class,
        JourneyGrowthEntity::class,
        RewardLedgerEntity::class,
        DailyXpEntity::class,
        RegionClaimEntity::class,
        ReadySelectionEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2, spec = PlentyDatabase.Migration1To2::class)],
)
abstract class PlentyDatabase : RoomDatabase() {
    abstract fun gardenDao(): GardenDao

    /** v2 adds the journey bundle columns. Journeys closed before it are treated as already seen. */
    class Migration1To2 : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.execSQL("UPDATE journeys SET resultSeenAt = settledAt WHERE status != 'ACTIVE'")
        }
    }

    companion object {
        fun create(context: Context): PlentyDatabase =
            Room.databaseBuilder(context, PlentyDatabase::class.java, "plenty.db").build()
    }
}
