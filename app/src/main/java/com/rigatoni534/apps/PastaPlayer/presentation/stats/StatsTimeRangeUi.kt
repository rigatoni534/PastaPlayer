package rigatoni534.apps.PastaPlayer.presentation.stats

import androidx.annotation.StringRes
import rigatoni534.apps.PastaPlayer.R
import rigatoni534.apps.PastaPlayer.data.stats.StatsTimeRange

@StringRes
fun StatsTimeRange.displayNameRes(): Int = when (this) {
    StatsTimeRange.DAY -> R.string.stats_range_today
    StatsTimeRange.WEEK -> R.string.stats_range_week_to_date
    StatsTimeRange.MONTH -> R.string.stats_range_month_to_date
    StatsTimeRange.YEAR -> R.string.stats_range_year_to_date
    StatsTimeRange.ALL -> R.string.stats_range_all_time
}
