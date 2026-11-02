package com.project.solaria_mobile.feature.profile

/**
 * Página pública da empresa no web: `<site>/pt-BR/empresa/<slug>`, ou a própria página da empresa (`/empresa`) quando
 * o catálogo não tem o identificador. Sem endereço do site configurado, não há botão.
 */
fun companyWebUrl(base: String, slug: String): String? {
    val site = base.trim().trimEnd('/')
    if (site.isEmpty() || !site.startsWith("https://")) return null
    val identifier = slug.trim()
    return if (identifier.isEmpty()) "$site/pt-BR/empresa" else "$site/pt-BR/empresa/${java.net.URLEncoder.encode(identifier, "UTF-8").replace("+", "%20")}"
}
