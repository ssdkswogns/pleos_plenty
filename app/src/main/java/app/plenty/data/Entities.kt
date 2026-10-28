package app.plenty.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A potted plant. [slot] is the cart pot 1..6 and is unique, so two plants can never share a pot. */
@Entity(tableName = "plants", indices = [Index(value = ["slot"], unique = true)])
data class PlantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val species: String,
    val stage: String,
    val xp: Int,
    val xpRemainder: Double,
    /** Current-stage trait points, encoded by [TraitPointsCodec]. */
    val stageTraitPoints: String,
    val sproutPrefix: String?,
    val maturePrefix: String?,
    val slot: Int?,
    val createdAt: Long,
)

/** Bag contents. [itemId] is `mallang:<species>` or `material:<id>`; see [ItemIds]. */
@Entity(tableName = "inventory")
data class InventoryEntity(
    @PrimaryKey val itemId: String,
    val quantity: Int,
)

/**
 * One journey from departure to settlement. Companions and the start odometer are fixed at
 * departure; reward columns hold the one settled result so it is shown, never re-rolled.
 */
@Entity(tableName = "journeys")
data class JourneyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val status: String,
    /** Where the numbers came from; mock journeys are never presented as real vehicle data. */
    val source: String,
    val companionIds: String,
    val startOdometerKm: Double?,
    val endOdometerKm: Double?,
    val arrivalRegion: String?,
    val startedAt: Long,
    val settledAt: Long?,
    val distanceKm: Double?,
    val rewardMaterial: String?,
    val rewardMaterialCount: Int,
    val rewardSeed: String?,
    val rulesVersion: Int?,
)

@Entity(tableName = "journey_growth", primaryKeys = ["journeyId", "plantId"])
data class JourneyGrowthEntity(
    val journeyId: Long,
    val plantId: Long,
    val xpGained: Int,
    val stageBefore: String,
    val stageAfter: String,
)

/** One row per granted reward. The primary key is the idempotency guard for every grant. */
@Entity(tableName = "reward_ledger")
data class RewardLedgerEntity(
    @PrimaryKey val rewardKey: String,
    val journeyId: Long?,
    val rulesVersion: Int,
    val grantedAt: Long,
)

@Entity(tableName = "daily_xp", primaryKeys = ["day", "plantId"])
data class DailyXpEntity(
    val day: String,
    val plantId: Long,
    val xp: Int,
)

@Entity(tableName = "region_claims", primaryKeys = ["regionId", "day"])
data class RegionClaimEntity(
    val regionId: String,
    val day: String,
    val journeyId: Long,
)

object JourneyStatus {
    const val ACTIVE = "ACTIVE"
    const val SETTLED = "SETTLED"
    const val REJECTED = "REJECTED"
}

enum class JourneySource { VEHICLE, MOCK }
