package app.plenty.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.plenty.core.content.GrowthStage
import app.plenty.core.content.MaterialIds
import app.plenty.core.content.RegionId
import app.plenty.core.content.SpeciesIds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

/** The plan's mandatory scenarios for storage: one grant per key, across repeats and restarts. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GardenRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = Clock.fixed(Instant.parse("2026-10-28T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private val busan = RegionId("busan")
    private lateinit var db: PlentyDatabase

    @Before
    fun open() {
        context.deleteDatabase(DB_NAME)
        db = openDb()
    }

    @After
    fun close() {
        db.close()
        context.deleteDatabase(DB_NAME)
    }

    private fun openDb() = Room.databaseBuilder(context, PlentyDatabase::class.java, DB_NAME)
        .allowMainThreadQueries()
        .build()

    private fun repo(random: Random = Random(7)) = GardenRepository(db, clock = clock, random = random)

    private suspend fun GardenRepository.plantedSeed(slot: Int = 1): Long {
        ensureStarterGift()
        return plantMallang(GardenRepository.STARTER_SPECIES, slot)
    }

    private suspend fun GardenRepository.snapshot() = garden.first()

    @Test
    fun starterGiftIsGrantedOnceEvenAfterReopening() = runTest {
        repo().ensureStarterGift()
        repo().ensureStarterGift()
        db.close()
        db = openDb()
        repo().ensureStarterGift()
        assertEquals(1, repo().snapshot().count(ItemIds.mallang(SpeciesIds.MOONBELL)))
    }

    @Test
    fun plantingUsesOneMallangAndAnEmptyPotOnly() = runTest {
        val repo = repo()
        val id = repo.plantedSeed(slot = 2)
        val snapshot = repo.snapshot()
        assertEquals(0, snapshot.count(ItemIds.mallang(SpeciesIds.MOONBELL)))
        assertEquals(listOf(2), snapshot.plants.map { it.slot })
        assertEquals(GrowthStage.SEED, snapshot.plants.single { it.state.id == id }.state.stage)

        val noMallangLeft = runCatching { repo.plantMallang(SpeciesIds.MOONBELL, 3) }
        assertTrue(noMallangLeft.exceptionOrNull() is IllegalStateException)
        db.gardenDao().upsertInventory(InventoryEntity(ItemIds.mallang(SpeciesIds.CLOUD_FERN), 1))
        val occupied = runCatching { repo.plantMallang(SpeciesIds.CLOUD_FERN, 2) }
        assertTrue(occupied.exceptionOrNull() is IllegalStateException)
        assertEquals(1, repo.snapshot().count(ItemIds.mallang(SpeciesIds.CLOUD_FERN)))
    }

    @Test
    fun settlingTwiceGrantsOnceAndReturnsTheSameResult() = runTest {
        val repo = repo()
        val plant = repo.plantedSeed()
        val journey = repo.startJourney(listOf(plant), startOdometerKm = 1_000.0, source = JourneySource.MOCK)

        val first = repo.settleJourney(journey, endOdometerKm = 1_030.0, arrivalRegion = busan) as SettlementView.Settled
        val second = repo.settleJourney(journey, endOdometerKm = 1_200.0, arrivalRegion = busan) as SettlementView.Settled

        assertFalse(first.alreadySettled)
        assertTrue(second.alreadySettled)
        assertEquals(first.copy(alreadySettled = true), second)
        assertEquals(30.0, first.distanceKm, 1e-9)
        val state = repo.snapshot().plants.single().state
        assertEquals(30, state.xp)
        assertEquals(GrowthStage.SPROUT, state.stage)
        assertEquals(1, repo.snapshot().count(ItemIds.material(MaterialIds.MOON_DEW)))
        assertEquals(30.0, repo.snapshot().totalDistanceKm, 1e-9)
    }

    @Test
    fun reopeningTheDatabaseNeverPaysAJourneyAgain() = runTest {
        val plant = repo().plantedSeed()
        val journey = repo().startJourney(listOf(plant), 0.0, JourneySource.MOCK)
        val first = repo().settleJourney(journey, 12.0, busan) as SettlementView.Settled

        db.close()
        db = openDb()
        val again = repo(Random(999)).settleJourney(journey, 12.0, busan) as SettlementView.Settled

        assertEquals(first.copy(alreadySettled = true), again)
        assertEquals(12, repo().snapshot().plants.single().state.xp)
    }

    @Test
    fun theSeedDrawIsStoredOnceAndNotReRolled() = runTest {
        val plant = repo().plantedSeed()
        val journey = repo().startJourney(listOf(plant), 0.0, JourneySource.MOCK)
        // First visit to a region always gives a 말랑; the species comes from the draw.
        val first = repo(Random(1)).settleJourney(journey, 10.0, busan) as SettlementView.Settled
        val seed = checkNotNull(first.seed)
        val bagAfterFirst = repo().snapshot().count(ItemIds.mallang(seed))

        val again = repo(Random(12345)).settleJourney(journey, 10.0, busan) as SettlementView.Settled
        assertEquals(seed, again.seed)
        assertEquals(bagAfterFirst, repo().snapshot().count(ItemIds.mallang(seed)))
    }

    @Test
    fun dailyCapAndRegionRewardSpanJourneysOfTheSameDay() = runTest {
        val repo = repo()
        val plant = repo.plantedSeed()
        val morning = repo.startJourney(listOf(plant), 0.0, JourneySource.MOCK)
        repo.settleJourney(morning, 40.0, busan)
        val evening = repo.startJourney(listOf(plant), 40.0, JourneySource.MOCK)
        val second = repo.settleJourney(evening, 70.0, busan) as SettlementView.Settled

        assertEquals(10, second.growth.single().xpGained)
        assertEquals(null, second.material)
        assertEquals(50, repo.snapshot().plants.single().state.xp)
        assertEquals(1, repo.snapshot().count(ItemIds.material(MaterialIds.MOON_DEW)))
    }

    @Test
    fun unknownOdometerKeepsTheJourneyOpenUntilARealReading() = runTest {
        val repo = repo()
        val plant = repo.plantedSeed()
        val journey = repo.startJourney(listOf(plant), 500.0, JourneySource.VEHICLE)

        assertEquals(SettlementView.Pending, repo.settleJourney(journey, null, busan))
        assertEquals(journey, repo.activeJourneyId())
        assertEquals(0, repo.snapshot().plants.single().state.xp)

        val settled = repo.settleJourney(journey, 525.0, busan) as SettlementView.Settled
        assertEquals(25, settled.growth.single().xpGained)
    }

    @Test
    fun resetOdometerClosesTheJourneyWithoutReward() = runTest {
        val repo = repo()
        val plant = repo.plantedSeed()
        val journey = repo.startJourney(listOf(plant), 900.0, JourneySource.VEHICLE)

        assertEquals(SettlementView.Rejected(journey), repo.settleJourney(journey, 3.0, busan))
        assertEquals(SettlementView.Rejected(journey), repo.settleJourney(journey, 950.0, busan))
        assertEquals(0, repo.snapshot().plants.single().state.xp)
        assertEquals(null, repo.activeJourneyId())
    }

    @Test
    fun onlyOneJourneyCanBeOpenAndCompanionsAreLimitedToThree() = runTest {
        val repo = repo()
        val plant = repo.plantedSeed()
        repo.startJourney(listOf(plant), 0.0, JourneySource.MOCK)
        assertTrue(runCatching { repo.startJourney(listOf(plant), 0.0, JourneySource.MOCK) }.exceptionOrNull() is IllegalStateException)
        assertTrue(runCatching { repo.startJourney(listOf(1, 2, 3, 4), 0.0, JourneySource.MOCK) }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun materialsBoostTraitPointsAndAreSpentInTheSameTransaction() = runTest {
        val repo = repo()
        val plant = repo.plantedSeed()
        db.gardenDao().upsertInventory(InventoryEntity(ItemIds.material(MaterialIds.WIND_STONE), 2))

        repo.applyMaterial(plant, MaterialIds.WIND_STONE, count = 2)
        assertEquals(0, repo.snapshot().count(ItemIds.material(MaterialIds.WIND_STONE)))
        assertEquals(10, repo.snapshot().plants.single().state.stageTraitPoints.values.single())

        val tooMany = runCatching { repo.applyMaterial(plant, MaterialIds.WIND_STONE, count = 1) }
        assertTrue(tooMany.exceptionOrNull() is IllegalStateException)
        assertEquals(10, repo.snapshot().plants.single().state.stageTraitPoints.values.single())
    }

    private companion object {
        const val DB_NAME = "garden-test.db"
    }
}
