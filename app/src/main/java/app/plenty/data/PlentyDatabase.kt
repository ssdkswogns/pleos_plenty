package app.plenty.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlantEntity::class,
        InventoryEntity::class,
        JourneyEntity::class,
        JourneyGrowthEntity::class,
        RewardLedgerEntity::class,
        DailyXpEntity::class,
        RegionClaimEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PlentyDatabase : RoomDatabase() {
    abstract fun gardenDao(): GardenDao

    companion object {
        fun create(context: Context): PlentyDatabase =
            Room.databaseBuilder(context, PlentyDatabase::class.java, "plenty.db").build()
    }
}
