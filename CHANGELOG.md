# Changelog

## [0.3.0](https://github.com/Solierrr/mobile-app/compare/v0.2.0...v0.3.0) (2026-10-29)


### Features

* add common mobile primitives ([#18](https://github.com/Solierrr/mobile-app/issues/18)) ([ad6b500](https://github.com/Solierrr/mobile-app/commit/ad6b500657f956254306e9ac217097f6889027ec))
* add multipart upload request bodies ([#22](https://github.com/Solierrr/mobile-app/issues/22)) ([072a7f1](https://github.com/Solierrr/mobile-app/commit/072a7f17b26af07c9c639a5be197188f43b71949))
* add network connectivity monitor ([#23](https://github.com/Solierrr/mobile-app/issues/23)) ([3a965c8](https://github.com/Solierrr/mobile-app/commit/3a965c8698cbcd91ed927b164655f8a5453e4855))
* add realtime STOMP client ([#34](https://github.com/Solierrr/mobile-app/issues/34)) ([2366079](https://github.com/Solierrr/mobile-app/commit/2366079738c101c2ea5aabe2aaab6b8f6f0d3e3b))
* add REST credential transport ([#24](https://github.com/Solierrr/mobile-app/issues/24)) ([6a1c43a](https://github.com/Solierrr/mobile-app/commit/6a1c43a236cb1a2df98df0e4c52de22f909fc393))
* add REST network foundation ([#21](https://github.com/Solierrr/mobile-app/issues/21)) ([28c1e8a](https://github.com/Solierrr/mobile-app/commit/28c1e8ad8c2cee342be816e27f74452e92a2ca53))
* add typed REST error contract ([#20](https://github.com/Solierrr/mobile-app/issues/20)) ([0f5efa0](https://github.com/Solierrr/mobile-app/commit/0f5efa09e84cc7f19d54d508a2eaee3880301752))
* create a project from the app ([#32](https://github.com/Solierrr/mobile-app/issues/32)) ([211ad13](https://github.com/Solierrr/mobile-app/commit/211ad13ec2a1aab32da946f3021650a660beaf7e))
* criar conversa direta com uma pessoa ([#42](https://github.com/Solierrr/mobile-app/issues/42)) ([0717d9b](https://github.com/Solierrr/mobile-app/commit/0717d9bb11af9a6d2c5d590a781a8829963c4941))
* editar e excluir mensagem própria ([#41](https://github.com/Solierrr/mobile-app/issues/41)) ([edb049d](https://github.com/Solierrr/mobile-app/commit/edb049d064e70befd9e0f1e404bc7eb84e53e0d6))
* Fila offline de mensagens do chat e tempo real ao messenger ([#38](https://github.com/Solierrr/mobile-app/issues/38)) ([2c0281a](https://github.com/Solierrr/mobile-app/commit/2c0281af4112eb4eec9eced5104065261c726444))
* leitura, silenciar e arquivar conversas ([#40](https://github.com/Solierrr/mobile-app/issues/40)) ([91a6209](https://github.com/Solierrr/mobile-app/commit/91a6209f769a93b88ab6374ed719afedf8e733d5))
* load the project sections from the api ([#29](https://github.com/Solierrr/mobile-app/issues/29)) ([77e7d54](https://github.com/Solierrr/mobile-app/commit/77e7d54b59b6b4a651f09f576cdd5a5c75013b74))
* mobile frontend with flavors, domain layer, deeplink login and qa integration ([#26](https://github.com/Solierrr/mobile-app/issues/26)) ([72634d9](https://github.com/Solierrr/mobile-app/commit/72634d921d7e7e9ba5769cc43c728fe3318aa26a))
* presença e digitando nas conversas ([#43](https://github.com/Solierrr/mobile-app/issues/43)) ([b641955](https://github.com/Solierrr/mobile-app/commit/b64195526b8663dd6685731db30f23017685fd6e))
* read chat history offline with a local Room cache ([#39](https://github.com/Solierrr/mobile-app/issues/39)) ([0066df1](https://github.com/Solierrr/mobile-app/commit/0066df14e0115c2dbdef341e9c3a3d2deea5da2a))
* rebrand the app as elos and show real recent companies ([#33](https://github.com/Solierrr/mobile-app/issues/33)) ([03e5798](https://github.com/Solierrr/mobile-app/commit/03e5798b5c3c8ea4433fe12f776950489a062efb))
* use the own profile for the home avatar ([#31](https://github.com/Solierrr/mobile-app/issues/31)) ([bfd15e8](https://github.com/Solierrr/mobile-app/commit/bfd15e8282f447586621a02cf1cdc6a4fc5a5c01))


### Bug Fixes

* read the own profile from the self profile response ([#30](https://github.com/Solierrr/mobile-app/issues/30)) ([cbef300](https://github.com/Solierrr/mobile-app/commit/cbef30050048d683c397d30cbc29f95acfc6530f))
* send the pkce challenge method in the login transaction ([#28](https://github.com/Solierrr/mobile-app/issues/28)) ([6f16ef2](https://github.com/Solierrr/mobile-app/commit/6f16ef2244d9de09da6ca65a4bbd88bf9857a485))
* subscribe to the private errors topic of the user ([#37](https://github.com/Solierrr/mobile-app/issues/37)) ([da2d8a5](https://github.com/Solierrr/mobile-app/commit/da2d8a543896e679e70acb6022932d0b3e8368b5))

## [0.2.0](https://github.com/Solierrr/mobile-app/compare/v0.1.0...v0.2.0) (2026-09-30)


### Features

* add android makefile ([#13](https://github.com/Solierrr/mobile-app/issues/13)) ([1e55cb3](https://github.com/Solierrr/mobile-app/commit/1e55cb37d812e35f6a8b5c4596a465acf72079f9))
* self-bootstrap infra-scripts and split env flow into vault-config, vault-auth, extract-env ([ec52d87](https://github.com/Solierrr/mobile-app/commit/ec52d87c8f9c10da077f2705a755fb051501fcd7))


### Bug Fixes

* corrigir download do artifact de coverage no workflow do SonarQube ([91a10f4](https://github.com/Solierrr/mobile-app/commit/91a10f4d954f3a2001163a2fc4624eada2afbc16))
* pass workflow run id when downloading coverage artifact for sonarqube ([6ded34e](https://github.com/Solierrr/mobile-app/commit/6ded34e47457d5d39cf61b456ffbac9316e166b6))
* replace wildcard import and remove unused import ([ea6385b](https://github.com/Solierrr/mobile-app/commit/ea6385b40b9c2e29a9e434e9aa31f80584398036))
* support powershell secret extraction ([525b812](https://github.com/Solierrr/mobile-app/commit/525b81202b7bd69de4b325566a4565dedbe8fc9a))
