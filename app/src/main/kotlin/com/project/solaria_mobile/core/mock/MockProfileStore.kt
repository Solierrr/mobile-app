package com.project.solaria_mobile.core.mock

import android.content.Context
import com.project.solaria_mobile.core.network.ApiJson
import com.project.solaria_mobile.feature.onboarding.data.TechnicianProfileDto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Onde o perfil profissional do modo mock fica entre uma abertura do app e outra. Sem isso o onboarding recomeçava a cada
 * reinício do processo, porque o mock guardava o perfil só na memória.
 */
interface MockProfileStore {
    fun load(): TechnicianProfileDto?

    /** `null` apaga o perfil guardado. */
    fun save(profile: TechnicianProfileDto?)

    /** Não guarda nada; usado nos testes. */
    data object None : MockProfileStore {
        override fun load(): TechnicianProfileDto? = null
        override fun save(profile: TechnicianProfileDto?) = Unit
    }
}

@Singleton
class FileMockProfileStore @Inject constructor(@ApplicationContext context: Context) : MockProfileStore {
    private val file = File(context.noBackupFilesDir, "mock-technician-profile.json")

    override fun load(): TechnicianProfileDto? = runCatching {
        if (file.exists()) ApiJson.instance.decodeFromString(TechnicianProfileDto.serializer(), file.readText()) else null
    }.getOrNull()

    override fun save(profile: TechnicianProfileDto?) {
        runCatching {
            if (profile == null) file.delete() else file.writeText(ApiJson.instance.encodeToString(TechnicianProfileDto.serializer(), profile))
        }
    }
}
