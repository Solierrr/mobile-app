package com.project.solaria_mobile.feature.communication.data

/**
 * Quem está online entre os contatos da conta, montado com o retrato e os avisos de presença do messenger.
 *
 * Cada pessoa guarda a época e a versão do último dado aceito. Um dado vale se a época mudou ou se a versão é maior que a
 * daquela pessoa; assim um retrato lido antes de um aviso mais novo não desfaz o aviso, e os retratos parciais, enviados
 * quando uma conversa ganha gente, só somam ao que já se sabe. Os identificadores são guardados em minúsculas.
 *
 * Não é seguro entre threads: quem usa serializa as chamadas.
 */
internal class PresenceTracker {
    private class Entry(val online: Boolean, val epoch: String, val version: Long)

    private val entries = HashMap<String, Entry>()

    /** Soma o retrato ao que já se sabe, pessoa por pessoa. */
    fun snapshot(epoch: String, version: Long, users: Map<String, Boolean>) {
        users.forEach { (user, online) -> accept(user, online, epoch, version) }
    }

    /** Aplica o aviso de uma pessoa que ficou online ou offline. */
    fun change(user: String, online: Boolean, epoch: String, version: Long) = accept(user, online, epoch, version)

    /** Identificadores, em minúsculas, de quem está online agora. */
    fun online(): Set<String> = entries.filterValues { it.online }.keys.toSet()

    fun clear() = entries.clear()

    private fun accept(user: String, online: Boolean, epoch: String, version: Long) {
        val id = user.lowercase()
        val known = entries[id]
        if (known == null || known.epoch != epoch || version > known.version) entries[id] = Entry(online, epoch, version)
    }
}
