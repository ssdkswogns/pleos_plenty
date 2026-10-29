package app.plenty.core.content

import app.plenty.core.GardenLayout

@JvmInline value class SpeciesId(val value: String)
@JvmInline value class TraitId(val value: String)
@JvmInline value class RegionId(val value: String)
@JvmInline value class MaterialId(val value: String)

/** SEED is a 말랑 planted in a pot. 말랑 still in the bag are inventory items, not plants. */
enum class GrowthStage { SEED, SPROUT, MATURE }

/**
 * A regional mood category. It only changes the name prefix of sprouts and mature plants; it is
 * not a stat, rarity or growth speed (design/spec/DECISIONS.md §1).
 */
data class Trait(val id: TraitId, val label: String, val namePrefix: String, val boostMaterial: MaterialId?)

data class Region(val id: RegionId, val name: String, val trait: TraitId, val material: MaterialId)

/** Numbers are play-test hypotheses (GAME_RULES.md §2-3). */
data class GrowthConfig(
    val xpPerKm: Double = 1.0,
    val dailyXpCapPerPlant: Int = 50,
    val sproutXp: Int = 20,
    val matureXp: Int = 100,
    val traitPointsPerMaterial: Int = 5,
    val maxCompanions: Int = GardenLayout.MAX_COMPANIONS,
)

data class RewardConfig(
    val materialPerVisit: Int = 1,
    val repeatSeedChance: Double = 0.30,
)

data class DistanceConfig(val maxJourneyKm: Double = 2_000.0)

data class GameContent(
    val rulesVersion: Int,
    val species: List<SpeciesId>,
    /** List order breaks ties between equal trait scores. */
    val traits: List<Trait>,
    val regions: List<Region>,
    val growth: GrowthConfig = GrowthConfig(),
    val rewards: RewardConfig = RewardConfig(),
    val distance: DistanceConfig = DistanceConfig(),
) {
    fun region(id: RegionId): Region? = regions.firstOrNull { it.id == id }
    fun traitBoostedBy(material: MaterialId): Trait? = traits.firstOrNull { it.boostMaterial == material }
}

object SpeciesIds {
    val MOONBELL = SpeciesId("moonbell")
    val CLOUD_FERN = SpeciesId("cloud-fern")
    val TIDE_LOTUS = SpeciesId("tide-lotus")
}

object MaterialIds {
    val MOON_DEW = MaterialId("moon-dew")
    val CLOUD_LIGHT = MaterialId("cloud-light")
    val WIND_STONE = MaterialId("wind-stone")
}

object TraitIds {
    val WATERSIDE = TraitId("waterside")
    val HIGHLAND = TraitId("highland")
    val CITY = TraitId("city")
}

/**
 * Rules v1 with the confirmed MVP content `plenty-mvp-content-1` (design/spec/content-v1.json).
 * Trait order is the tie priority: 물가 -> 산자락 -> 도시. Region IDs must stay stable so the
 * per-region reward ledger keeps working.
 */
val DefaultContent = GameContent(
    rulesVersion = 1,
    species = listOf(SpeciesIds.MOONBELL, SpeciesIds.CLOUD_FERN, SpeciesIds.TIDE_LOTUS),
    traits = listOf(
        Trait(TraitIds.WATERSIDE, label = "물가", namePrefix = "물가의", boostMaterial = MaterialIds.MOON_DEW),
        Trait(TraitIds.HIGHLAND, label = "산자락", namePrefix = "산자락의", boostMaterial = MaterialIds.WIND_STONE),
        Trait(TraitIds.CITY, label = "도시", namePrefix = "도시의", boostMaterial = MaterialIds.CLOUD_LIGHT),
    ),
    regions = listOf(
        Region(RegionId("gangneung"), "강릉", TraitIds.WATERSIDE, MaterialIds.MOON_DEW),
        Region(RegionId("busan"), "부산", TraitIds.WATERSIDE, MaterialIds.MOON_DEW),
        Region(RegionId("pyeongchang"), "평창", TraitIds.HIGHLAND, MaterialIds.WIND_STONE),
        Region(RegionId("seoul"), "서울", TraitIds.CITY, MaterialIds.CLOUD_LIGHT),
        Region(RegionId("daejeon"), "대전", TraitIds.CITY, MaterialIds.CLOUD_LIGHT),
    ),
)
