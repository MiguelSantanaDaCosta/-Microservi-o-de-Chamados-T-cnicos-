# ms-chamados — Abertura e Consulta

Microserviço REST de chamados técnicos do **ThinDesk**, construído em
**Spring Boot 4.1.1** com **Java 21**, **MongoDB** e autenticação por **JWT**.

Esta branch (`feature/abertura-consulta`) entrega o **primeiro fluxo de negócio**
do serviço: o usuário autenticado pode **abrir um chamado** e **consultar seus
próprios chamados** (ou qualquer chamado, se for técnico/admin). A base
(dependências, configuração, entidade, repositório, JWT e segurança) veio da
branch `base` e permanece intacta.

---

## O que esta branch adiciona

| Camada     | Arquivo novo/alterado                              |
| ---------- | -------------------------------------------------- |
| Controller | `controller/ChamadoController.java`                |
| DTO        | `dto/ChamadoRequest.java`                          |
| Service    | `service/ChamadoService.java`                      |
| Enum       | `enums/Status.java` (corrigido no service)         |

### `ChamadoController`

Expõe três endpoints REST sob `/api/chamados`:

| Método | Rota                          | Descrição                                |
| ------ | ----------------------------- | ---------------------------------------- |
| POST   | `/api/chamados`               | Abre um novo chamado                     |
| GET    | `/api/chamados/{id}`          | Consulta um chamado pelo id              |
| GET    | `/api/chamados/usuario/{id}`  | Lista chamados de um solicitante         |

Todos exigem `Authorization: Bearer <token>` válido (o `SecurityConfig` da
foundation já bloqueia `anyRequest()` que não seja Swagger/health).

### `ChamadoService`

Concentra as regras de negócio:

- **`criar(request, solicitante)`** — monta o `Chamado` com:
  - `titulo` e `descricao` com `trim()`
  - `solicitante` extraído do JWT (`Authentication.getName()`)
  - `status = Status.ABERTO`
  - `dataAbertura = Instant.now()`
  - campos de resolução (`tecnico`, `solucao`, `dataResolucao`) ficam `null`

- **`listarPorUsuario(usuario, auth)`** — só permite listar:
  - os **próprios** chamados, ou
  - qualquer chamado, se o usuário tiver `ROLE_TECNICO` ou `ROLE_ADMIN`
  - caso contrário → `403 Forbidden`

- **`buscar(id, auth)`** — busca por id e valida:
  - `404 Not Found` se o id não existir
  - `403 Forbidden` se não for dono e não for privilegiado

### `ChamadoRequest`

DTO de entrada com validação Jakarta Bean Validation:

```java
public record ChamadoRequest(
    @NotBlank @Size(max = 120)  String titulo,
    @NotBlank @Size(max = 2000) String descricao
) {}
```

Entradas inválidas retornam `400 Bad Request` automaticamente (Hibernate
Validator acionado pelo `@Valid` no controller).

### `enums/Status`

Enum com dois valores: `ABERTO`, `RESOLVIDO`. A foundation já trazia esse
arquivo; esta branch **corrige** o `ChamadoService`, que ainda referenciava o
nome antigo `StatusChamado` (resquício de rename anterior). Também é o tipo
usado no documento `Chamado`, então serializa como string no JSON
(`"ABERTO"`, `"RESOLVIDO"`).

---

## Modelo de dados

Documento MongoDB `Chamado` na coleção `chamados`:

```json
{
  "id":            "65f1c0a8e4b0a1b2c3d4e5f6",
  "titulo":        "Impressora não imprime",
  "descricao":     "A impressora do 3º andar não responde desde ontem.",
  "solicitante":   "joao.silva",
  "status":        "ABERTO",
  "dataAbertura":  "2026-10-21T04:35:44.123Z",
  "tecnico":       null,
  "solucao":       null,
  "dataResolucao": null
}
```

`solicitante` é `@Indexed` — a query
`findBySolicitanteOrderByDataAberturaDesc` fica eficiente mesmo com muitos
documentos.

