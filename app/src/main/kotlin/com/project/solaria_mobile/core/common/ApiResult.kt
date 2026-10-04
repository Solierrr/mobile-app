package com.project.solaria_mobile.core.common

/**
 * resultado das chamadas de API
 *
 * Retornos: resultado esperado | [ApiResult.Failure]
 *
 * [ApiResult.Failure] informa a categoria do erro e os campos inválidos
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
 *         showUser(user) // encadeamento de função
 *     }
 *     .onFailure { failure ->
 *         showError(failure.kind) // encadeamento de função
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
 *         println(result.kind)
 *         println(result.invalidFields)
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

    /**
     * A chamada falhou; [kind] informa a categoria e [invalidFields] lista os campos
     * rejeitados, quando disponíveis.
     */
    data class Failure(
        val kind: ErrorKind,
        val invalidFields: Set<String> = emptySet(),
    ) : ApiResult<Nothing>
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
inline fun <T> ApiResult<T>.onFailure(action: (ApiResult.Failure) -> Unit): ApiResult<T> {
    if (this is ApiResult.Failure) action(this)
    return this
}

/** junta as duas possibilidades em um único valor: [onSuccess] para sucesso | [onFailure] para falha */
inline fun <T, R> ApiResult<T>.fold(onSuccess: (T) -> R, onFailure: (ApiResult.Failure) -> R): R = when (this) {
    is ApiResult.Success -> onSuccess(data)
    is ApiResult.Failure -> onFailure(this)
}

/** O valor de um sucesso, ou `null` para uma falha */
fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.data
