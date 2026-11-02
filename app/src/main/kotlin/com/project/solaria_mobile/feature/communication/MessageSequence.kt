package com.project.solaria_mobile.feature.communication

/**
 * Verdadeiro se uma mensagem que chegou ao vivo deixa um buraco na sequência da conversa: o servidor numera as mensagens de 1 em 1,
 * então [incoming] maior que o seguinte de [lastKnown] indica uma que ficou para trás. Sem mensagem conhecida (ou sem sequência, como
 * nas mensagens locais) não há o que comparar.
 */
internal fun hasSequenceGap(lastKnown: Int, incoming: Int): Boolean = lastKnown > 0 && incoming > lastKnown + 1
