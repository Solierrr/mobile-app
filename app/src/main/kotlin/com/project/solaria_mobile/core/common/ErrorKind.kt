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

    /** 401 -> autenticação recusada */
    UNAUTHORIZED,

    /** 403 -> api-core responde 403 para usuário sem vínculo dependendo da entidade solicitada */
    FORBIDDEN,

    /** 404 */
    NOT_FOUND,

    /** 409 -> Nem sempre é falha | conflito de dados */
    CONFLICT,

    /** 400 ou 422 -> validação | [ApiResult.Failure.invalidFields] para localizar os campos */
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
