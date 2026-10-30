package app.plenty.ui

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.plenty.R
import app.plenty.core.GardenLayout
import app.plenty.core.content.DefaultContent
import app.plenty.core.content.MaterialId
import app.plenty.core.content.SpeciesId
import app.plenty.data.ItemIds
import app.plenty.ui.art.LocalPlantAtlas
import app.plenty.ui.art.LocalSceneArt
import app.plenty.ui.art.WorldBackdrop
import app.plenty.ui.art.rememberSceneArt
import app.plenty.ui.bag.BagScreen
import app.plenty.ui.bag.MaterialSheet
import app.plenty.ui.bag.toBagUiState
import app.plenty.ui.collection.CollectionScreen
import app.plenty.ui.collection.SpeciesDetailScreen
import app.plenty.ui.collection.toCollectionUiState
import app.plenty.ui.components.GroveNavigationRail
import app.plenty.ui.components.ToastHost
import app.plenty.ui.components.ToastState
import app.plenty.ui.components.TopDestination
import app.plenty.ui.garden.GardenScreen
import app.plenty.ui.garden.GardenStatusChips
import app.plenty.ui.garden.toGardenUiState
import app.plenty.ui.journey.CompanionsScreen
import app.plenty.ui.journey.GrowthCompleteScreen
import app.plenty.ui.journey.JournalScreen
import app.plenty.ui.journey.JourneyReadyScreen
import app.plenty.ui.journey.JourneyResultScreen
import app.plenty.ui.journey.SeedDiscoveryScreen
import app.plenty.ui.motion.rememberDrivingRestriction
import app.plenty.ui.motion.rememberHomeMotion
import app.plenty.ui.motion.rememberHomeMotionAllowed
import app.plenty.ui.plant.PlantConfirmScreen
import app.plenty.ui.plant.PlantDetailScreen
import app.plenty.ui.plant.TraitTuningScreen
import app.plenty.ui.theme.PlentySpace
import app.plenty.ui.theme.PlentyTheme
import app.plenty.vehicle.DrivingRestriction
import kotlinx.coroutines.delay

private const val SettingsRoute = "settings"
private const val PlantDetailRoute = "plant/{slot}"
private const val TuneRoute = "plant/{slot}/tune"
private const val ConfirmRoute = "plant-confirm/{species}?slot={slot}&from={from}"
private const val SpeciesRoute = "species/{species}"
private const val CompanionsRoute = "companions?with={with}"
private const val ReadyRoute = "journey-ready"
private const val ResultRoute = "journey/{id}?fresh={fresh}"
private const val GrowthRoute = "journey/{id}/growth"
private const val DiscoveryRoute = "journey/{id}/discovery"
private const val FROM_BAG = "bag"
private const val FROM_GARDEN = "garden"
private const val FROM_DISCOVERY = "discovery"

