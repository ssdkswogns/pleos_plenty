package app.plenty.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.plenty.PlentyApplication
import app.plenty.core.content.MaterialId
import app.plenty.core.content.SpeciesId
import app.plenty.data.GardenRepository
import app.plenty.data.GardenSnapshot
import app.plenty.data.JourneyLogEntry
import app.plenty.data.JourneyResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PlantOutcome {
    data class Planted(val species: SpeciesId, val slot: Int) : PlantOutcome

    /** The pot was taken or the 말랑 ran out meanwhile; nothing was spent. */
    data object StateChanged : PlantOutcome

    data object Failed : PlantOutcome
}

sealed interface MaterialOutcome {
    data class Applied(val material: MaterialId, val count: Int) : MaterialOutcome

    /** A settlement made the plant mature meanwhile; its name is final and nothing was spent. */
    data object AlreadyMature : MaterialOutcome

    /** Not enough materials anymore; nothing was spent. */
    data object StateChanged : MaterialOutcome

    data object Failed : MaterialOutcome
}

/**
 * One garden state for every screen; screens map it to their own UI state and never write directly.
 * Every write re-checks quantities inside a repository transaction; [busy] blocks double submits.
 */
class PlentyViewModel(private val repository: GardenRepository) : ViewModel() {

    /** Null until the first read from storage completes. */
    val garden: StateFlow<GardenSnapshot?> = repository.garden
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    /** Companions saved on 05; null until loaded, empty when nothing is ready. */
    val readyCompanions: StateFlow<List<Long>?> = repository.readyCompanions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    /** Newest journey whose result screen has not been shown yet. */
    val unseenResultId: StateFlow<Long?> = repository.unseenResultId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    val journal: StateFlow<List<JourneyLogEntry>?> = repository.journal
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    fun journeyResult(journeyId: Long): Flow<JourneyResult?> = repository.journeyResult(journeyId)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    init {
        viewModelScope.launch { repository.ensureStarterGift() }
    }

    fun plant(species: SpeciesId, slot: Int, onResult: (PlantOutcome) -> Unit) {
        if (!_busy.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            val outcome = try {
                repository.plantMallang(species, slot)
                PlantOutcome.Planted(species, slot)
            } catch (e: IllegalStateException) {
                PlantOutcome.StateChanged
            } catch (e: Exception) {
                Log.w(TAG, "planting $species in $slot failed", e)
                PlantOutcome.Failed
            }
            _busy.value = false
            onResult(outcome)
        }
    }

    /** Not retried automatically: the repository has no request-id idempotency for materials. */
    fun applyMaterial(plantId: Long, material: MaterialId, count: Int, onResult: (MaterialOutcome) -> Unit) {
        if (!_busy.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            val outcome = try {
                repository.applyMaterial(plantId, material, count)
                MaterialOutcome.Applied(material, count)
            } catch (e: IllegalArgumentException) {
                MaterialOutcome.AlreadyMature
            } catch (e: IllegalStateException) {
                MaterialOutcome.StateChanged
            } catch (e: Exception) {
                Log.w(TAG, "applying $count $material to $plantId failed", e)
                MaterialOutcome.Failed
            }
            _busy.value = false
            onResult(outcome)
        }
    }

    /** Saves 05's choice only; the journey starts when the vehicle leaves park (JourneyCoordinator). */
    fun prepareJourney(companionIds: List<Long>, onResult: (saved: Boolean) -> Unit) {
        if (!_busy.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            val saved = try {
                repository.prepareJourney(companionIds)
                true
            } catch (e: Exception) {
                Log.w(TAG, "preparing a journey with $companionIds failed", e)
                false
            }
            _busy.value = false
            onResult(saved)
        }
    }

    fun markResultSeen(journeyId: Long) {
        viewModelScope.launch { repository.markResultSeen(journeyId) }
    }

    companion object {
        private const val TAG = "PlentyViewModel"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PlentyApplication
                PlentyViewModel(app.container.gardenRepository)
            }
        }
    }
}
