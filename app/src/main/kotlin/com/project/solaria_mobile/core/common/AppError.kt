package com.project.solaria_mobile.core.common

/**
 * Categorias de possiveis falhas para chamadas backend
 *
 * ViewModels não precisam conhecer status code nem exceptions, apenas a categoria do erro
 */
enum class ErrorKind {
    /** Sem rede | falha de DNS | conexão recusada ou resetada | falha de TLS */
    CONNECTIVITY,

    /** Timeout de conexão | leitura ou escrita */
    TIMEOUT,

    /** 401 -> [AppError.code] diz o motivo (`INVALID_CREDENTIALS`, `REFRESH_TOKEN_REUSED`...) */
    UNAUTHORIZED,

    /** 403 -> api-core responde 403 para usuário sem vínculo dependendo da entidade solicitada */
    FORBIDDEN,

    /** 404 */
    NOT_FOUND,

    /** 409 -> Nem sempre é falha | conflito de dados */
    CONFLICT,

    /** 400 ou 422 -> validação | [AppError.fieldErrors] para ver as mensagens por campo */
    VALIDATION,

    /** 429 -> Kong | infra | segurança da aplicação */
    RATE_LIMITED,

    /** 5XX */
    SERVER,

    /** resposta com formato inválido dos DTOs */
    SERIALIZATION,

    /** qualquer outra coisa */
    UNKNOWN,
}

/**
 * mensagem de validação para campos
 * @property field nome do campo | `null` quando a mensagem não é de um campo específico
 * @property message texto da mensagem enviada pela API
 */
data class FieldError(
    val field: String?,
    val message: String,
)

/**
 * Falha de uma chamada a backend, já normalizada a partir do formato de erro que o serviço usou
 * (o `ApiErrorParser` documenta os formatos que entende).
 *
 *
 * @property kind categoria de falha para ramos de `when`
 * @property status status HTTP
 * @property code codigo de erro enviado pelas APIs
 * @property message mensagem de erro enviada pelas APIs
 * @property fieldErrors mensagens de validação por campo
 * @property cause exceção original
 */
data class AppError(
    val kind: ErrorKind,
    val status: Int? = null,
    val code: String? = null,
    val message: String? = null,
    val fieldErrors: List<FieldError> = emptyList(),
    val cause: Throwable? = null,
)