/** App shell: backdrop + rail + header + destination content, plus the sheet and toast layers. */
@Composable
fun PlentyApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selected = tabOf(backStackEntry)
    val onGarden = currentRoute == TopDestination.Garden.route
    val viewModel: PlentyViewModel = viewModel(factory = PlentyViewModel.Factory)
    val snapshot by viewModel.garden.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val readyIds by viewModel.readyCompanions.collectAsStateWithLifecycle()
    val unseenResult by viewModel.unseenResultId.collectAsStateWithLifecycle()
    val journal by viewModel.journal.collectAsStateWithLifecycle()
    val gardenState = remember(snapshot) { snapshot?.toGardenUiState() }
    val restriction by rememberDrivingRestriction()
    val motionAllowed = rememberHomeMotionAllowed(restriction)
    val motion = rememberHomeMotion(active = onGarden && motionAllowed)
    val content = DefaultContent
    val resources = LocalContext.current.resources
    val toast = remember { ToastState() }

    // Empty pot tapped on the garden: 11 preselects it after the user picks a 말랑 in the bag.
    var preferredSlot by rememberSaveable { mutableStateOf<Int?>(null) }
    // Pot to emphasize once the garden is visible again after planting (INTERACTIONS.md §11).
    var highlightSlot by rememberSaveable { mutableStateOf<Int?>(null) }
    var sheetMaterial by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmChangedKey by rememberSaveable { mutableIntStateOf(0) }
    var lastShownResult by rememberSaveable { mutableStateOf<Long?>(null) }

    // Parked again with a new result: open 08 once (INTERACTIONS.md §08). It is marked seen right
    // away so re-entering later shows the stored result without replaying 13/09.
    LaunchedEffect(unseenResult, restriction) {
        val journeyId = unseenResult ?: return@LaunchedEffect
        if (restriction != DrivingRestriction.ALLOWED || journeyId == lastShownResult) return@LaunchedEffect
        lastShownResult = journeyId
        viewModel.markResultSeen(journeyId)
        sheetMaterial = null
        // Whatever was open before driving (often 06) is stale now that the selection departed.
        navController.popBackStack(TopDestination.Garden.route, inclusive = false)
        navController.navigateTopLevel(TopDestination.Journey.route)
        navController.popBackStack(TopDestination.Journey.route, inclusive = false)
        navController.navigate("journey/$journeyId?fresh=true")
    }

    LaunchedEffect(highlightSlot, onGarden, motion.running) {
        val slot = highlightSlot ?: return@LaunchedEffect
        if (!onGarden) return@LaunchedEffect
        if (motion.running) {
            delay(350)
            motion.wiggle(slot - 1)
        }
        highlightSlot = null
    }

    CompositionLocalProvider(
        LocalPlantAtlas provides ImageBitmap.imageResource(R.drawable.plant_atlas),
        LocalSceneArt provides rememberSceneArt(),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(PlentyTheme.colors.background),
        ) {
            if (onGarden) WorldBackdrop(motion, Modifier.fillMaxSize())
            Row(Modifier.fillMaxSize().padding(start = 24.dp, top = 24.dp, end = 48.dp, bottom = 28.dp)) {
                GroveNavigationRail(
                    selected = selected,
                    onSelect = { destination ->
                        preferredSlot = null
                        // Re-selecting the current tab returns to its first screen. Restoring its
                        // saved stack instead would reopen the nested screen (e.g. plant detail).
                        if (destination == selected) {
                            navController.popBackStack(destination.route, inclusive = false)
                        } else {
                            navController.navigateTopLevel(destination.route)
                        }
                    },
                    onSettings = { navController.navigate(SettingsRoute) { launchSingleTop = true } },
                    modifier = Modifier.padding(top = 88.dp),
                )
                Spacer(Modifier.width(40.dp))
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    val showMaterials = currentRoute == TopDestination.Garden.route || currentRoute == TopDestination.Bag.route
                    PlentyHeader(actions = { gardenState?.let { GardenStatusChips(it, includeMaterials = showMaterials) } })
                    NavHost(
                        navController = navController,
                        startDestination = TopDestination.Garden.route,
                        modifier = Modifier.weight(1f),
                    ) {
                        composable(TopDestination.Garden.route) {
                            gardenState?.let { state ->
                                GardenScreen(
                                    state = state,
                                    motion = motion,
                                    onPotClick = { slot -> navController.navigate("plant/$slot") },
                                    onEmptyPotClick = { slot ->
                                        preferredSlot = slot
                                        navController.navigateTopLevel(TopDestination.Bag.route)
                                    },
                                    onPrepareJourney = {
                                        navController.navigate(if (readyIds.isNullOrEmpty()) "companions" else ReadyRoute)
                                    },
                                    readyCount = readyIds?.size ?: 0,
                                )
                            }
                        }
                        composable(TopDestination.Collection.route) {
                            snapshot?.let { garden ->
                                CollectionScreen(
                                    state = garden.toCollectionUiState(content),
                                    onOpenSpecies = { species -> navController.navigate("species/${species.value}") },
                                    content = content,
                                )
                            }
                        }
                        composable(TopDestination.Bag.route) {
                            snapshot?.let { garden ->
                                BagScreen(
                                    state = garden.toBagUiState(content),
                                    onPlant = { species ->
                                        navController.navigate("plant-confirm/${species.value}?slot=${preferredSlot ?: -1}&from=$FROM_BAG")
                                    },
                                    onOpenMaterial = { sheetMaterial = it.value },
                                )
                            }
                        }
                        composable(TopDestination.Journey.route) {
                            journal?.let { entries ->
                                JournalScreen(
                                    entries = entries,
                                    readyCount = readyIds?.size ?: 0,
                                    content = content,
                                    onOpen = { id -> navController.navigate("journey/$id") },
                                    onPrepare = { navController.navigate("companions") },
                                    onOpenReady = { navController.navigate(ReadyRoute) },
                                )
                            }
                        }
                        composable(SettingsRoute) {
                            PlaceholderScreen(title = stringResource(R.string.nav_settings))
                        }
                        composable(PlantDetailRoute, arguments = listOf(navArgument("slot") { type = NavType.IntType })) { entry ->
                            val slot = entry.arguments?.getInt("slot")
                            val plant = snapshot?.plants?.firstOrNull { it.slot == slot }
                            if (plant != null) {
                                PlantDetailScreen(
                                    plant = plant,
                                    content = content,
                                    onBack = { navController.popBackStack() },
                                    onTune = { navController.navigate("plant/$slot/tune") },
                                    onCompanions = { navController.navigate("companions?with=${plant.state.id}") },
                                )
                            }
                        }
                        composable(TuneRoute, arguments = listOf(navArgument("slot") { type = NavType.IntType })) { entry ->
                            val slot = entry.arguments?.getInt("slot")
                            val garden = snapshot
                            val plant = garden?.plants?.firstOrNull { it.slot == slot }
                            if (garden != null && plant != null) {
                                TraitTuningScreen(
                                    plant = plant,
                                    garden = garden,
                                    content = content,
                                    busy = busy,
                                    onBack = { navController.popBackStack() },
                                    onApply = { material, count ->
                                        viewModel.applyMaterial(plant.state.id, material, count) { outcome ->
                                            navController.popBackStack()
                                            toast.show(materialOutcomeText(outcome, resources))
                                        }
                                    },
                                )
                            }
                        }
                        composable(
                            ConfirmRoute,
                            arguments = listOf(
                                navArgument("species") { type = NavType.StringType },
                                navArgument("slot") { type = NavType.IntType; defaultValue = -1 },
                                navArgument("from") { type = NavType.StringType; defaultValue = FROM_BAG },
                            ),
                        ) { entry ->
                            val species = SpeciesId(checkNotNull(entry.arguments?.getString("species")))
                            val from = entry.arguments?.getString("from")
                            snapshot?.let { garden ->
                                PlantConfirmScreen(
                                    species = species,
                                    garden = garden,
                                    preferredSlot = entry.arguments?.getInt("slot")?.takeIf { it > 0 },
                                    backTo = stringResource(
                                        when (from) {
                                            FROM_GARDEN -> R.string.nav_garden
                                            FROM_DISCOVERY -> R.string.discovery_back
                                            else -> R.string.nav_bag
                                        },
                                    ),
                                    cancelLabel = stringResource(
                                        when (from) {
                                            FROM_GARDEN -> R.string.confirm_to_garden
                                            FROM_DISCOVERY -> R.string.confirm_to_discovery
                                            else -> R.string.confirm_to_bag
                                        },
                                    ),
                                    busy = busy,
                                    stateChangedKey = confirmChangedKey,
                                    onCancel = { navController.popBackStack() },
                                    onConfirm = { slot ->
                                        viewModel.plant(species, slot) { outcome ->
                                            when (outcome) {
                                                is PlantOutcome.Planted -> {
                                                    preferredSlot = null
                                                    highlightSlot = outcome.slot
                                                    // Close the confirm first so the caller's saved tab stack
                                                    // doesn't reopen it later (rail 가방 would restore it).
                                                    navController.popBackStack()
                                                    // From 09 the journey screens are done too.
                                                    if (from == FROM_DISCOVERY) {
                                                        navController.leaveToGarden()
                                                    } else {
                                                        navController.showGardenHome()
                                                    }
                                                    toast.show(
                                                        resources.getString(
                                                            R.string.planted_toast,
                                                            resources.getString(ContentRes.speciesName(outcome.species)),
                                                        ),
                                                    )
                                                }
                                                PlantOutcome.StateChanged -> confirmChangedKey++
                                                PlantOutcome.Failed -> toast.show(resources.getString(R.string.action_failed))
                                            }
                                        }
                                    },
                                )
                            }
                        }
                        composable(SpeciesRoute, arguments = listOf(navArgument("species") { type = NavType.StringType })) { entry ->
                            val species = SpeciesId(checkNotNull(entry.arguments?.getString("species")))
                            val speciesEntry = snapshot?.toCollectionUiState(content)?.entries?.firstOrNull { it.species == species }
                            if (speciesEntry?.best != null) {
                                SpeciesDetailScreen(speciesEntry, content, onBack = { navController.popBackStack() })
                            }
                        }
                        composable(
                            CompanionsRoute,
                            arguments = listOf(navArgument("with") { type = NavType.LongType; defaultValue = -1L }),
                        ) { entry ->
                            val garden = snapshot
                            val ready = readyIds
                            if (garden != null && ready != null) {
                                val with = entry.arguments?.getLong("with")?.takeIf { it > 0 }
                                CompanionsScreen(
                                    garden = garden,
                                    initialSelection = (ready + listOfNotNull(with)).distinct().take(GardenLayout.MAX_COMPANIONS),
                                    busy = busy,
                                    backTo = backLabel(navController.previousBackStackEntry),
                                    content = content,
                                    onBack = { navController.popBackStack() },
                                    onConfirm = { ids ->
                                        viewModel.prepareJourney(ids) { saved ->
                                            if (saved) {
                                                navController.navigate(ReadyRoute) { popUpTo(CompanionsRoute) { inclusive = true } }
                                            } else {
                                                toast.show(resources.getString(R.string.action_failed))
                                            }
                                        }
                                    },
                                    onOpenBag = { navController.navigateTopLevel(TopDestination.Bag.route) },
                                )
                            }
                        }
                        composable(ReadyRoute) {
                            val garden = snapshot
                            val ready = readyIds
                            if (garden != null && ready != null) {
                                JourneyReadyScreen(
                                    companions = ready.mapNotNull { id -> garden.plants.firstOrNull { it.state.id == id } },
                                    backTo = backLabel(navController.previousBackStackEntry),
                                    content = content,
                                    onBack = { navController.popBackStack() },
                                    onChange = { navController.navigate("companions") { popUpTo(ReadyRoute) { inclusive = true } } },
                                )
                            }
                        }
                        composable(
                            ResultRoute,
                            arguments = listOf(
                                navArgument("id") { type = NavType.LongType },
                                navArgument("fresh") { type = NavType.BoolType; defaultValue = false },
                            ),
                        ) { entry ->
                            val journeyId = checkNotNull(entry.arguments?.getLong("id"))
                            val fresh = entry.arguments?.getBoolean("fresh") == true
                            val result by remember(journeyId) { viewModel.journeyResult(journeyId) }.collectAsStateWithLifecycle(null)
                            var growthShown by rememberSaveable { mutableStateOf(false) }
                            // 13 plays once, the first time this result is shown after parking.
                            LaunchedEffect(result) {
                                val settled = result ?: return@LaunchedEffect
                                if (fresh && !growthShown && settled.growth.any { it.grew }) {
                                    growthShown = true
                                    navController.navigate("journey/$journeyId/growth")
                                }
                            }
                            result?.let {
                                JourneyResultScreen(
                                    result = it,
                                    content = content,
                                    onBack = { navController.popBackStack() },
                                    onMeet = { navController.navigate("journey/$journeyId/discovery") },
                                    onGarden = { navController.leaveToGarden() },
                                )
                            }
                        }
                        composable(GrowthRoute, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                            val journeyId = checkNotNull(entry.arguments?.getLong("id"))
                            val result by remember(journeyId) { viewModel.journeyResult(journeyId) }.collectAsStateWithLifecycle(null)
                            result?.let {
                                GrowthCompleteScreen(
                                    grown = it.growth.filter { growth -> growth.grew },
                                    content = content,
                                    onDone = { navController.popBackStack() },
                                )
                            }
                        }
                        composable(DiscoveryRoute, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                            val journeyId = checkNotNull(entry.arguments?.getLong("id"))
                            val result by remember(journeyId) { viewModel.journeyResult(journeyId) }.collectAsStateWithLifecycle(null)
                            val seed = result?.seed
                            val garden = snapshot
                            if (seed != null && garden != null) {
                                SeedDiscoveryScreen(
                                    species = seed,
                                    inBag = garden.count(ItemIds.mallang(seed)),
                                    hasEmptyPot = garden.plants.size < GardenLayout.SLOT_COUNT,
                                    onBack = { navController.popBackStack() },
                                    onPlant = { navController.navigate("plant-confirm/${seed.value}?slot=-1&from=$FROM_DISCOVERY") },
                                    onLater = { navController.leaveToGarden() },
                                )
                            }
                        }
                    }
                }
            }

            val material = sheetMaterial?.let(::MaterialId)
            if (material != null && currentRoute == TopDestination.Bag.route) {
                MaterialSheet(
                    material = material,
                    owned = snapshot?.count(ItemIds.material(material)) ?: 0,
                    content = content,
                    onDismiss = { sheetMaterial = null },
                )
            }
            // In the header band between logo and status chips: never over titles or bottom actions.
            ToastHost(toast, Modifier.align(Alignment.TopCenter).padding(top = 24.dp))
        }
    }
}

