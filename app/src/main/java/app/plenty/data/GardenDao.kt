package app.plenty.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GardenDao {

    @Query("SELECT * FROM plants WHERE slot IS NOT NULL ORDER BY slot")
    fun observePottedPlants(): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants WHERE id IN (:ids)")
    suspend fun plants(ids: List<Long>): List<PlantEntity>

    @Query("SELECT * FROM plants WHERE id = :id")
    suspend fun plant(id: Long): PlantEntity?

    @Query("SELECT * FROM plants WHERE slot = :slot")
    suspend fun plantInSlot(slot: Int): PlantEntity?

    @Insert
    suspend fun insertPlant(plant: PlantEntity): Long

    @Update
    suspend fun updatePlants(plants: List<PlantEntity>)

    @Query("SELECT * FROM inventory")
    fun observeInventory(): Flow<List<InventoryEntity>>

    @Query("SELECT quantity FROM inventory WHERE itemId = :itemId")
    suspend fun quantity(itemId: String): Int?

    @Upsert
    suspend fun upsertInventory(item: InventoryEntity)

    @Insert
    suspend fun insertJourney(journey: JourneyEntity): Long

    @Update
    suspend fun updateJourney(journey: JourneyEntity)

    @Query("SELECT * FROM journeys WHERE id = :id")
    suspend fun journey(id: Long): JourneyEntity?

    @Query("SELECT * FROM journeys WHERE status = 'ACTIVE' LIMIT 1")
    suspend fun activeJourney(): JourneyEntity?

    @Query("SELECT COALESCE(SUM(distanceKm), 0) FROM journeys WHERE status = 'SETTLED'")
    fun observeTotalDistanceKm(): Flow<Double>

    @Insert
    suspend fun insertGrowth(rows: List<JourneyGrowthEntity>)

    @Query("SELECT * FROM journey_growth WHERE journeyId = :journeyId")
    suspend fun growthFor(journeyId: Long): List<JourneyGrowthEntity>

    /** ABORT on a duplicate key: a second grant fails the whole transaction instead of paying twice. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLedger(entry: RewardLedgerEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM reward_ledger WHERE rewardKey = :rewardKey)")
    suspend fun hasLedger(rewardKey: String): Boolean

    @Query("SELECT * FROM daily_xp WHERE day = :day AND plantId IN (:plantIds)")
    suspend fun dailyXp(day: String, plantIds: List<Long>): List<DailyXpEntity>

    @Upsert
    suspend fun upsertDailyXp(rows: List<DailyXpEntity>)

    @Query("SELECT EXISTS(SELECT 1 FROM region_claims WHERE regionId = :regionId AND day = :day)")
    suspend fun regionClaimedOn(regionId: String, day: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM region_claims WHERE regionId = :regionId)")
    suspend fun regionEverClaimed(regionId: String): Boolean

    @Insert
    suspend fun insertRegionClaim(claim: RegionClaimEntity)
}
