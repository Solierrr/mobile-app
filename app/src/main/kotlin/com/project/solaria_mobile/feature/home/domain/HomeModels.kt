package com.project.solaria_mobile.feature.home.domain


enum class CompanyLogoId { GOOGLE, SWIFT, AMAZON, PARTNER, SOLAR }

data class CompanyLogo(val name: String, val logoId: CompanyLogoId)
