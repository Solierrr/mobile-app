package com.project.solaria_mobile.core.common

/**
 * resultado das chamadas de API
 *
 * Retornos: resultado esperado | [AppError]
 *
 * [AppError] remove a necessidade do view model ter try/catchs
 * em chamadas de api, porque tipa a exception
 *
 * Principais operações:
 *
 * - [map] -> transforma o valor de um sucesso
 * - [fold] -> junta as duas possibilidades em um único valor
 * - [onSuccess] | [onFailure] -> executam uma ação em um dos lados, permitindo encadear
 * - [getOrNull] -> o valor de um sucesso, ou `null`
 *
 * Objetivo da classe:
 *
 * - Diminuir/simplificar código para tratamento de execeção
 * - fazer encadeamento de funções para diferentes respostas de API
 *
 * Exemplos de uso:
 *
 * ```kotlin
 * repository.getUser()
 *     .onSuccess { user ->
 *         println("Usuário: ${user.name}")   encadeamento de função
 *     }
 *     .onFailure { error ->
 *         println("Erro: $error")    encadeamento de função
 *     }
 * ```
 *
 * --------------------------------------------------------------------
 *
 * ```kotlin
 * when (val result = repository.getUser()) {
 *     is ApiResult.Success -> {
 *         val name = result.data.name
 *         println(name)
 *     }
 *
 *     is ApiResult.Failure -> {
 *         println(result.error)
 *     }
 * }
 * ```
 *
 * Preferência de uso:
 *
 * usar when para lógica complexa
 *
 * Usar encadeamento para lógicas simples
 */
sealed interface ApiResult<out T> {
    /** A chamada deu certo e trouxe [data] */
    data class Success<out T>(val data: T) : ApiResult<T>

    /** A chamada falhou | [error] é a exception */
    data class Failure(val error: AppError) : ApiResult<Nothing>
}

/** Transforma o valor de [ApiResult.Success] | [ApiResult.Failure] passa sem alteração */
inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
}

/** Executa [action] com o valor quando é um sucesso e devolve `this`, para encadear */
inline fun <T> ApiResult<T>.onSuccess(action: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) action(data)
    return this
}

/** Executa [action] com o erro quando é uma falha e devolve `this`, para encadear */
inline fun <T> ApiResult<T>.onFailure(action: (AppError) -> Unit): ApiResult<T> {
    if (this is ApiResult.Failure) action(error)
    return this
}

/** junta as duas possibilidades em um único valor: [onSuccess] para sucesso | [onFailure] para falha */
inline fun <T, R> ApiResult<T>.fold(onSuccess: (T) -> R, onFailure: (AppError) -> R): R = when (this) {
    is ApiResult.Success -> onSuccess(data)
    is ApiResult.Failure -> onFailure(error)
}

/** O valor de um sucesso, ou `null` para uma falha */
fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.data