private fun materialOutcomeText(outcome: MaterialOutcome, resources: Resources): String = when (outcome) {
    is MaterialOutcome.Applied -> {
        val trait = DefaultContent.traitBoostedBy(outcome.material)
        resources.getString(
            R.string.tune_done_toast,
            trait?.label.orEmpty(),
            outcome.count * DefaultContent.growth.traitPointsPerMaterial,
            resources.getString(ContentRes.materialName(outcome.material)),
            outcome.count,
        )
    }
    MaterialOutcome.AlreadyMature -> resources.getString(R.string.tune_became_mature)
    MaterialOutcome.StateChanged -> resources.getString(R.string.confirm_changed)
    MaterialOutcome.Failed -> resources.getString(R.string.action_failed)
}

/**
 * Rail tab of the current screen (INTERACTIONS.md 내비게이션): plant screens, 05 and 06 stay under
 * 정원, 08/09/13 under 여정, species detail under 도감, and the plant confirm under whoever opened it.
 */
private fun tabOf(entry: NavBackStackEntry?): TopDestination? = when (entry?.destination?.route) {
    PlantDetailRoute, TuneRoute, CompanionsRoute, ReadyRoute -> TopDestination.Garden
    ResultRoute, GrowthRoute, DiscoveryRoute -> TopDestination.Journey
    SpeciesRoute -> TopDestination.Collection
    ConfirmRoute -> when (entry.arguments?.getString("from")) {
        FROM_GARDEN -> TopDestination.Garden
        FROM_DISCOVERY -> TopDestination.Journey
        else -> TopDestination.Bag
    }
    else -> TopDestination.entries.firstOrNull { it.route == entry?.destination?.route }
}

