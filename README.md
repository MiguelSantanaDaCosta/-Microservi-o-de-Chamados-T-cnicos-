# ms-chamados — novidades desta branch

Este documento descreve apenas o que foi adicionado nesta branch em relação
à branch base. O contrato HTTP, as regras de negócio e os endpoints existentes
permanecem iguais.

## Resumo

Esta branch adiciona:

- Configuração de documentação Swagger/OpenAPI com autenticação Bearer JWT.
- Configuração do `AuthorProvider` do Javers para registrar o autor das alterações.
- Anotações OpenAPI no `ChamadoController`.
- Comentários explicativos nos principais arquivos Java do projeto.

## Novos arquivos

### `config/OpenApiConfig.java`

Cria a configuração do OpenAPI/Swagger:

- Define o esquema de segurança `bearerAuth`.
- Informa ao Swagger que a autenticação é HTTP Bearer com JWT.
- Permite usar o botão **Authorize** na interface do Swagger UI.

O controller usa o mesmo nome de esquema (`OpenApiConfig.ESQUEMA_JWT`) para
indicar que os endpoints exigem JWT.

### `config/JaversConfig.java`

Configura o `AuthorProvider` do Javers:

- Lê o usuário autenticado do `SecurityContextHolder`.
- Usa o nome do usuário retornado pelo filtro JWT.
- Se não houver usuário autenticado, grava como `"anonimo"`.

Esse provider é usado pelo Javers para saber quem realizou alterações auditadas.

## Alterações em arquivos existentes

### `controller/ChamadoController.java`

Foram adicionadas anotações de documentação:

- `@Tag`: agrupa os endpoints como “Chamados”.
- `@SecurityRequirement`: indica que os endpoints usam o esquema `bearerAuth`.
- `@Operation`: descreve cada operação.
- `@ApiResponse`: documenta os principais códigos de resposta.

Não houve alteração na lógica dos endpoints.

### `repository/ChamadoRepository.java`

Foi adicionado o import:

```java
import org.javers.spring.annotation.JaversSpringDataAuditable;
```
