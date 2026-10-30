package app.plenty.ui.journey

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.plenty.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val JourneyTime = DateTimeFormatter.ofPattern("M월 d일 HH:mm", Locale.KOREA)

/** "10월 28일 14:05", in the device time zone. */
fun formatJourneyTime(millis: Long): String =
    JourneyTime.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

/** "10월 28일 14:05 출발". */
@Composable
fun startedAtText(millis: Long): String = stringResource(R.string.journey_started_at, formatJourneyTime(millis))