/** "뒤로, {screen}" target for 05/06, which open from the garden, plant detail or the journey tab. */
@Composable
private fun backLabel(previous: NavBackStackEntry?): String = stringResource(
    when (previous?.destination?.route) {
        PlantDetailRoute -> R.string.screen_plant_detail
        TopDestination.Journey.route -> R.string.nav_journey
        else -> R.string.nav_garden
    },
)

/** Ends the journey screens: the 여정 tab keeps only its log, then the garden home opens. */
private fun NavHostController.leaveToGarden() {
    popBackStack(TopDestination.Journey.route, inclusive = false)
    showGardenHome()
}

/** The garden home itself, not whatever nested screen the 정원 tab saved last. */
private fun NavHostController.showGardenHome() {
    navigateTopLevel(TopDestination.Garden.route)
    popBackStack(TopDestination.Garden.route, inclusive = false)
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun PlentyHeader(actions: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_garden),
            contentDescription = null,
            tint = PlentyTheme.colors.primary,
            modifier = Modifier.size(33.dp),
        )
        Spacer(Modifier.width(13.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = PlentyTheme.typography.label.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
            color = PlentyTheme.colors.primary,
        )
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.padding(end = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

@Composable
private fun PlaceholderScreen(title: String, body: String = stringResource(R.string.placeholder_body)) {
    Column(Modifier.fillMaxSize().padding(start = 4.dp, top = PlentySpace.xl)) {
        Text(text = title, style = PlentyTheme.typography.title, color = PlentyTheme.colors.text)
        Spacer(Modifier.height(PlentySpace.m))
        Text(text = body, style = PlentyTheme.typography.body, color = PlentyTheme.colors.textSecondary)
    }
}
