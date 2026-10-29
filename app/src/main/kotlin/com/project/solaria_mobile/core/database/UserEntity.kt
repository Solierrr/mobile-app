package com.project.solaria_mobile.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Linha da tabela `users`: nome e avatar de uma pessoa, guardados para o chat funcionar sem rede.
 *
 * Preenchida com `GET /api/users/auth/{authId}/summary` do api-core. A conversa direta usa o nome do outro
 * participante, e as bolhas de grupo usam o nome do remetente.
 *
 * @property authId id da conta (`sub` do JWT) em minúsculas. Chave primária.
 * @property username nome de exibição.
 * @property avatar endereço da imagem do avatar, ou `null` se a pessoa não tem.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val authId: String,
    val username: String,
    val avatar: String?,
)
