# Acesso e instalação de QA

QA consome os serviços do Render. Produção consome os serviços do GCP e mantém PKCE/deeplink. A interface e o nome do app são iguais; os ícones de launcher são diferentes (QA azul).

O botão Entrar do QA chama `POST /auth/mobile/qa/session`. A conta seed é escolhida exclusivamente pela configuração do servidor, que verifica o profissional aprovado e emite a sessão mobile normal. O APK não contém conta, senha ou credencial de provisionamento.

## Preparar um aparelho

Instale o APK `qaDebug`, autorize ADB no aparelho e provisione uma credencial autorizada no servidor:

```powershell
$credential = Read-Host "Credencial de aparelho QA" -AsSecureString
./scripts/provision-qa-device.ps1 -DeviceToken $credential -Serial <serial> -Adb <caminho-do-adb>
```

A credencial deve existir em `QA_MOBILE_DEVICE_TOKENS` no `api-auth` QA. Ela é entregue por stdin do ADB, lida uma vez e cifrada com Android Keystore em `noBackupFilesDir`; não é enviada em argumento do processo nem incluída em backup. O comando `run-as` exige uma build debuggable e acesso ADB autorizado. Sem provisionamento, a entrada QA é recusada. Apagar os dados do app exige preparar o aparelho novamente.

Revogue um aparelho removendo sua credencial da configuração do servidor e revogue suas sessões já emitidas. Produção não compila as classes de acesso/provisionamento de QA.

## Sessão

Os tokens de sessão são cifrados e persistidos nos dois flavors. Falhas de rede, timeout e respostas temporárias na renovação preservam a sessão. Rejeição definitiva do refresh token encerra a sessão local. O logout impede que uma renovação concorrente recrie a sessão encerrada.
