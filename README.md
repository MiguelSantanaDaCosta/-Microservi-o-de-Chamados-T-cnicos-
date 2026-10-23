# 📘 README.md — ms-chamados

> Microserviço REST de chamados técnicos do ecossistema **ThinDesk**.
> Autenticação via **JWT emitido pelo ThinDesk**, persistência no **MongoDB**,
> documentação no **Swagger/OpenAPI** e auditoria preparada para o **JaVers**.

---

## 📑 Índice

1. [Visão geral](#-visão-geral)
2. [Requisitos funcionais atendidos](#-requisitos-funcionais-atendidos)
3. [Stack e bibliotecas](#-stack-e-bibliotecas)
4. [Arquitetura e fluxo de uma requisição](#-arquitetura-e-fluxo-de-uma-requisição)
5. [Métodos por camada](#-métodos-por-camada)
6. [Pré-requisitos](#-pré-requisitos)
7. [Passo 1 — Gerar o `JWT_SECRET`](#-passo-1--gerar-o-jwt_secret)
8. [Passo 2 — Configurar o `.env` (MongoDB Atlas)](#-passo-2--configurar-o-env-mongodb-atlas)
9. [Passo 3 — Subir a aplicação](#-passo-3--subir-a-aplicação)
10. [Passo 4 — Gerar tokens no `jwt.io`](#-passo-4--gerar-tokens-no-jwtio)
11. [Passo 5 — Testar no Swagger](#-passo-5--testar-no-swagger)
12. [Passo 6 — Verificar no MongoDB Atlas](#-passo-6--verificar-no-mongodb-atlas)
13. [Regras de negócio](#-regras-de-negócio)
14. [Códigos de status](#-códigos-de-status)
15. [Troubleshooting](#-troubleshooting)

---

## 🎯 Visão geral

O **ms-chamados** expõe endpoints REST para:

- **Abertura** de chamados por usuários autenticados.
- **Consulta** de chamados (por usuário ou individual).
- **Resolução** por técnicos, com solução, técnico e data registrados.

**Não emite JWT.** Valida tokens assinados pelo **ThinDesk** — ou por qualquer
emissor que compartilhe o mesmo `JWT_SECRET`. A autenticação existente é
**reutilizada**, não recriada.

---

## ✅ Requisitos funcionais atendidos

| # | Requisito | Pontuação | Endpoint / Método |
|---|---|---|---|
| 1 | **Abertura de chamado** — usuário autenticado informa título e descrição; sistema registra **solicitante**, **data de abertura** e **status ABERTO** automaticamente | 1,5 | `POST /api/chamados` → `ChamadoService.criar` |
| 2 | **Consulta e acompanhamento** — listar chamados de um usuário e consultar individualmente com status | 1,0 | `GET /api/chamados/usuario/{id}` e `GET /api/chamados/{id}` |
| 3 | **Resolução do chamado** — técnico registra solução, altera para **RESOLVIDO** e registra **data de resolução** | 1,5 | `PUT /api/chamados/{id}/resolver` → `ChamadoService.resolver` |
| 4 | **Regras de negócio e integração** — validações, persistência no MongoDB, autenticação existente, **impedir alteração após resolvido** | 1,0 | `@Valid`, `@Document`, `@PreAuthorize`, `ChamadoJaResolvidoException` → 409 |

---

## 🧱 Stack e bibliotecas

### Runtime

| Tecnologia | Versão | Uso |
|---|---|---|
| Java | 21 | Linguagem |
| Spring Boot | 4.1.1 | Framework base |
| MongoDB | 7 (Atlas ou Docker) | Persistência |

### Dependências principais (`pom.xml`)

| Dependência | Uso |
|---|---|
| `spring-boot-starter-webmvc` | Endpoints REST |
| `spring-boot-starter-data-mongodb` | `MongoRepository`, `@Document` |
| `spring-boot-starter-security` | Cadeia de filtros, `@PreAuthorize` |
| `spring-boot-starter-validation` | `@NotBlank`, `@Size`, `@Valid` |
| `jjwt-api` / `impl` / `jackson` (0.13.0) | Parse e validação de JWT |
| `springdoc-openapi-starter-webmvc-ui` (3.0.3) | Swagger UI |
| `javers-spring-boot-starter-mongo` (7.11.9) | Auditoria de alterações |
| `springboot4-dotenv` (5.0.1) | Leitura do `.env` |
| `lombok` (1.18.36) | `@Data`, `@Builder`, `@RequiredArgsConstructor` |

---

## 🏗 Arquitetura e fluxo de uma requisição

```
Cliente  →  JwtAuthFilter  →  SecurityConfig  →  ChamadoController
                                                     │
                                                     │ @PreAuthorize (só resolver)
                                                     ▼
                                              ChamadoService ⭐
                                                     │
                                                     ▼
                                              ChamadoRepository
                                                     │
                                                     ▼
                                                   MongoDB
                                                     ▲
                                                     │ AuthorProvider
                                                 JaVers (auditoria)
```

### Perguntas que cada camada responde

| Camada | Arquivo | Pergunta | Se falhar |
|---|---|---|---|
| Filtro JWT | `security/JwtAuthFilter.java` | "Quem é você?" | Não popula o contexto (não lança) |
| Regras de rota | `config/SecurityConfig.java` | "Essa rota exige login?" | **401 vazio** |
| Papel | `controller/ChamadoController.java` (`@PreAuthorize`) | "Você tem papel para isso?" | **403** genérico |
| Tradução HTTP | `controller/ChamadoController.java` | "Como converto HTTP ↔ objeto?" | **400** via `@Valid` |
| Negócio | `service/ChamadoService.java` | "Quais são as regras?" | **403, 404, 409** |
| Persistência | `repository/ChamadoRepository.java` | "Como falo com o banco?" | Exceção de infra |
| Armazenamento | MongoDB | "Onde persisto a verdade?" | Erro de conexão |
| Erros | `exception/GlobalExceptionHandler.java` | "Como devolvo o erro?" | — |

---

## 🔧 Métodos por camada

### `ChamadoService` — regras de negócio

| Método | Entrada | Saída | Regra protegida |
|---|---|---|---|
| `criar(ChamadoRequest, String solicitante)` | DTO + username do JWT | `Chamado` persistido | **Cliente pede, servidor define** — solicitante vem do JWT, status e data da factory |
| `listarPorUsuario(String usuario, Authentication auth)` | username + `Authentication` | `List<Chamado>` | **Só o dono vê** (ou TECNICO/ADMIN) |
| `buscar(String id, Authentication auth)` | id + `Authentication` | `Chamado` | **404 antes de 403** (não vaza existência) |
| `resolver(String id, ResolucaoRequest, String tecnico)` | id + DTO + username do JWT | `Chamado` atualizado | **Imutável após RESOLVIDO** (409) |
| `obter(String id)` *(privado)* | id | `Chamado` | Helper — busca ou lança 404 |

### `ChamadoController` — endpoints

| Método HTTP | Rota | Método Java | Papel exigido |
|---|---|---|---|
| `POST` | `/api/chamados` | `abrir` | Autenticado |
| `GET` | `/api/chamados/usuario/{id}` | `listarPorUsuario` | Autenticado (service filtra) |
| `GET` | `/api/chamados/{id}` | `buscar` | Autenticado (service filtra) |
| `PUT` | `/api/chamados/{id}/resolver` | `resolver` | **TECNICO** ou **ADMIN** |

### `Chamado` — entidade (`@Document(collection = "chamados")`)

| Campo | Tipo | Quem define | Observação |
|---|---|---|---|
| `id` | String | Mongo | `@Id` |
| `titulo` | String | Cliente (via `@Valid`) | `trim()` na factory |
| `descricao` | String | Cliente (via `@Valid`) | `trim()` na factory |
| `solicitante` | String | **JWT** | `@Indexed` (usado na listagem) |
| `status` | `Status` (enum) | **Servidor** | `ABERTO` → `RESOLVIDO` |
| `dataAbertura` | `Instant` | **Servidor** | `Instant.now()` em UTC |
| `solucao` | String | Cliente (via `@Valid` no `resolver`) | `trim()` |
| `tecnico` | String | **JWT** | Só preenchido ao resolver |
| `dataResolucao` | `Instant` | **Servidor** | `Instant.now()` em UTC |

### DTOs

| Record | Campos | Validações |
|---|---|---|
| `ChamadoRequest` | `titulo`, `descricao` | `@NotBlank`, `@Size(max=120)`, `@Size(max=2000)` |
| `ResolucaoRequest` | `solucao` | `@NotBlank`, `@Size(max=2000)` |

> ⚠️ **Nenhum dos dois DTOs tem `solicitante`, `status`, `tecnico` ou datas.** Esses campos são controlados pelo servidor — o cliente não tem como injetá-los.

---

## 🧰 Pré-requisitos

| Ferramenta | Versão mínima | Verificação |
|---|---|---|
| Java JDK | 21 | `java -version` |
| Maven (wrapper incluso) | — | `./mvnw -v` |
| Conta MongoDB Atlas | grátis | [mongodb.com/atlas](https://www.mongodb.com/atlas) |
| Navegador | moderno | Swagger UI, jwt.io |

> Alternativa ao Atlas: MongoDB local via Docker (`docker compose up -d`).

---

## 🔑 Passo 1 — Gerar o `JWT_SECRET`

O segredo precisa ter **≥ 32 caracteres** (o `jjwt` exige **256 bits** para HS256).

### Opção A — `openssl` (recomendada)

```bash
openssl rand -hex 32
```

Saída de exemplo (**não use esta** — gere a sua):

```
a1b2c3d4e5f6...sua-chave-aleatoria-de-64-caracteres-hex...fim
```

### Opção B — site gerador

1. Abrir <https://www.browserling.com/tools/random-hex>.
2. **Length**: `64`.
3. **Generate** → copiar a string.

### Opção C — gerenciador de senhas

Criar senha de **64 caracteres** alfanuméricos no 1Password / Bitwarden / KeePassXC.

> ⚠️ **Não use a chave de exemplo deste README em produção.** Sempre gere a sua.
> **Não faça commit do `.env`** — ele já está no `.gitignore`.

---

## 🌐 Passo 2 — Configurar o `.env` (MongoDB Atlas)

### 2.1 — Criar o cluster no Atlas

1. Acessar <https://cloud.mongodb.com> e criar conta grátis.
2. **Create** → **M0 Free** (grátis).
3. **Provider**: AWS / Google Cloud / Azure (qualquer um).
4. **Region**: mais próxima do seu país.
5. **Create Deployment**.
6. **Database Access** → **Add New Database User**:
   - Username: `ms-chamados-user`
   - Password: gerar e **anotar**
   - Role: `Read and write to any database`
7. **Network Access** → **Add IP Address** → **Allow Access from Anywhere** (`0.0.0.0/0`) *para desenvolvimento*. Em produção, restrinja.
8. **Clusters** → **Connect** → **Drivers** → copiar a **connection string**:

```
mongodb+srv://ms-chamados-user:<password>@cluster0.xxxxx.mongodb.net/?retryWrites=true&w=majority
```

9. **Adicionar o nome do banco** (`/ms_chamados`) **antes** do `?`:

```
mongodb+srv://ms-chamados-user:<password>@cluster0.xxxxx.mongodb.net/ms_chamados?retryWrites=true&w=majority
```

### 2.2 — Criar o `.env`

Copiar `.env.example` para `.env`:

```bash
cp .env.example .env
```

Editar o `.env` com os seus valores:

```env
MONGO_URI_CHAMADOS=mongodb+srv://ms-chamados-user:SUA_SENHA@cluster0.xxxxx.mongodb.net/ms_chamados?retryWrites=true&w=majority
JWT_SECRET=cole-aqui-o-resultado-do-openssl-rand-hex-32
SERVER_PORT=8081
```

### 2.3 — `.env.example` (template — **sem segredos reais**)

O `.env.example` versionado **não deve conter segredos reais**. O conteúdo ideal:

```env
# Copie para .env e preencha com os seus valores.
# NUNCA faça commit do .env.

# MongoDB Atlas — string do cluster com /ms_chamados no path
MONGO_URI_CHAMADOS=mongodb+srv://USUARIO:SENHA@cluster0.xxxxx.mongodb.net/ms_chamados?retryWrites=true&w=majority

# Alternativa local via docker compose
# MONGO_URI_CHAMADOS=mongodb://localhost:27017/ms_chamados

# Segredo JWT — gere com: openssl rand -hex 32
# Precisa ter >= 32 caracteres (HS256 exige 256 bits)
JWT_SECRET=GERE_COM_openssl_rand_hex_32

# Porta do servidor
SERVER_PORT=8081
```

> 🔐 **Regra de ouro:** o `.env` **nunca** vai para o Git (já está no `.gitignore`).
> O `.env.example` **só tem placeholders**.

---

## ▶ Passo 3 — Subir a aplicação

### Opção A — MongoDB Atlas (recomendado)

```bash
# 1) build
./mvnw clean package -DskipTests

# 2) rodar
./mvnw spring-boot:run
```

Aguardar no log:

```
Started ChamadosApplication in X.XXX seconds
```

### Opção B — MongoDB local via Docker

```bash
docker compose up -d
./mvnw spring-boot:run
```

No `.env`, use:

```env
MONGO_URI_CHAMADOS=mongodb://localhost:27017/ms_chamados
```

### Verificação rápida

```bash
# Health check (rota pública)
curl http://localhost:8081/actuator/health
# → {"status":"UP"}
```

---

## 🎫 Passo 4 — Gerar tokens no `jwt.io`

> Este serviço **não emite** tokens. Ele **valida** tokens do ThinDesk.
> Para testar, geramos tokens manualmente no <https://jwt.io> com o **mesmo
> `JWT_SECRET`** que está no seu `.env`.

### 4.1 — Técnico (pode resolver chamados)

**Abrir** <https://jwt.io> → limpar a tela (botão **Clear** ou F5).

**Header** (deixar o padrão):

```json
{ "alg": "HS256", "typ": "JWT" }
```

**Payload**:

```json
{
  "sub": "joao.silva",
  "roles": ["TECNICO"],
  "preAuth": false,
  "iat": 1730000000,
  "exp": 4070908800
}
```

**Verify Signature** — colar **o seu `JWT_SECRET`** (o mesmo do `.env`):

```
<COLE_AQUI_O_SEU_JWT_SECRET>
```

⚠️ **Deixe o checkbox `secret base64 encoded` DESMARCADO.** O segredo é texto
puro (hex), não Base64.

**Copiar o token** do painel **Encoded** (à direita) usando o **ícone 📋** —
não selecione com o mouse, o texto pode ser truncado com `...`.

### 4.2 — Usuário comum (só consulta os próprios)

Trocar **apenas** o Payload. O Header e o segredo ficam iguais:

```json
{
  "sub": "maria.souza",
  "roles": [],
  "preAuth": false,
  "iat": 1730000000,
  "exp": 4070908800
}
```

Copiar o novo token do painel **Encoded**.

### 4.3 — Por que esses valores?

| Campo | Valor | Motivo |
|---|---|---|
| `sub` | `joao.silva` / `maria.souza` | Vira `auth.getName()` → o **solicitante** dos chamados |
| `roles` | `["TECNICO"]` / `[]` | O `JwtUtil` normaliza para `ROLE_TECNICO`. Lista vazia = usuário comum |
| `preAuth` | `false` | Token com `preAuth: true` é a **etapa intermediária** do login do ThinDesk — este serviço **rejeita** |
| `iat` | qualquer passado | Não é validado |
| `exp` | **futuro distante** (`4070908800` = jan/2099) | Imune a expiração durante testes |

> 💡 **Por que `exp` em 2099?** Para não ter que regenerar token toda hora.
> Em produção, o ThinDesk emite com `exp = agora + 1h`.

### 4.4 — Salvar os tokens

Crie um arquivo `tokens.txt` **fora do repositório** (ex.: `~/Desktop/tokens.txt`):

```
=== TÉCNICO (João) ===
eyJhbGciOiJIUzI1NiJ9...TOKEN_DO_TECNICO_AQUI...

=== USUÁRIO COMUM (Maria) ===
eyJhbGciOiJIUzI1NiJ9...TOKEN_DA_MARIA_AQUI...
```

> ⚠️ **Nunca faça commit desse arquivo.**

---

## 🧪 Passo 5 — Testar no Swagger

### 5.1 — Abrir

```
http://localhost:8081/swagger-ui.html
```

### 5.2 — Autorizar com o token do técnico

1. Clicar em **Authorize** (ícone de cadeado, canto superior direito).
2. No campo **Value**, colar o **token do técnico** — **sem** o prefixo `Bearer ` (o Swagger adiciona).
3. **Authorize** → **Close**.

### 5.3 — Abrir chamado

**`POST /api/chamados`** → **Try it out** → colar o body:

```json
{
  "titulo": "Impressora não imprime",
  "descricao": "A impressora do 3º andar não responde desde ontem."
}
```

→ **Execute** → esperado **201 Created**.

**Copiar o `id` da resposta.**

### 5.4 — Resolver o chamado

**`PUT /api/chamados/{id}/resolver`** → **Try it out**:

- **`id`**: colar o id copiado.
- **Body**:

```json
{ "solucao": "Reiniciado o serviço de impressão (spooler)." }
```

→ **Execute** → esperado **200 OK**.

### 5.5 — Testar a regra de imutabilidade

Repetir o mesmo `PUT` → esperado **409 Conflict** com
`Chamado <id> já está resolvido`.

### 5.6 — Testar autorização (403)

1. **Authorize** → **Logout** → **Close**.
2. Colar o **token da Maria** → **Authorize** → **Close**.
3. **`PUT /api/chamados/{id}/resolver`** em qualquer chamado → esperado **403** com
   `"Você não tem permissão para esta operação"` (barrado pelo `@PreAuthorize`).
4. **`GET /api/chamados/{id}`** no chamado do João → esperado **403** com
   `"Você não tem permissão para ver este chamado"` (barrado pelo service).

### 5.7 — Testar sem token (401)

1. **Authorize** → **Logout** → **Close**.
2. Executar qualquer endpoint autenticado → esperado **401 vazio**.

---

## 🗄 Passo 6 — Verificar no MongoDB Atlas

### 6.1 — Pelo painel web

1. Acessar <https://cloud.mongodb.com>.
2. **Clusters** → **Browse Collections**.
3. Banco: `ms_chamados` → coleção: `chamados`.
4. Confirmar os documentos com `status`, `tecnico`, `dataResolucao` preenchidos.

### 6.2 — Pelo `mongosh` (terminal)

**Instalar o `mongosh`** (Linux):

```bash
# Debian/Ubuntu
wget -qO - https://www.mongodb.org/static/pgp/server-7.0.asc | sudo apt-key add -
echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list
sudo apt update && sudo apt install -y mongodb-mongosh
```

**Conectar no Atlas** (com a mesma URI do `.env`, **sem** o `/ms_chamados` no final):

```bash
mongosh "mongodb+srv://ms-chamados-user:SUA_SENHA@cluster0.xxxxx.mongodb.net/"
```

**Comandos de verificação**:

```js
// 1) Selecionar o banco
use ms_chamados

// 2) Contar documentos
db.chamados.countDocuments()

// 3) Listar todos
db.chamados.find().pretty()

// 4) Confirmar resolução completa (as 4 condições atômicas)
db.chamados.find({
  status: "RESOLVIDO",
  tecnico: "joao.silva",
  solucao: { $exists: true, $ne: null },
  dataResolucao: { $exists: true, $ne: null }
}).pretty()

// 5) Confirmar que o trim() funcionou
db.chamados.find({
  $or: [
    { titulo: { $regex: /^\s/ } },
    { titulo: { $regex: /\s$/ } },
    { descricao: { $regex: /^\s/ } },
    { descricao: { $regex: /\s$/ } }
  ]
}).pretty()
// Esperado: vazio

// 6) Confirmar o índice em solicitante (criado pelo @Indexed)
db.chamados.getIndexes()

// 7) Confirmar ordenação da API
db.chamados.find().sort({ dataAbertura: -1 }).pretty()

// 8) Snapshots do JaVers (auditoria)
db.jv_snapshots.countDocuments()

// 9) Sair
exit
```

### 6.3 — Pelo Docker (alternativa local)

Se estiver usando `docker compose`:

```bash
docker exec -it ms-chamados-mongo mongosh
```

Depois, os mesmos comandos acima a partir do `use ms_chamados`.

---

## 📋 Regras de negócio

### 1. O cliente pede, o servidor define

- **`solicitante`** vem do **JWT** — nunca do body.
- **`status`** começa como `ABERTO` na factory `Chamado.abrir()`.
- **`dataAbertura`** é `Instant.now()` em UTC, na factory.
- **`tecnico`** vem do **JWT**, na resolução.
- **`dataResolucao`** é `Instant.now()`, na entidade.

**Como isso é garantido:** o `ChamadoRequest` **não tem** esses campos. Se o
cliente tentar enviá-los, o Jackson **ignora**.

### 2. Usuário comum só vê os próprios chamados

Checado **no `ChamadoService`**, não no controller:

```java
if (!usuario.equals(auth.getName()) && !PapelUtil.isPrivilegiado(auth)) {
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "...");
}
```

**TECNICO** e **ADMIN** veem de qualquer um (`PapelUtil.isPrivilegiado`).

### 3. Chamado resolvido é imutável

`ChamadoService.resolver` lança `ChamadoJaResolvidoException` → o
`GlobalExceptionHandler` mapeia para **409 CONFLICT**.

### 4. `404` antes de `403`

Em `buscar` e `resolver`, a busca acontece **antes** da checagem de dono —
para não vazar a existência de um id alheio.

---

## 📊 Códigos de status

| Código | Quando | Origem |
|---|---|---|
| **200** | Consulta ou resolução bem-sucedida | Controller |
| **201** | Chamado criado | `ResponseEntity.created` |
| **400** | Validação falhou (`@NotBlank`, `@Size`, JSON inválido) | `GlobalExceptionHandler` |
| **401** | Sem token ou token inválido | `HttpStatusEntryPoint` |
| **403** | Autenticado mas sem permissão | `@PreAuthorize` ou service |
| **404** | Chamado não encontrado | `ChamadoService.obter` |
| **409** | Chamado já resolvido | `ChamadoJaResolvidoException` |

---

## 🔍 Troubleshooting

| Sintoma | Causa provável | Solução |
|---|---|---|
| `401` no Swagger com token "válido" | Segredo errado ou `secret base64 encoded` marcado no jwt.io | Regenerar token com o mesmo segredo do `.env` |
| `401` após 1h | `exp` curto | Usar `exp: 4070908800` para testes |
| `403` ao resolver sendo técnico | `roles` do token não tem `"TECNICO"` | Conferir o payload no jwt.io |
| `502` ou erro de conexão com Mongo | URI errada ou IP não liberado no Atlas | Conferir connection string e **Network Access** |
| `App started` mas Swagger não abre | Porta ocupada | Conferir `SERVER_PORT` no `.env` |
| `Connection refused` no Mongo local | Container parado | `docker compose up -d` |
| Erro de build `cannot find symbol` | Falta de `./mvnw clean` | `./mvnw clean package` |
| `mongosh` reclama de SSL | Certificado | `mongosh "mongodb+srv://..." --tls` |

---

## ✅ Checklist final de teste

- [ ] `.env` configurado com Mongo (Atlas ou local) e `JWT_SECRET` próprio
- [ ] App sobe sem erro (`Started ChamadosApplication`)
- [ ] `/actuator/health` retorna `{"status":"UP"}`
- [ ] Swagger UI abre em `http://localhost:8081/swagger-ui.html`
- [ ] Token do técnico autorizado e `POST` retorna `201`
- [ ] `PUT /resolver` retorna `200` com `status: RESOLVIDO`
- [ ] Segundo `PUT` retorna `409`
- [ ] Token da Maria em endpoint de técnico retorna `403`
- [ ] Sem token, qualquer endpoint retorna `401` vazio
- [ ] `mongosh` ou Atlas Compass confirmam os documentos gravados
- [ ] Índice em `solicitante` existe (`getIndexes`)
- [ ] `trim()` validado (regex de espaços retorna vazio)

---

## 📎 Referências

- [Spring Boot — Documentação oficial](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Security — JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Spring Data MongoDB](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/)
- [JJWT (Java JWT)](https://github.com/jwtk/jjwt)
- [SpringDoc OpenAPI](https://springdoc.org/)
- [JaVers](https://javers.org/)
- [MongoDB Atlas](https://www.mongodb.com/docs/atlas/)
- [jwt.io — Debugger](https://jwt.io/)

---

