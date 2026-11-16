package com.project.solaria_mobile.feature.project

import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.designsystem.components.userMessage

/**
 * Texto da recusa de uma operação do projeto. A falha não traz a mensagem do servidor, então as recusas conhecidas do
 * api-core (encerrar com pendências, formato ou limite de imagens, permissão) ganham uma explicação própria.
 */
internal fun ProjectAction.Failed.message(): String = when (kind) {
    ErrorKind.CONNECTIVITY, ErrorKind.TIMEOUT -> when (operation) {
        ProjectOperation.STATUS -> "Sem conexão. A situação não foi alterada."
        ProjectOperation.ARCHIVE -> "Sem conexão. O projeto não foi arquivado."
        ProjectOperation.ACTIVITY_STATUS -> "Sem conexão. A atividade não foi alterada."
        ProjectOperation.INVITE, ProjectOperation.REVOKE_INVITE, ProjectOperation.RESEND_INVITE, ProjectOperation.RESPOND_INVITE -> "Sem conexão. O convite não foi alterado."
        ProjectOperation.REMOVE_MEMBER -> "Sem conexão. A pessoa continua no projeto."
        ProjectOperation.SAVE_ACTIVITY -> "Sem conexão. A atividade não foi salva."
        else -> "Sem conexão. As imagens não foram alteradas."
    }
    ErrorKind.FORBIDDEN -> when (operation) {
        ProjectOperation.STATUS, ProjectOperation.ARCHIVE, ProjectOperation.SET_COVER, ProjectOperation.INVITE,
        ProjectOperation.REVOKE_INVITE, ProjectOperation.RESEND_INVITE, ProjectOperation.REMOVE_MEMBER, ProjectOperation.SAVE_ACTIVITY,
        -> "Só quem gerencia o projeto pode fazer isso."
        ProjectOperation.ACTIVITY_STATUS -> "Você não pode alterar esta atividade."
        ProjectOperation.RESPOND_INVITE -> "Este convite não está mais disponível."
        else -> "Você não tem permissão para alterar as imagens deste projeto."
    }
    ErrorKind.VALIDATION, ErrorKind.CONFLICT -> when (operation) {
        ProjectOperation.STATUS ->
            if (status?.closes == true) "Conclua ou cancele as atividades e serviços pendentes antes de encerrar o projeto."
            else "O projeto não aceita essa situação agora."
        ProjectOperation.ARCHIVE -> "O projeto não pode ser arquivado agora."
        ProjectOperation.ACTIVITY_STATUS -> "A atividade não aceita essa situação agora."
        ProjectOperation.INVITE -> "Confira o e-mail. A pessoa pode já participar do projeto ou ter um convite pendente."
        ProjectOperation.RESPOND_INVITE -> "O convite expirou ou já foi respondido."
        ProjectOperation.REMOVE_MEMBER -> "Esta pessoa não pode ser removida do projeto."
        ProjectOperation.REVOKE_INVITE, ProjectOperation.RESEND_INVITE -> "Este convite não pode ser alterado agora."
        ProjectOperation.SAVE_ACTIVITY ->
            "Confira o título e os horários: o término precisa ser depois do início. Observadores não podem ser responsáveis, e só atividades planejadas podem ser editadas."
        ProjectOperation.UPLOAD_PHOTO -> "Use uma imagem JPG ou PNG de até 8 MB. Cada projeto aceita até 30 imagens."
        else -> "A imagem não pôde ser alterada agora."
    }
    else -> kind.userMessage()
}
