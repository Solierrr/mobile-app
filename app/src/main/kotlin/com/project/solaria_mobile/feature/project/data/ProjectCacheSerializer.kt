package com.project.solaria_mobile.feature.project.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object ProjectCacheSerializer : KSerializer<ProjectDto> {
    private val delegate = ProjectDto.serializer()
    override val descriptor = delegate.descriptor
    override fun deserialize(decoder: Decoder): ProjectDto = delegate.deserialize(decoder)
    override fun serialize(encoder: Encoder, value: ProjectDto) {
        val location = value.location
        val cached = if (location?.geocodedAt == null) value else value.copy(
            location = location.copy(latitude = null, longitude = null, geocodedAt = null))
        delegate.serialize(encoder, cached)
    }
}