---

## Endpoints em detalhe

### `POST /api/chamados` — abrir chamado

**Headers**

```
Authorization: Bearer <jwt>
Content-Type: application/json
```

**Body**

```json
{
  "titulo": "Impressora não imprime",
  "descricao": "A impressora do 3º andar não responde desde ontem."
}
```

**Regras**

- `titulo`: obrigatório, máx. 120 caracteres
- `descricao`: obrigatória, máx. 2000 caracteres
- `solicitante` **nunca** vem do body — sempre do `sub` do JWT
- `status` inicial é sempre `ABERTO`

**Resposta `201 Created`** (com `Location: /api/chamados/{id}`)

```json
{
  "id":           "65f1c0a8e4b0a1b2c3d4e5f6",
  "titulo":       "Impressora não imprime",
  "descricao":    "A impressora do 3º andar não responde desde ontem.",
  "solicitante":  "joao.silva",
  "status":       "ABERTO",
  "dataAbertura": "2026-10-21T04:35:44.123Z",
  "tecnico":      null,
  "solucao":      null,
  "dataResolucao": null
}
```

**Erros**

| HTTP | Motivo                                    |
| ---- | ----------------------------------------- |
| 400  | `titulo`/`descricao` vazios ou muito longos |
| 401  | Sem header `Authorization` ou token inválido |

---

### `GET /api/chamados/{id}` — consultar chamado

**Headers**

```
Authorization: Bearer <jwt>
```

**Resposta `200 OK`**

```json
{
  "id":           "65f1c0a8e4b0a1b2c3d4e5f6",
  "titulo":       "Impressora não imprime",
  "solicitante":  "joao.silva",
  "status":       "ABERTO",
  "dataAbertura": "2026-10-21T04:35:44.123Z"
}
```

**Erros**

| HTTP | Motivo                                                   |
| ---- | -------------------------------------------------------- |
| 401  | Sem token / token inválido                               |
| 403  | Chamado existe, mas o usuário não é dono nem `TECNICO`/`ADMIN` |
| 404  | Não existe chamado com esse `id`                         |

---

### `GET /api/chamados/usuario/{id}` — listar por solicitante

**Headers**

```
Authorization: Bearer <jwt>
```

O `{id}` na rota é o **username** do solicitante (o mesmo `sub` do JWT).

**Resposta `200 OK`**

```json
[
  {
    "id": "65f1c0a8e4b0a1b2c3d4e5f6",
    "titulo": "Impressora não imprime",
    "solicitante": "joao.silva",
    "status": "ABERTO",
    "dataAbertura": "2026-10-21T04:35:44.123Z"
  },
  {
    "id": "65f1c0a8e4b0a1b2c3d4e5f7",
    "titulo": "VPN cai a cada 10 min",
    "solicitante": "joao.silva",
    "status": "RESOLVIDO",
    "dataAbertura": "2026-10-15T13:02:11.000Z"
  }
]
```

Lista **vazia** (`[]`) se o usuário não tiver chamados — não é erro.

**Erros**

| HTTP | Motivo                                                          |
| ---- | --------------------------------------------------------------- |
| 401  | Sem token / token inválido                                      |
| 403  | `{id}` ≠ usuário do token **e** o usuário não é `TECNICO`/`ADMIN` |

---

## Regras de autorização

Toda a autorização é feita **no service**, com base em:

1. `Authentication.getName()` — username do JWT
2. `Authentication.getAuthorities()` — roles do JWT (`ROLE_TECNICO`, `ROLE_ADMIN`)

| Operação                        | Dono do chamado | `ROLE_TECNICO` / `ROLE_ADMIN` |
| ------------------------------- | :-------------: | :---------------------------: |
| Abrir chamado                   |       ✅        |              ✅               |
| Listar chamados de um usuário   |       ✅        |              ✅               |
| Consultar um chamado por id     |       ✅        |              ✅               |

Quem não é dono **e** não é privilegiado recebe `403 Forbidden`.

