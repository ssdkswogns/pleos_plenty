package app.plenty.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.plenty.PlentyApplication
import app.plenty.core.GardenLayout
import app.plenty.core.content.SpeciesId
import app.plenty.data.GardenRepository
import app.plenty.data.GardenSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One garden state for every screen; screens map it to their own UI state and never write directly. */
class PlentyViewModel(private val repository: GardenRepository) : ViewModel() {

    /** Null until the first read from storage completes. */
    val garden: StateFlow<GardenSnapshot?> = repository.garden
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue = null)

    init {
        viewModelScope.launch { repository.ensureStarterGift() }
    }

    /** Plants one 말랑 into the first empty pot; [onPlanted] gets the slot. Does nothing when pots are full. */
    fun plant(species: SpeciesId, onPlanted: (slot: Int) -> Unit) {
        val occupied = garden.value?.plants?.map { it.slot }?.toSet() ?: return
        val slot = (1..GardenLayout.SLOT_COUNT).firstOrNull { it !in occupied } ?: return
        viewModelScope.launch {
            runCatching { repository.plantMallang(species, slot) }
                .onSuccess { onPlanted(slot) }
                .onFailure { Log.w(TAG, "planting $species failed", it) }
        }
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
