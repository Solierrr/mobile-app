package com.project.solaria_mobile.feature.communication.data

import com.project.solaria_mobile.core.database.MessageStats

/**
 * Próximo pedido de histórico que falta para completar uma conversa no aparelho.
 *
 * - [Done] -> a conversa está completa, da `sequence` 1 até a última.
 * - [Latest] -> a conversa não tem mensagens guardadas. Busca as mais recentes.
 * - [Since] -> busca o que vem depois de [Since.sequence]. Preenche um buraco ou traz o que chegou depois.
 * - [Before] -> busca o que vem antes de [Before.sequence]. Baixa o histórico para trás até a `sequence` 1.
 */
internal sealed interface HistoryStep {
    data object Done : HistoryStep
    data object Latest : HistoryStep
    data class Since(val sequence: Int) : HistoryStep
    data class Before(val sequence: Int) : HistoryStep
}

/**
 * Decide o próximo pedido de histórico de uma conversa, só com o que está guardado e com o que o servidor informou.
 *
 * A ordem das regras importa: primeiro o buraco no meio, depois o que falta no fim e por último o que falta no começo.
 * Assim a conversa fica correta nas pontas antes de o histórico antigo terminar de descer.
 *
 * @param stats resumo das mensagens guardadas da conversa.
 * @param holeAfter `sequence` que antecede o primeiro buraco, ou `null` se não há buraco.
 * @param headSequence `sequence` da última mensagem segundo a lista de conversas. `0` se a conversa não tem mensagens.
 * @return o próximo pedido, ou [HistoryStep.Done] se não falta nada.
 */
internal fun nextHistoryStep(stats: MessageStats, holeAfter: Int?, headSequence: Int): HistoryStep {
    val min = stats.minSequence
    val max = stats.maxSequence
    return when {
        min == null || max == null -> if (headSequence > 0) HistoryStep.Latest else HistoryStep.Done
        holeAfter != null -> HistoryStep.Since(holeAfter)
        headSequence > max -> HistoryStep.Since(max)
        min > 1 -> HistoryStep.Before(min)
        else -> HistoryStep.Done
    }
}
