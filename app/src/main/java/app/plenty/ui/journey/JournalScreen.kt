package app.plenty.ui.journey

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.plenty.R
import app.plenty.core.content.GameContent
import app.plenty.data.JourneyLogEntry
import app.plenty.data.JourneyResultState
import app.plenty.ui.components.GroveButton
import app.plenty.ui.components.GroveButtonStyle
import app.plenty.ui.components.PlentyCard
import app.plenty.ui.formatKm
import app.plenty.ui.theme.PlentyTheme

/** 여정 탭: the ready state (if any) and past journeys, newest first. A row reopens its 08. */
@Composable
fun JournalScreen(
    entries: List<JourneyLogEntry>,
    readyCount: Int,
    content: GameContent,
    onOpen: (journeyId: Long) -> Unit,
    onPrepare: () -> Unit,
    onOpenReady: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    Column(modifier.fillMaxSize().padding(start = 4.dp, top = 36.dp)) {
        Text(stringResource(R.string.journal_title), style = type.title, color = colors.text)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.journal_subtitle), style = type.body.copy(fontSize = 22.sp), color = colors.textSecondary)
        Spacer(Modifier.height(32.dp))
        if (readyCount > 0) {
            PlentyCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 36.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = colors.accent, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(20.dp))
                    Text(stringResource(R.string.journal_ready, readyCount), style = type.heading, color = colors.primary, modifier = Modifier.weight(1f))
                    GroveButton(stringResource(R.string.journal_ready_open), onOpenReady, Modifier.width(260.dp), style = GroveButtonStyle.Secondary)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        if (entries.isEmpty()) {
            PlentyCard(Modifier.fillMaxWidth().weight(1f).padding(bottom = 24.dp)) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(painterResource(R.drawable.ic_route), contentDescription = null, tint = colors.accent, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(20.dp))
                    Text(stringResource(R.string.journal_empty_title), style = type.heading, color = colors.primary)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.journal_empty_body), style = type.body, color = colors.textSecondary)
                    if (readyCount == 0) {
                        Spacer(Modifier.height(32.dp))
                        GroveButton(stringResource(R.string.garden_prepare_journey), onPrepare, Modifier.width(300.dp))
                    }
                }
            }
            return@Column
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(entries, key = { it.journeyId }) { entry -> JournalRow(entry, content, onOpen) }
        }
    }
}

@Composable
private fun JournalRow(entry: JourneyLogEntry, content: GameContent, onOpen: (Long) -> Unit) {
    val colors = PlentyTheme.colors
    val type = PlentyTheme.typography
    val region = entry.arrivalRegion?.let(content::region)?.name
    val outcome = when (entry.state) {
        JourneyResultState.Settled -> stringResource(R.string.journal_distance, formatKm(entry.distanceKm ?: 0.0))
        JourneyResultState.Rejected -> stringResource(R.string.journal_rejected)
        JourneyResultState.CheckingDistance, JourneyResultState.Underway -> stringResource(R.string.journal_checking)
    }
    val place = region ?: stringResource(R.string.result_region_unknown).takeIf { entry.state == JourneyResultState.Settled }
    PlentyCard(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button) { onOpen(entry.journeyId) }
            .semantics(mergeDescendants = true) {},
    ) {
        Row(Modifier.padding(horizontal = 36.dp, vertical = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).background(colors.surfaceTonal, CircleShape), contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.ic_route), contentDescription = null, tint = colors.primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(24.dp))
            Column(Modifier.weight(1f)) {
                Text(formatJourneyTime(entry.startedAt), style = type.label.copy(fontWeight = FontWeight.SemiBold), color = colors.primary)
                Spacer(Modifier.height(6.dp))
                Text(
                    listOfNotNull(place, stringResource(R.string.journal_companions, entry.companionCount)).joinToString(" · "),
                    style = type.caption.copy(fontSize = 20.sp),
                    color = colors.textSecondary,
                )
            }
            Text(outcome, style = type.heading, color = colors.accent)
            Spacer(Modifier.width(20.dp))
            Icon(painterResource(R.drawable.ic_arrow), contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(26.dp))
        }
    }
}
