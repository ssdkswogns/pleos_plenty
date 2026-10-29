package app.plenty.ui.bag

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plenty.R
import app.plenty.core.content.DefaultContent
import app.plenty.core.content.GameContent
import app.plenty.core.content.GrowthStage
import app.plenty.core.content.MaterialId
import app.plenty.core.content.SpeciesId
import app.plenty.data.GardenSnapshot
import app.plenty.data.ItemIds
import app.plenty.ui.ContentRes
import app.plenty.ui.art.PlantArt
import app.plenty.ui.art.PlantImage
import app.plenty.ui.components.FilterPill
import app.plenty.ui.components.PlentyCard
import app.plenty.ui.materialsInOrder
import app.plenty.ui.theme.PlentyTheme

data class BagUiState(
    val mallang: List<MallangItem>,
    val materials: List<MaterialItem>,
)

data class MallangItem(val species: SpeciesId, val count: Int)

data class MaterialItem(
    val material: MaterialId,
    val count: Int,
    /** Label of the trait this material tends ("산자락"); null if it boosts none. */
    val traitLabel: String?,
)

/** Owned 말랑 only; materials always show, with 0 when empty (components spec: consistent 0). */
fun GardenSnapshot.toBagUiState(content: GameContent = DefaultContent): BagUiState = BagUiState(
    mallang = content.species.map { MallangItem(it, count(ItemIds.mallang(it))) }.filter { it.count > 0 },
    materials = content.materialsInOrder().map {
        MaterialItem(it, count(ItemIds.material(it)), content.traitBoostedBy(it)?.label)
    },
)

private enum class BagTab(@StringRes val label: Int, @StringRes val hint: Int) {
    All(R.string.bag_tab_all, R.string.bag_hint_all),
    Mallang(R.string.bag_tab_mallang, R.string.bag_hint_mallang),
    Materials(R.string.bag_tab_materials, R.string.bag_hint_materials),
}

/** 14 여행 가방: 말랑 cards open 11 (plant), material cards open the 14a sheet. */
@Composable
fun BagScreen(
    state: BagUiState,
    onPlant: (SpeciesId) -> Unit,
    onOpenMaterial: (MaterialId) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(BagTab.All) }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 4.dp, top = 36.dp, bottom = 24.dp),
    ) {
        Text(stringResource(R.string.bag_title), style = PlentyTheme.typography.title, color = PlentyTheme.colors.text)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(tab.hint), style = PlentyTheme.typography.body.copy(fontSize = 22.sp), color = PlentyTheme.colors.textSecondary)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BagTab.entries.forEach { FilterPill(stringResource(it.label), selected = tab == it, onClick = { tab = it }) }
        }
        Spacer(Modifier.height(32.dp))
        if (tab != BagTab.Materials) {
            if (state.mallang.isEmpty()) {
                EmptyMallangCard()
            } else {
                CardGrid(state.mallang) { MallangCard(it, onPlant) }
            }
        }
        if (tab == BagTab.All) Spacer(Modifier.height(28.dp))
        if (tab != BagTab.Mallang) {
            CardGrid(state.materials) { MaterialCard(it, onOpenMaterial) }
        }
    }
}

/** Three equal columns (14/15 layout), wrapping into rows 28 apart. */
@Composable
private fun <T> CardGrid(items: List<T>, card: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { Box(Modifier.weight(1f)) { card(it) } }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MallangCard(item: MallangItem, onPlant: (SpeciesId) -> Unit) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    val name = stringResource(R.string.mallang_name, stringResource(ContentRes.speciesName(item.species)))
    val action = stringResource(R.string.bag_plant)
    PlentyCard(
        Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clickable(role = Role.Button) { onPlant(item.species) }
            .semantics { contentDescription = "$name ${item.count}개, $action" },
    ) {
        PlantImage(
            art = PlantArt.of(item.species, GrowthStage.SEED),
            contentDescription = null,
            modifier = Modifier
                .padding(start = 22.dp, top = 20.dp)
                .size(width = 165.dp, height = 160.dp),
        )
        Column(Modifier.padding(start = 190.dp, top = 40.dp).clearAndSetSemantics {}) {
            Text(name, style = type.label.copy(fontWeight = FontWeight.SemiBold), color = colors.text)
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.bag_count, item.count),
                style = type.title.copy(fontSize = 35.sp, lineHeight = 44.sp),
                color = colors.primary,
            )
        }
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 28.dp, end = 32.dp, bottom = 26.dp)
                .clearAndSetSemantics {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = action,
                style = type.caption.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                color = colors.accent,
                modifier = Modifier.weight(1f),
            )
            Icon(painterResource(R.drawable.ic_arrow), null, tint = colors.accent, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun MaterialCard(item: MaterialItem, onOpen: (MaterialId) -> Unit) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    val name = stringResource(ContentRes.materialName(item.material))
    val description = item.traitLabel?.let { stringResource(R.string.material_cultivates, it) }
        ?: stringResource(R.string.material_no_trait)
    PlentyCard(
        Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clickable(role = Role.Button) { onOpen(item.material) }
            .clearAndSetSemantics { contentDescription = "$name ${item.count}개. $description" },
    ) {
        Box(
            Modifier
                .padding(start = 28.dp, top = 32.dp)
                .size(120.dp)
                .background(colors.surfaceTonal, RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(ContentRes.materialIcon(item.material)), null, tint = colors.accent, modifier = Modifier.size(54.dp))
        }
        Column(Modifier.padding(start = 180.dp, top = 44.dp)) {
            Text(name, style = type.body.copy(fontWeight = FontWeight.SemiBold), color = colors.text)
            Spacer(Modifier.height(16.dp))
            Text(
                text = item.count.toString(),
                style = type.title.copy(fontSize = 35.sp, lineHeight = 44.sp),
                color = colors.primary,
            )
        }
        Text(
            text = description,
            style = type.caption.copy(fontSize = 20.sp, lineHeight = 28.sp),
            color = colors.textSecondary,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, end = 28.dp, bottom = 26.dp),
        )
    }
}

/** S01 "빈 가방" for 말랑: cause in one line, how to recover in the next. */
@Composable
private fun EmptyMallangCard() {
    val colors = PlentyTheme.colors
    PlentyCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 32.dp, vertical = 28.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_seed), null, tint = colors.accent, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(20.dp))
            Column {
                Text(stringResource(R.string.bag_empty_title), style = PlentyTheme.typography.body.copy(fontWeight = FontWeight.SemiBold), color = colors.text)
                Text(stringResource(R.string.bag_empty_body), style = PlentyTheme.typography.caption, color = colors.textSecondary)
            }
        }
    }
}
