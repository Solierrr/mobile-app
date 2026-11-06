package com.project.solaria_mobile.feature.project

import androidx.annotation.DrawableRes
import com.project.solaria_mobile.core.designsystem.icons.AppIcons
import com.project.solaria_mobile.feature.project.domain.ActivityTypeOption

/** Ícone de cada tipo de atividade, usado na agenda e no formulário da atividade. */
@get:DrawableRes
val ActivityTypeOption.icon: Int
    get() = when (this) {
        ActivityTypeOption.SERVICE -> AppIcons.wrench
        ActivityTypeOption.VISIT -> AppIcons.search
        ActivityTypeOption.MEETING -> AppIcons.users
        ActivityTypeOption.OTHER -> AppIcons.calendar_days
    }

/** Tipo vindo do servidor; valor ausente ou desconhecido cai em "outra". */
fun activityTypeOf(api: String?): ActivityTypeOption = ActivityTypeOption.entries.firstOrNull { it.api == api } ?: ActivityTypeOption.OTHER
