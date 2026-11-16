package com.project.solaria_mobile.core.auth

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.feature.auth.domain.LoginDestination
import javax.inject.Inject

class FlavorLogin @Inject constructor() {
    suspend fun beginLogin(): ApiResult<LoginDestination>? = null
}
