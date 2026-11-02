package com.project.solaria_mobile.feature.discovery

import com.project.solaria_mobile.R
import androidx.annotation.StringRes
import androidx.annotation.DrawableRes
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.feature.discovery.domain.WorkJourney
import java.time.DayOfWeek

/** Padrões de dias para preencher a jornada de uma vez; o horário continua ajustável dia a dia. */
internal enum class JourneyPreset(@StringRes val label: Int, @DrawableRes val icon: Int, val days: Set<DayOfWeek>) {
    WEEKDAYS(R.string.preset_weekdays, AppIcons.briefcase, WorkJourney.Weekdays.toSet()),
    EVERY_DAY(R.string.preset_every_day, AppIcons.calendar_days, DayOfWeek.entries.toSet()),
    SUNDAY_OFF(R.string.preset_sunday_off, AppIcons.sun, DayOfWeek.entries.toSet() - DayOfWeek.SUNDAY),
}

/** O padrão que a jornada atual segue exatamente, ou `null` se ela é personalizada. */
internal fun JourneyDraft.preset(): JourneyPreset? {
    val on = filterValues { it != null }.keys
    return JourneyPreset.entries.firstOrNull { it.days == on }
}

/** Liga os dias do [preset] e desliga os demais, com o primeiro horário já definido; sem nenhum, ficam sem horário. */
internal fun JourneyDraft.withPreset(preset: JourneyPreset): JourneyDraft {
    val reference = referenceHours()
    return DayOfWeek.entries.associateWith { day -> DaySlot(reference).takeIf { day in preset.days } }
}
