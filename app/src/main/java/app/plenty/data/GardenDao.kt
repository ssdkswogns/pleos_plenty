package app.plenty.data

import androidx.room.Dao
import androidx.room.Embedded
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

    @Query("SELECT * FROM journeys WHERE id = :id")
    fun observeJourney(id: Long): Flow<JourneyEntity?>

    /** Closed journeys plus open ones that already parked (pending distance), newest first. */
    @Query("SELECT * FROM journeys WHERE status != 'ACTIVE' OR arrivedAt IS NOT NULL ORDER BY startedAt DESC, id DESC")
    fun observeJournal(): Flow<List<JourneyEntity>>

    /** Newest journey whose result screen has not been shown yet. */
    @Query(
        "SELECT id FROM journeys WHERE resultSeenAt IS NULL AND (status != 'ACTIVE' OR arrivedAt IS NOT NULL) " +
            "ORDER BY id DESC LIMIT 1",
    )
    fun observeUnseenResultId(): Flow<Long?>

    @Query("UPDATE journeys SET resultSeenAt = :at WHERE id = :id AND resultSeenAt IS NULL")
    suspend fun markResultSeen(id: Long, at: Long)

    @Query("SELECT COALESCE(SUM(distanceKm), 0) FROM journeys WHERE status = 'SETTLED'")
    fun observeTotalDistanceKm(): Flow<Double>

    @Insert
    suspend fun insertGrowth(rows: List<JourneyGrowthEntity>)

    @Query("SELECT * FROM journey_growth WHERE journeyId = :journeyId")
    suspend fun growthFor(journeyId: Long): List<JourneyGrowthEntity>

    @Query(
        "SELECT g.*, p.species FROM journey_growth g JOIN plants p ON p.id = g.plantId " +
            "WHERE g.journeyId = :journeyId ORDER BY p.slot",
    )
    fun observeGrowthWithSpecies(journeyId: Long): Flow<List<GrowthWithSpecies>>

    @Query("SELECT * FROM ready_selection WHERE id = 1")
    fun observeReadySelection(): Flow<ReadySelectionEntity?>

    @Query("SELECT * FROM ready_selection WHERE id = 1")
    suspend fun readySelection(): ReadySelectionEntity?

    @Upsert
    suspend fun upsertReadySelection(selection: ReadySelectionEntity)

    @Query("DELETE FROM ready_selection")
    suspend fun deleteReadySelection()

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

data class GrowthWithSpecies(
    @Embedded val growth: JourneyGrowthEntity,
    val species: String,
)
