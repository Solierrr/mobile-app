package com.project.solaria_mobile.feature.profile.domain

import com.project.solaria_mobile.feature.project.domain.ProjectSummary

data class CompanyProfile(
    val id: String,
    val name: String,
    val logoUrl: String?,
    val projects: List<ProjectSummary>,
    val detailsAvailable: Boolean = true,
    val place: String = "",
    val bannerUrl: String? = null,
    /** Identificador público da empresa no web; vazio quando o catálogo não a lista. */
    val slug: String = "",
)

fun companyFromProjects(id: String, projects: List<ProjectSummary>): CompanyProfile? {
    val linked = projects.filter { it.companyId == id }
    val company = linked.firstOrNull { it.companyName.isNotBlank() } ?: return null
    return CompanyProfile(id, company.companyName, linked.firstNotNullOfOrNull { it.companyLogoUrl }, linked.sortedByDescending { it.createdAtMillis })
}

/** Empresa no catálogo público do api-core: identidade comercial sem dados privados. */
data class CatalogCompany(val id: String, val name: String, val place: String, val slug: String = "")

/** Logo e capa da empresa, já como endereços de imagem. */
data class CompanyPhotos(val logoUrl: String?, val bannerUrl: String?)

/**
 * Identidade vem do catálogo; projetos vinculados são uma seção à parte. Empresa sem projeto continua com perfil, e só
 * fica indisponível quando não está no catálogo nem nos projetos da conta.
 */
fun companyProfile(id: String, catalog: List<CatalogCompany>?, projects: List<ProjectSummary>): CompanyProfile? {
    val listed = catalog?.firstOrNull { it.id == id }
    val fromProjects = companyFromProjects(id, projects)
    if (listed == null && fromProjects == null) return null
    return CompanyProfile(
        id = id,
        name = listed?.name ?: fromProjects!!.name,
        logoUrl = fromProjects?.logoUrl,
        projects = fromProjects?.projects.orEmpty(),
        place = listed?.place.orEmpty(),
        slug = listed?.slug.orEmpty(),
    )
}
