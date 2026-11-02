package com.project.solaria_mobile.core.designsystem.components

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.project.solaria_mobile.R
import com.project.solaria_mobile.core.common.ApiResult

/** Mensagem de um `errorCode` do messenger que o status HTTP sozinho não explica; nulo quando não há texto próprio. */
@StringRes
internal fun errorCodeMessage(code: String?): Int? = when {
    code == null -> null
    code == "SUCCESSOR_REQUIRED" -> R.string.error_code_successor_required
    code == "SUCCESSOR_INELIGIBLE" -> R.string.error_code_successor_ineligible
    code == "GROUP_MANAGER_UNAVAILABLE" -> R.string.error_code_manager_unavailable
    code == "CONCURRENT_MODIFICATION" -> R.string.error_code_concurrent
    code == "UNAUTHORIZED_ACCESS" || code == "ACCESS_DENIED" -> R.string.error_code_not_allowed
    code == "BUSINESS_RULE_VIOLATION" -> R.string.error_code_business_rule
    code == "MEDIA_EXPIRED" -> R.string.error_code_media_expired
    code == "MEDIA_MISMATCH" || code == "MEDIA_NOT_UPLOADED" -> R.string.error_code_media_invalid
    code == "DEPENDENCY_UNAVAILABLE" -> R.string.error_code_dependency
    code == "REMOVAL_INCOMPLETE" -> R.string.error_code_removal_incomplete
    code == "DUPLICATE_RESOURCE" -> R.string.error_code_duplicate
    // O servidor não sabe se a escrita valeu: quem repete sem conferir pode duplicar.
    code.endsWith("_UNCERTAIN") -> R.string.error_code_uncertain
    else -> null
}

/** O texto do `errorCode` quando existe, ou o da categoria do erro. */
fun ApiResult.Failure.userMessage(resources: Resources): String =
    errorCodeMessage(errorCode)?.let(resources::getString) ?: kind.userMessage(resources)

@Composable
fun ApiResult.Failure.userMessage(): String = userMessage(LocalContext.current.resources)
