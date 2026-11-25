# Changelog

Formato [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e [Versionamento Semântico 2.0.0](https://semver.org/lang/pt-BR/)
(`MAJOR.MINOR.PATCH[-PRÉ-RELEASE]`).

## [1.0.0-beta] - 2026-11-25

### Adicionado
- `GET /api/chamados/abertos`: fila do técnico (somente `TECNICO` e `ADMIN`), do chamado mais antigo ao mais novo.
- `Dockerfile` multi-stage (JDK 21 para build, JRE 21 para execução, usuário não-root) e `.dockerignore`.

### Corrigido
- Conexão com o banco: no Spring Boot 4 a propriedade é `spring.mongodb.uri`; a antiga era ignorada e a aplicação
  caía em `localhost:27017` em vez do MongoDB Atlas.

### Alterado
- Versão do `pom.xml` de `0.1.0-SNAPSHOT` para `1.0.0-beta` (padrão SemVer).

## [1.0.0-alpha] - 2026-10-22

### Adicionado
- API REST de chamados: abrir, consultar (por usuário e individual) e resolver.
- Autenticação por JWT emitido pelo Thindesk (mesmo `JWT_SECRET`), com papéis `TECNICO`, `ADMIN` e usuário comum.
- Regras de negócio: solicitante e datas definidos pelo servidor, chamado resolvido imutável (`409`).
- Validações, tratamento global de erros, Swagger/OpenAPI e auditoria com JaVers.

[1.0.0-beta]: https://github.com/MiguelSantanaDaCosta/-Microservi-o-de-Chamados-T-cnicos-/releases/tag/v1.0.0-beta
[1.0.0-alpha]: https://github.com/MiguelSantanaDaCosta/-Microservi-o-de-Chamados-T-cnicos-/releases/tag/v1.0.0-alpha
