package com.project.solaria_mobile.presentation.navigation

import androidx.lifecycle.SavedStateHandle

private const val ID_ARGUMENT = "id"

fun SavedStateHandle.routeId(): String = checkNotNull(get<String>(ID_ARGUMENT)) { "Missing route argument $ID_ARGUMENT" }

fun SavedStateHandle.routeIdOrNull(): String? = get<String>(ID_ARGUMENT)
