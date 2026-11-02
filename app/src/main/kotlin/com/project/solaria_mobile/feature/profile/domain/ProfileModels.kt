package com.project.solaria_mobile.feature.profile.domain

data class UserProfile(
    val id: String,
    val initials: String,
    val name: String,
    val handle: String,
    val state: String,
    val occupation: String,
    val phone: String,
    val affiliation: String = "",
    val email: String = "",
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val companyId: String? = null,
    val connected: Boolean = false,
    /** Id do perfil técnico próprio; só existe para profissionais. */
    val technicianId: String? = null,
    /** Profissional autônomo: é quem define jornada de trabalho e integra o Google Agenda. */
    val independent: Boolean = false,
)

/** Números do perfil próprio; ficam nulos quando a consulta falha, e a linha some em vez de mostrar zero falso. */
data class ProfileStats(val projects: Int?, val activeProjects: Int?, val connections: Int?)

/** [authId] é o identificador de login da pessoa, que o messenger usa para abrir uma conversa com ela. */
data class Connection(val id: String, val initials: String, val name: String, val handle: String, val avatarUrl: String? = null, val authId: String? = null)

data class PeoplePage(val items: List<Connection>, val hasMore: Boolean = false)

/** Imagens do perfil aceitas pelo api-core. */
enum class ProfilePhotoType(val api: String) { PROFILE("PROFILE"), BANNER("BANNER") }

/** Mesma regra do api-core: 3 a 30 caracteres, só letras minúsculas, números e sublinhado. */
val USERNAME_PATTERN = Regex("^[a-z0-9_]{3,30}$")
