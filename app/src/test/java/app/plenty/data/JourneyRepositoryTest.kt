package app.plenty.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.plenty.core.content.GrowthStage
import app.plenty.core.content.RegionId
import app.plenty.core.content.SpeciesIds
import app.plenty.core.content.TraitIds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.random.Random

/** 05 saves a ready selection only; the journey starts on departure and 08 reads stored snapshots. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JourneyRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val clock = Clock.fixed(Instant.parse("2026-10-28T03:00:00Z"), ZoneId.of("Asia/Seoul"))
    private lateinit var db: PlentyDatabase
    private lateinit var repo: GardenRepository

    @Before
    fun open() {
        context.deleteDatabase(DB_NAME)
        db = Room.databaseBuilder(context, PlentyDatabase::class.java, DB_NAME).allowMainThreadQueries().build()
        repo = GardenRepository(db, clock = clock, random = Random(7))
    }

    @After
    fun close() {
        db.close()
        context.deleteDatabase(DB_NAME)
    }

    private suspend fun plantedSeed(slot: Int): Long {
        repo.ensureStarterGift()
        return repo.plantMallang(GardenRepository.STARTER_SPECIES, slot)
    }

    @Test
    fun preparingSavesCompanionsWithoutOpeningAJourney() = runTest {
        val plant = plantedSeed(slot = 1)
        repo.prepareJourney(listOf(plant))

        assertEquals(listOf(plant), repo.readyCompanions.first())
        assertNull(repo.activeJourneyId())
    }

    @Test
    fun departureTurnsTheReadySelectionIntoOneActiveJourney() = runTest {
        val plant = plantedSeed(slot = 1)
        repo.prepareJourney(listOf(plant))

        val journey = checkNotNull(repo.depart(startOdometerKm = 1000.0, source = JourneySource.VEHICLE))

        assertEquals(journey, repo.activeJourneyId())
        assertEquals(emptyList<Long>(), repo.readyCompanions.first())
        // Leaving park again (e.g. P -> D -> P -> D before arrival settles) keeps the same journey.
        assertEquals(journey, repo.depart(startOdometerKm = 1003.0, source = JourneySource.VEHICLE))
    }

    @Test
    fun departureWithoutAReadySelectionStartsNothing() = runTest {
        plantedSeed(slot = 1)
        assertNull(repo.depart(startOdometerKm = 1000.0, source = JourneySource.VEHICLE))
        assertNull(repo.activeJourneyId())
    }

    @Test
    fun settlementStoresWhat08ShowsEvenAfterThePlantChanges() = runTest {
        val plant = plantedSeed(slot = 1)
        repo.prepareJourney(listOf(plant))
        val journey = checkNotNull(repo.depart(1000.0, JourneySource.VEHICLE))
        repo.settleJourney(journey, endOdometerKm = 1025.0, arrivalRegion = RegionId("busan"))

        val result = checkNotNull(repo.journeyResult(journey).first())
        assertEquals(JourneyResultState.Settled, result.state)
        assertEquals(25.0, checkNotNull(result.distanceKm), 1e-9)
        assertEquals(RegionId("busan"), result.arrivalRegion)
        val growth = result.growth.single()
        assertEquals(SpeciesIds.MOONBELL, growth.species)
        assertEquals(GrowthStage.SEED to GrowthStage.SPROUT, growth.stageBefore to growth.stageAfter)
        assertEquals(0 to 25, growth.xpBefore to growth.xpAfter)
        assertEquals(TraitIds.WATERSIDE, growth.prefixAfter)
        assertEquals(25, growth.traitPoints)
        // First visit to busan: material plus a guaranteed 말랑.
        assertNotNull(result.material)
        assertNotNull(result.seed)
    }

    @Test
    fun unreadableEndOdometerShowsCheckingDistanceForTheSameJourney() = runTest {
        val plant = plantedSeed(slot = 1)
        repo.prepareJourney(listOf(plant))
        val journey = checkNotNull(repo.depart(1000.0, JourneySource.VEHICLE))

        assertNull(repo.unseenResultId.first())
        assertEquals(SettlementView.Pending, repo.settleJourney(journey, endOdometerKm = null, arrivalRegion = null))
        assertEquals(JourneyResultState.CheckingDistance, repo.journeyResult(journey).first()?.state)
        assertEquals(journey, repo.unseenResultId.first())

        repo.markResultSeen(journey)
        assertNull(repo.unseenResultId.first())
        repo.settleJourney(journey, endOdometerKm = 1004.0, arrivalRegion = null)
        val result = checkNotNull(repo.journeyResult(journey).first())
        assertEquals(JourneyResultState.Settled, result.state)
        assertNull(result.arrivalRegion)
        assertEquals(0, result.growth.single().traitPoints)
        assertEquals(listOf(journey), repo.journal.first().map { it.journeyId })
    }

    private companion object {
        const val DB_NAME = "journey-test.db"
    }
}
