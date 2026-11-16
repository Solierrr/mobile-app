package com.project.solaria_mobile.feature.project.domain

data class ProjectSummary(
    val id: String,
    val name: String,
    val place: String,
    val status: String,
    val people: String,
    val companyId: String? = null,
    val companyName: String = "",
    val companyLogoUrl: String? = null,
    val createdAtMillis: Long = 0,
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val geocodedAt: String? = null,
)

fun ProjectSummary.hasCoordinates(): Boolean {
    if (latitude == null || longitude == null || !latitude.isFinite() || !longitude.isFinite()
        || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return false
    return geocodedAt == null || runCatching {
        java.time.OffsetDateTime.parse(geocodedAt).isAfter(java.time.OffsetDateTime.now().minusDays(29))
    }.getOrDefault(false)
}

/** Situações aceitas pelo api-core; `label` é o texto mostrado no app. */
enum class ProjectStatusOption(val api: String, val label: String) {
    OPEN("OPEN", "Aberto"),
    IN_PROGRESS("IN_PROGRESS", "Em andamento"),
    COMPLETED("COMPLETED", "Concluído"),
    CANCELED("CANCELED", "Cancelado"),
    ;

    /** Concluir ou cancelar exige que atividades e serviços do projeto estejam encerrados. */
    val closes: Boolean get() = this == COMPLETED || this == CANCELED

    companion object {
        fun fromLabel(label: String): ProjectStatusOption? = entries.firstOrNull { it.label == label }
    }
}
