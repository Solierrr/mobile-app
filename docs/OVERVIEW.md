<p align="center">
  <img src="../assets/readme/banners/jetpack-compose.png" alt="Jetpack Compose" width="100%">
</p>

# Visão de produto

O aplicativo atende às pessoas que acompanham projetos e conversam com participantes pelo Android. O acesso ocorre no navegador e volta ao aplicativo depois da autorização. O servidor decide a elegibilidade da conta e o acesso aos recursos.

## Capacidades conectadas

- Consultar e criar projetos com nome, descrição e endereço opcional.
- Ver membros, atividades, histórico, localização e fotos de um projeto.
- Consultar o próprio perfil, outras pessoas e conexões.
- Adicionar ou remover conexão e iniciar conversa direta.
- Ler conversas humanas já sincronizadas sem conexão.
- Preparar mensagens sem rede e acompanhar se aguardam envio ou foram recusadas.
- Editar e excluir mensagens com rede, conforme a permissão do serviço.
- Marcar leitura sem rede e sincronizar depois.
- Arquivar e silenciar conversa com conexão.
- Ver presença e digitação quando o tempo real está ativo.

## Regras

Uma mensagem guardada para envio ainda não foi entregue, e uma mensagem recusada pode ser reenviada ou descartada. A fila pertence a quem a criou, e sair ou trocar de conta apaga os dados locais, inclusive as mensagens não enviadas.

A conversa que perdeu acesso permanece em leitura do conteúdo local e deixa de aceitar novos envios pelo aplicativo. O histórico de conversas com assistentes não fica guardado como histórico humano no banco do aparelho.

O nome do projeto tem entre 3 e 160 caracteres e a descrição tem até 4000, e quando informados o estado precisa ser uma unidade federativa aceita e o CEP precisa ter oito dígitos, sem dispensar as verificações do servidor.

## Telas e limites

A navegação reúne início, projetos, conversas, chamados, agenda, mapa, perfis, conexões e configurações. A existência da tela não comprova integração remota.

A agenda, os locais salvos, os chamados e as notificações usam conteúdo de exemplo, sem recebimento de push integrado, e a busca de endereço só tem destino configurado no QA. As áreas com conteúdo de exemplo ficam fora da versão distribuída.

Login, projetos, perfis e conversas já possuem integrações remotas. A descrição antiga que colocava todo o aplicativo sem APIs não representa esta versão.
