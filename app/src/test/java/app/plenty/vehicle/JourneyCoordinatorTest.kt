package app.plenty.vehicle

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.plenty.core.content.RegionId
import app.plenty.data.GardenRepository
import app.plenty.data.JourneyResultState
import app.plenty.data.JourneySource
import app.plenty.data.PlentyDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

/** Gear scenarios from INTERACTIONS.md §05/06 against a real database, with short debounces. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JourneyCoordinatorTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val gear = MutableStateFlow(GearPosition.UNKNOWN)
    @Volatile private var odometerKm: Double? = 1000.0
    @Volatile private var region: RegionId? = null
    private lateinit var db: PlentyDatabase
    private lateinit var repo: GardenRepository

    @Before
    fun open() {
        context.deleteDatabase(DB_NAME)
        db = Room.databaseBuilder(context, PlentyDatabase::class.java, DB_NAME).build()
        repo = GardenRepository(db, random = Random(7))
    }

    @After
    fun close() {
        scope.cancel()
        db.close()
        context.deleteDatabase(DB_NAME)
    }

    private fun startCoordinator() = JourneyCoordinator(
        repository = repo,
        gear = gear,
        readOdometer = { odometerKm },
        arrivalRegion = { region },
        scope = scope,
        departureDebounceMs = DEBOUNCE_MS,
        arrivalDebounceMs = DEBOUNCE_MS * 2,
        pendingRetryMs = DEBOUNCE_MS,
    ).start()

    private suspend fun readyPlant(): Long {
        repo.ensureStarterGift()
        val plant = repo.plantMallang(GardenRepository.STARTER_SPECIES, 1)
        repo.prepareJourney(listOf(plant))
        return plant
    }

    private suspend fun eventually(condition: suspend () -> Boolean) {
        repeat(100) {
            if (condition()) return
            delay(20)
        }
        throw AssertionError("condition not met within 2 s")
    }

    /** Long enough for every debounce to fire and the coordinator to act. */
    private suspend fun settle() = delay(DEBOUNCE_MS * 6)

    @Test
    fun parkDriveParkSettlesTheReadyCompanionsOnce() = runBlocking {
        readyPlant()
        gear.value = GearPosition.PARK
        startCoordinator()
        settle()

        gear.value = GearPosition.IN_GEAR
        eventually { repo.activeJourneyId() != null }
        val journey = checkNotNull(repo.activeJourneyId())
        odometerKm = 1012.5
        region = RegionId("seoul")
        gear.value = GearPosition.PARK
        eventually { repo.activeJourneyId() == null }

        val result = checkNotNull(repo.journeyResult(journey).first())
        assertEquals(JourneyResultState.Settled, result.state)
        assertEquals(12.5, checkNotNull(result.distanceKm), 1e-9)
        assertEquals(RegionId("seoul"), result.arrivalRegion)
        assertEquals(journey, repo.unseenResultId.first())
        assertEquals(emptyList<Long>(), repo.readyCompanions.first())
    }

    @Test
    fun startingWhileAlreadyInGearNeverDeparts() = runBlocking {
        val plant = readyPlant()
        gear.value = GearPosition.IN_GEAR
        startCoordinator()
        settle()

        assertNull(repo.activeJourneyId())
        assertEquals(listOf(plant), repo.readyCompanions.first())
    }

    @Test
    fun aShortBlipOutOfParkIsNotADeparture() = runBlocking {
        readyPlant()
        gear.value = GearPosition.PARK
        startCoordinator()
        settle()

        gear.value = GearPosition.IN_GEAR
        delay(DEBOUNCE_MS / 5)
        gear.value = GearPosition.PARK
        settle()

        assertNull(repo.activeJourneyId())
        assertTrue(repo.journal.first().isEmpty())
    }

    @Test
    fun anOpenJourneyFoundWhileParkedSettlesOnRelaunch() = runBlocking {
        repo.ensureStarterGift()
        val plant = repo.plantMallang(GardenRepository.STARTER_SPECIES, 1)
        val journey = repo.startJourney(listOf(plant), startOdometerKm = 990.0, source = JourneySource.VEHICLE)
        gear.value = GearPosition.PARK
        startCoordinator()

        eventually { repo.activeJourneyId() == null }
        assertEquals(10.0, checkNotNull(repo.journeyResult(journey).first()?.distanceKm), 1e-9)
    }

    @Test
    fun anUnreadableOdometerAtArrivalIsRetriedForTheSameJourney() = runBlocking {
        readyPlant()
        gear.value = GearPosition.PARK
        startCoordinator()
        settle()
        gear.value = GearPosition.IN_GEAR
        eventually { repo.activeJourneyId() != null }
        val journey = checkNotNull(repo.activeJourneyId())

        odometerKm = null
        gear.value = GearPosition.PARK
        eventually { repo.journeyResult(journey).first()?.state == JourneyResultState.CheckingDistance }
        odometerKm = 1007.0
        eventually { repo.journeyResult(journey).first()?.state == JourneyResultState.Settled }
        assertEquals(7.0, checkNotNull(repo.journeyResult(journey).first()?.distanceKm), 1e-9)
    }

    private companion object {
        const val DB_NAME = "coordinator-test.db"
        const val DEBOUNCE_MS = 100L
    }
}
