package app.plenty.ui.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plenty.R
import app.plenty.core.content.GameContent
import app.plenty.data.PottedPlant
import app.plenty.ui.art.PlantArt
import app.plenty.ui.art.PlantImage
import app.plenty.ui.components.DetailHeader
import app.plenty.ui.components.GroveButton
import app.plenty.ui.components.GroveButtonStyle
import app.plenty.ui.components.PlentyCard
import app.plenty.ui.plantDisplayName
import app.plenty.ui.theme.PlentyTheme

/**
 * 06 출발 대기: the ready companions and what happens next. No journey exists yet; it starts with
 * the start odometer when the vehicle leaves park, and companions can change until then.
 */
@Composable
fun JourneyReadyScreen(
    companions: List<PottedPlant>,
    backTo: String,
    content: GameContent,
    onBack: () -> Unit,
    onChange: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    Column(modifier.fillMaxSize().padding(start = 4.dp, top = 34.dp, bottom = 24.dp)) {
        DetailHeader(
            title = stringResource(R.string.ready_title),
            subtitle = stringResource(R.string.ready_subtitle),
            backTo = backTo,
            onBack = onBack,
        )
        Spacer(Modifier.height(32.dp))
        Row(Modifier.weight(1f, fill = false).heightIn(max = 640.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            PlentyCard(Modifier.weight(688f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize().padding(start = 40.dp, end = 40.dp, top = 40.dp, bottom = 40.dp)) {
                    if (companions.isEmpty()) {
                        Text(stringResource(R.string.ready_none), style = type.heading, color = colors.primary)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        companions.forEach { plant ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PlantImage(
                                    PlantArt.of(plant.state.species, plant.state.stage),
                                    contentDescription = null,
                                    modifier = Modifier.size(if (companions.size > 2) 120.dp else 150.dp),
                                )
                                Spacer(Modifier.width(28.dp))
                                Column {
                                    Text(plantDisplayName(plant.state, content), style = type.heading, color = colors.primary)
                                    Spacer(Modifier.height(10.dp))
                                    Text(stringResource(R.string.ready_role), style = type.label.copy(fontWeight = FontWeight.Normal), color = colors.textSecondary)
                                }
                            }
                        }
                    }
                    Text(stringResource(R.string.ready_change_note), style = type.label.copy(fontWeight = FontWeight.Normal), color = colors.textSecondary)
                }
            }
            PlentyCard(Modifier.weight(508f).fillMaxHeight()) {
                Column(Modifier.fillMaxSize().padding(40.dp)) {
                    Icon(painterResource(R.drawable.ic_route), contentDescription = null, tint = colors.accent, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(36.dp))
                    Text(stringResource(R.string.ready_when_title), style = type.heading.copy(fontSize = 32.sp), color = colors.primary)
                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(R.string.ready_when_body), style = type.body.copy(lineHeight = 40.sp), color = colors.textSecondary)
                    Spacer(Modifier.height(40.dp))
                    if (companions.isNotEmpty()) {
                        Row(
                            Modifier.background(colors.surfaceTonal, CircleShape).padding(horizontal = 22.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = colors.primary, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                stringResource(R.string.ready_count, companions.size),
                                style = type.caption.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium),
                                color = colors.primary,
                            )
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    GroveButton(
                        text = stringResource(if (companions.isEmpty()) R.string.ready_pick else R.string.ready_change),
                        onClick = onChange,
                        modifier = Modifier.fillMaxWidth(),
                        style = if (companions.isEmpty()) GroveButtonStyle.Primary else GroveButtonStyle.Secondary,
                        trailingIcon = if (companions.isEmpty()) R.drawable.ic_arrow else R.drawable.ic_back,
                    )
                }
            }
        }
    }
}
