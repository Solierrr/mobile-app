package com.project.solaria_mobile.core.network

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Executa uma chamada Retrofit e devolve um [ApiResult] sucess | failure
 *
 * cada repository coloca sua chamada de API/código dentro dele e os try/catch acontecem de forma centralizada
 *
 * Depende de:
 * - [ApiErrorParser] -> traduz HTTP em categoria e campos de validação para a interface
 *
 * Principais operações:
 * - [execute] -> roda o bloco de código e classifica o resultado
 */
class ApiCallExecutor @Inject constructor(
    private val errorParser: ApiErrorParser,
) {

    /**
     * Roda [block] e converte o retorno em [ApiResult]
     *
     * @param block chamada de API / bloco de código
     * @return [ApiResult.Success] com o resultado de [block] | [ApiResult.Failure] para erros HTTP
     * @throws CancellationException se a corrotina for cancelada
     */
    suspend fun <T> execute(block: suspend () -> T): ApiResult<T> =
        try {
            ApiResult.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            val body = try {
                e.response()?.errorBody()?.string()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            errorParser.parse(e.code(), body)
        } catch (e: SerializationException) {
            ApiResult.Failure(ErrorKind.SERIALIZATION)
        } catch (e: InterruptedIOException) {
            ApiResult.Failure(ErrorKind.TIMEOUT)
        } catch (e: IOException) {
            ApiResult.Failure(ErrorKind.CONNECTIVITY)
        } catch (e: Exception) {
            ApiResult.Failure(ErrorKind.UNKNOWN)
        }
}
