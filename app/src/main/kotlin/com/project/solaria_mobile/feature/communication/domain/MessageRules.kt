package com.project.solaria_mobile.feature.communication.domain

/**
 * Prazo para editar ou excluir uma mensagem depois do envio: 30 minutos.
 *
 * É o espelho da propriedade `app.messages.mutation-window` do api-messenger. Aqui serve só para esconder as ações de
 * mensagens antigas. Quem decide é o servidor: ele recusa com 422 uma mudança fora do prazo, e o aparelho com o
 * relógio errado apenas mostra ou esconde a ação na hora errada.
 */
const val MUTATION_WINDOW_MILLIS: Long = 30L * 60 * 1000

/**
 * Diz se o usuário pode editar ou excluir [message] agora.
 *
 * Vale quando todas as condições são verdadeiras:
 * - a mensagem é do usuário, já foi confirmada pelo servidor e não foi excluída;
 * - a conversa não é do chatbot e o usuário não perdeu o acesso;
 * - há internet, porque a mudança não entra na fila de envio;
 * - ainda não passaram 30 minutos do envio.
 *
 * @param message mensagem da tela.
 * @param conversation conversa em que ela está.
 * @param online `true` se há internet.
 * @param now instante atual, em milissegundos.
 */
fun canChange(message: ChatMessage, conversation: Conversation, online: Boolean, now: Long): Boolean {
    val sentAt = message.sentAt ?: return false
    return message.mine &&
        message.status == MessageStatus.SENT &&
        !message.deleted &&
        !conversation.chatbot &&
        !conversation.accessLost &&
        online &&
        now < sentAt + MUTATION_WINDOW_MILLIS
}
