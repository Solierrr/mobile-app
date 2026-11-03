package com.project.solaria_mobile.feature.communication

import android.content.Context
import com.project.solaria_mobile.R
import com.project.solaria_mobile.feature.communication.domain.RejectedParticipant

/** Agrupa os motivos sem expor códigos técnicos nem identificadores pessoais na interface. */
internal fun List<RejectedParticipant>.description(context: Context): String =
    groupingBy { it.reasonCode }.eachCount().entries.joinToString("\n") { (reason, count) ->
        val label = when (reason) {
            "USER_NOT_FOUND" -> R.string.gp_recusado_nao_encontrado
            "USER_INACTIVE" -> R.string.gp_recusado_inativo
            "NOT_COMMUNITY_MEMBER" -> R.string.gp_recusado_comunidade
            else -> R.string.gp_recusado_outro
        }
        "${context.getString(label)}: $count"
    }
