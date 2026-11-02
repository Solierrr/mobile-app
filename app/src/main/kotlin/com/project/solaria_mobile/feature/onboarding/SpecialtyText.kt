package com.project.solaria_mobile.feature.onboarding

import androidx.annotation.StringRes
import com.project.solaria_mobile.R
import com.project.solaria_mobile.feature.onboarding.domain.Specialty

/** O nome da especialidade no idioma do aplicativo; o valor enviado ao servidor continua sendo [Specialty.api]. */
@StringRes
internal fun Specialty.labelRes(): Int = when (this) {
    Specialty.INSTALLATION -> R.string.specialty_installation
    Specialty.MAINTENANCE -> R.string.specialty_maintenance
    Specialty.CLEANING -> R.string.specialty_cleaning
    Specialty.INSPECTION -> R.string.specialty_inspection
    Specialty.PROJECT -> R.string.specialty_project
    Specialty.OTHER -> R.string.specialty_other
}
