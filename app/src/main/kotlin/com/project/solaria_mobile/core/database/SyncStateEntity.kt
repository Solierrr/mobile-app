package com.project.solaria_mobile.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Linha única da tabela `sync_state`: onde o catch-up de mensagens parou.
 *
 * Sem a linha, o aparelho ainda não fez a primeira sincronização. O [cursor] só anda pelo catch-up
 * (`GET /messaging/updates`), nunca por evento ao vivo, para que um evento perdido volte no próximo catch-up.
 *
 * @property id sempre [SINGLE_ROW]. A tabela tem uma linha.
 * @property syncEpoch época do relógio de sincronização do servidor. Se mudar, o aparelho refaz tudo.
 * @property cursor maior `changeVersion` que o catch-up já aplicou.
 */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val id: Int = SINGLE_ROW,
    val syncEpoch: String,
    val cursor: Long,
)

/** Valor fixo de [SyncStateEntity.id]. */
const val SINGLE_ROW = 0
