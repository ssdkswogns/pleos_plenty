package app.plenty.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.plenty.R
import app.plenty.ui.art.LocalPlantAtlas
import app.plenty.ui.art.WorldBackdrop
import app.plenty.ui.components.GroveNavigationRail
import app.plenty.ui.components.TopDestination
import app.plenty.ui.garden.GardenScreen
import app.plenty.ui.garden.GardenStatusChips
import app.plenty.ui.garden.MockGardenState
import app.plenty.ui.theme.PlentySpace
import app.plenty.ui.theme.PlentyTheme

private const val SettingsRoute = "settings"
private const val PlantDetailRoute = "plant/{slot}"
private const val CompanionsRoute = "companions"

/** App shell: backdrop + rail + header + destination content. */
@Composable
fun PlentyApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selected = tabOf(currentRoute)
    val onGarden = currentRoute == TopDestination.Garden.route
    // Layout mock until the repository lands (T05).
    val gardenState = MockGardenState

    CompositionLocalProvider(LocalPlantAtlas provides ImageBitmap.imageResource(R.drawable.plant_atlas)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(PlentyTheme.colors.background),
        ) {
            if (onGarden) WorldBackdrop(Modifier.fillMaxSize())
            Row(Modifier.fillMaxSize().padding(start = 24.dp, top = 24.dp, end = 48.dp, bottom = 28.dp)) {
                GroveNavigationRail(
                    selected = selected,
                    onSelect = { destination ->
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
                    PlentyHeader(actions = { if (onGarden) GardenStatusChips(gardenState) })
                    NavHost(
                        navController = navController,
                        startDestination = TopDestination.Garden.route,
                        modifier = Modifier.weight(1f),
                    ) {
                        composable(TopDestination.Garden.route) {
                            GardenScreen(
                                state = gardenState,
                                onPotClick = { slot -> navController.navigate("plant/$slot") },
                                onEmptyPotClick = { navController.navigateTopLevel(TopDestination.Bag.route) },
                                onPrepareJourney = { navController.navigate(CompanionsRoute) },
                            )
                        }
                        composable(TopDestination.Collection.route) {
                            PlaceholderScreen(title = stringResource(R.string.nav_collection))
                        }
                        composable(TopDestination.Bag.route) {
                            PlaceholderScreen(title = stringResource(R.string.nav_bag))
                        }
                        composable(TopDestination.Journey.route) {
                            PlaceholderScreen(title = stringResource(R.string.nav_journey))
                        }
                        composable(SettingsRoute) {
                            PlaceholderScreen(title = stringResource(R.string.nav_settings))
                        }
                        composable(PlantDetailRoute) {
                            PlaceholderScreen(title = stringResource(R.string.screen_plant_detail))
                        }
                        composable(CompanionsRoute) {
                            PlaceholderScreen(title = stringResource(R.string.screen_companions))
                        }
                    }
                }
            }
        }
    }
}

/** Rail tab a route belongs to. Nested screens keep their tab selected, as 10-plant-detail shows 정원. */
private fun tabOf(route: String?): TopDestination? = when (route) {
    PlantDetailRoute, CompanionsRoute -> TopDestination.Garden
    else -> TopDestination.entries.firstOrNull { it.route == route }
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
