# ms-chamados — Foundation

Microserviço REST de chamados técnicos do **ThinDesk**, construído em
**Spring Boot 4.1.1** com **Java 21**, **MongoDB** e autenticação por **JWT**.
Esta branch entrega apenas a **base** do serviço: dependências, configuração,
entidade, repositório, utilitário de JWT, filtro de autenticação e regras de
segurança. Os endpoints de negócio vêm nas próximas branches.

---

## Stack

| Camada       | Tecnologia                                    |
| ------------ | --------------------------------------------- |
| Linguagem    | Java 21                                       |
| Framework    | Spring Boot 4.1.1                             |
| Persistência | Spring Data MongoDB                           |
| Segurança    | Spring Security 7 + JWT (jjwt 0.13.0)         |
| Validação    | Jakarta Bean Validation (Hibernate Validator) |
| Documentação | Springdoc OpenAPI 3 (Swagger UI)              |
| Auditoria    | Javers Spring Boot Starter (Mongo)            |
| Build        | Maven (wrapper incluso)                       |

---

## Bibliotecas e por que cada uma está aqui

### Spring Boot Starters

- **`spring-boot-starter-webmvc`**
  Base do serviço REST: Tomcat embutido, Spring MVC, Jackson (JSON).

- **`spring-boot-starter-data-mongodb`**
  Spring Data Mongo — permite declarar `MongoRepository<Chamado, String>` e
  receber as operações CRUD prontas, além de queries derivadas do nome do
  método (ex.: `findBySolicitanteOrderByDataAberturaDesc`).

- **`spring-boot-starter-security`**
  Corrente de filtros de segurança. É o que injeta `SecurityFilterChain`,
  `PasswordEncoder` (BCrypt), `AuthenticationManager`, e habilita
  `@EnableMethodSecurity` para `@PreAuthorize` em controllers futuros.

- **`spring-boot-starter-validation`**
  Habilita `@NotBlank`, `@Size`, `@Email` etc. nos DTOs (Bean Validation).
  O controller responde 400 automaticamente quando a entrada é inválida.

- **`spring-boot-devtools`** (runtime, opcional)
  Reinício automático durante o desenvolvimento. Não entra no artefato final.

### JWT

- **`jjwt-api` / `jjwt-impl` / `jjwt-jackson` (0.13.0)**
  Geração e validação de tokens. `api` é a superfície pública,
  `impl` faz o trabalho em runtime, `jackson` serializa as claims.
  A versão 0.13.0 é a compatível com Spring Boot 4 — a 0.12.x quebra por
  mudanças internas no Jackson 3.

### Configuração e integração

- **`springboot4-dotenv` (5.0.1)**
  Lê o arquivo `.env` e injeta as variáveis no `Environment` antes do
  Spring subir. É o que faz `${MONGO_URI_CHAMADOS}` e `${JWT_SECRET}`
  funcionarem sem exportar nada no shell. O artefato é a variante
  específica para Boot 4.

- **`springdoc-openapi-starter-webmvc-ui` (3.0.3)**
  Gera `/v3/api-docs` e monta a UI em `/swagger-ui.html`. A série 3.x é a
  compatível com Boot 4 (o 2.x quebra por causa do Spring 7).

- **`javers-spring-boot-starter-mongo` (7.11.9)**
  Auditoria de entidades: guarda snapshots de cada mudança no `Chamado`.
  A configuração real (`@JaversSpringDataAuditable`) entra na branch
  `feature/polish`, mas a dependência é declarada agora para fixar a versão
  e o backend (Mongo) junto com a base de dados.

### Apoio ao desenvolvimento

- **`lombok`**
  Elimina boilerplate: `@Data`, `@Builder`, `@RequiredArgsConstructor`.
  É anotação de compilação — não entra no jar final (excluída no
  `spring-boot-maven-plugin`).

### Testes

- **`spring-boot-starter-data-mongodb-test`**
  `@DataMongoTest` + Mongo embutido para testes de repositório.

- **`spring-boot-starter-security-test`**
  `@WithMockUser`, `SecurityMockMvcRequestPostProcessors` para testar as
  regras de autorização sem subir um usuário de verdade.

- **`spring-boot-starter-validation-test`**
  `ValidationTestUtils` para testar constraints de DTO isoladamente.

- **`spring-boot-starter-webmvc-test`**
  `@WebMvcTest` e `MockMvc` para testes de controller.

---

## O que esta branch implementa

### `ChamadosApplication`

Ponto de entrada do Spring Boot (`@SpringBootApplication`).

### `Chamado` (entity)

Documento MongoDB na coleção `chamados` com:

- `id`, `titulo`, `descricao`
- `solicitante` (indexado — busca por usuário fica rápida)
- `status` (`ABERTO` / `RESOLVIDO`)
- `dataAbertura`
- `tecnico`, `solucao`, `dataResolucao`

`@Builder` e `@Data` do Lombok reduzem o boilerplate dos getters/setters.

### `Status`

`enum` com dois estados: `ABERTO`, `RESOLVIDO`. Modelado como enum para
serializar automaticamente como string no JSON e evitar valores inválidos
no banco.

### `ChamadoRepository`

`MongoRepository<Chamado, String>` com uma query derivada:

```java
List<Chamado> findBySolicitanteOrderByDataAberturaDesc(String solicitante);
```