O `SecurityConfig` da foundation já cobre a camada de autenticação: `401`
para requisição sem token válido, `403` para token válido mas sem permissão.
Swagger (`/swagger-ui.html`, `/v3/api-docs/**`) e health
(`/actuator/health`) permanecem públicos.

---

## Como rodar

### 1. Variáveis de ambiente

Copie `.env.example` para `.env` e preencha:

```env
MONGO_URI_CHAMADOS=mongodb+srv://USUARIO:SENHA@cluster0.xxxxx.mongodb.net/ms_chamados?appName=ms-chamados
JWT_SECRET=mesmo-segredo-do-thindesk
SERVER_PORT=8081
```

O `springboot4-dotenv` carrega o `.env` automaticamente.

> **Importante:** `JWT_SECRET` precisa ser **idêntico** ao do serviço
> autenticador (ThinDesk), senão tokens emitidos lá serão rejeitados aqui.

### 2. Build

```bash
./mvnw clean compile
```

### 3. Subir a aplicação

```bash
./mvnw spring-boot:run
```

O serviço sobe em `http://localhost:8081`.

### 4. Swagger UI

Com a app rodando, acesse:

```
http://localhost:8081/swagger-ui.html
```

Para testar endpoints protegidos no Swagger, clique em **Authorize** e cole o
JWT (sem `Bearer ` — o Swagger adiciona).

---

## Como testar manualmente

Obtenha um JWT no serviço autenticador do ThinDesk e guarde em variável:

```bash
TOKEN="eyJhbGciOiJIUzI1NiJ9..."
```

**Abrir um chamado**

```bash
curl -i -X POST http://localhost:8081/api/chamados \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "titulo": "Impressora não imprime",
        "descricao": "A impressora do 3º andar não responde desde ontem."
      }'
```

Esperado: `201 Created` + `Location: /api/chamados/<id>` + body com o chamado.

**Consultar um chamado**

```bash
curl -i http://localhost:8081/api/chamados/<id> \
  -H "Authorization: Bearer $TOKEN"
```

**Listar chamados do usuário logado**

```bash
USER=$(curl -s http://localhost:8081/api/chamados/me -H "Authorization: Bearer $TOKEN" >/dev/null; \
       echo "$TOKEN" | cut -d. -f2 | base64 -d 2>/dev/null | jq -r .sub)
curl -s http://localhost:8081/api/chamados/usuario/$USER \
  -H "Authorization: Bearer $TOKEN" | jq
```

**Tentar ver chamado de outra pessoa (espera 403)**

```bash
curl -i http://localhost:8081/api/chamados/<id-de-outro> \
  -H "Authorization: Bearer $TOKEN"
```

---

## O que vem depois

Esta branch cobre apenas **abertura e consulta**. As próximas branches devem
implementar:

- `PATCH /api/chamados/{id}/status` — atualizar status (com transições
  válidas `ABERTO → RESOLVIDO` e regra de quem pode executar)
- `PATCH /api/chamados/{id}/solucao` — registrar técnico, solução e
  `dataResolucao`
- `GET /api/chamados?status=...` — filtros e paginação para o painel do
  técnico
- `@JaversSpringDataAuditable` no `Chamado` — auditoria via Javers/Mongo
  (dependência já declarada na foundation)
- Testes de integração (`@DataMongoTest`, `@WebMvcTest` com
  `SecurityMockMvcRequestPostProcessors`) cobrindo as regras de
  autorização do service

---

## Referências

- [Spring Boot 4.1.1 — documentação](https://docs.spring.io/spring-boot/4.1.1/)
- [Spring Data MongoDB — query methods](https://docs.spring.io/spring-data/mongodb/reference/repositories/query-methods-details.html)
- [jjwt 0.13.0 — README](https://github.com/jwtk/jjwt)
- [Bean Validation — constraints](https://jakarta.ee/specifications/bean-validation/3.1/jakarta-bean-validation-spec-3.1.html)
