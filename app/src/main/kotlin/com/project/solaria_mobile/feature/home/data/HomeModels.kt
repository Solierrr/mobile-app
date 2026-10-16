package com.project.solaria_mobile.feature.home.data


enum class CompanyLogoId { GOOGLE, SWIFT, AMAZON, PARTNER, SOLAR }

data class CompanyLogo(val name: String, val logoId: CompanyLogoId)
