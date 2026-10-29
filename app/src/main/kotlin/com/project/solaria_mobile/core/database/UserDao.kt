package com.project.solaria_mobile.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * DAO dos nomes e avatares, sobre a tabela `users`.
 *
 * Principais operações:
 * - [observeAll] -> acompanha todas as pessoas guardadas.
 * - [save] -> insere a pessoa se não existe, atualiza se mudou e não faz nada se é igual.
 */
@Dao
abstract class UserDao {
    /** Emite todas as pessoas guardadas a cada mudança na tabela. */
    @Query("SELECT * FROM users")
    abstract fun observeAll(): Flow<List<UserEntity>>

    /** Busca a pessoa pelo `authId`, ou `null`. */
    @Query("SELECT * FROM users WHERE authId = :authId")
    abstract suspend fun find(authId: String): UserEntity?

    /** Grava a linha, trocando a que tiver o mesmo `authId`. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun put(user: UserEntity)

    /**
     * Guarda a pessoa só se ela é nova ou mudou, para não acordar quem observa a tabela sem necessidade.
     *
     * @param user dados recebidos do api-core.
     */
    @Transaction
    open suspend fun save(user: UserEntity) {
        if (find(user.authId) != user) put(user)
    }
}
