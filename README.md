## Como testar manualmente (sem terminal)

Este serviço **não emite JWT**. Ele valida tokens assinados pelo ThinDesk
(ou por qualquer emissor que compartilhe o mesmo `JWT_SECRET` do `.env`).
Todo o roteiro abaixo acontece em **interface gráfica**: site de geração
de chave, editor de texto e Swagger UI. Nenhum comando de shell.

### 1. Gerar o `JWT_SECRET`

O valor precisa ter **≥ 32 caracteres** (o `jjwt` exige 256 bits para
HS256). Escolha **uma** das opções:

#### Opção A — site gerador de chave hex (recomendado)

1. Abra <https://www.browserling.com/tools/random-hex>.
2. Em **Length**, coloque `64`.
3. Clique em **Generate**.
4. Copie a string resultante (só `0-9` e `a-f`).

Exemplo de tamanho da chave :

```
L05050Z11111N07032U04013S03024C12245R02066F03097B04038J04029OP64
```

#### Opção B — gerenciador de senhas

No 1Password, Bitwarden, KeePassXC ou similar:

1. Crie uma senha nova.
2. Configure para **64 caracteres**, só letras e números.
3. Copie.

#### Opção C — frase longa (menos aleatória, mas funciona)

Se você só quer testar localmente e não se importa com a qualidade
criptográfica da chave de **desenvolvimento**:

```
thindesk-local-test-secret-key-32bytes-minimum
```

> ⚠️ Só use isso para **dev local**. Em produção, use sempre algo
> gerado por `openssl rand -hex 32` ou por um gerenciador de senhas.

---

### 2. Colocar o `JWT_SECRET` no `.env`

Abra o arquivo `.env` na **raiz do projeto** com seu editor de texto
(VSCode, Kate, gedit, Notepad++ — qualquer um). Se o arquivo não
existir, copie o `.env.example` primeiro.

Conteúdo final:

```env
MONGO_URI_CHAMADOS=mongodb://localhost:27017/ms_chamados
JWT_SECRET=L05050Z11111N07032U04013S03024C12245R02066F03097B04038J04029OP64
SERVER_PORT=8081
```

**Salve o arquivo** (Ctrl+S / Cmd+S). Não deixe espaço nem newline no
fim do valor do `JWT_SECRET`.

> Se preferir nem editar o `.env`, você pode passar a variável direto
> na configuração do Spring Boot no seu IDE (IntelliJ, Eclipse, VSCode
> Java). Mas aí perde a vantagem do `dotenv` — o arquivo é a forma
> mais simples e replicável.

---

### 3. Subir a aplicação

No terminal (o **único** comando desta seção):

```bash
./mvnw spring-boot:run
```

Ou, se você usa o IDE, clique em **Run** na classe `ChamadosApplication`.
Aguarde `Started ChamadosApplication in X.XXX seconds`.

O `.env` só é lido neste momento. Se depois você mudar o `JWT_SECRET`,
**pare e suba de novo** — o DevTools não observa `.env`.

---

### 4. Gerar os dois tokens no jwt.io

Abra <https://jwt.io>. O site tem três painéis: **Decoded** (esquerda),
**Verify Signature** (embaixo à direita) e **Encoded** (direita).

#### 4.1 Token do técnico

1. No painel **Decoded → Header**, deixe como está (`{"alg": "HS256", "typ": "JWT"}`).
2. No painel **Decoded → Payload**, cole:

   ```json
   {
     "sub": "joao.silva",
     "roles": ["TECNICO"],
     "preAuth": false,
     "iat": 1730000000,
     "exp": 1730003600
   }
   ```

3. Em **Verify Signature**, cole o valor do `JWT_SECRET` (o mesmo do
   `.env`) no campo `your-256-bit-secret`. **Marque** o checkbox
   `secret base64 encoded` **não** — o valor é texto puro.
4. O painel **Encoded** atualiza sozinho com o token completo. Copie.

> **Sobre `iat` e `exp`:** são **epoch em segundos**, não data legível.
> Use o epoch atual. Para consultar: <https://www.epochconverter.com>.
> `iat = agora`, `exp = agora + 3600` (1 hora de validade). Se `exp`
> estiver no passado, o `JwtUtil` rejeita com `401` silencioso.

#### 4.2 Token do usuário comum

Repita o passo 4.1, só trocando o payload:

```json
{
  "sub": "maria.souza",
  "roles": [],
  "preAuth": false,
  "iat": 1730000000,
  "exp": 1730003600
}
```

Copie o novo token.

> **Dica:** o jwt.io mostra o token no painel **Encoded**. Se você colar
> o token no navegador e ele aparecer com `...` no meio (truncado), use
> o botão **Copy** (ícone de prancheta) em vez de selecionar com o mouse.

---

### 5. Autorizar no Swagger UI

Abra <http://localhost:8081/swagger-ui.html>.

1. Clique em **Authorize** (cadeado, canto superior direito).
2. No campo **Value**, cole o token do **técnico** — sem o prefixo
   `Bearer ` (o Swagger adiciona).
3. Clique em **Authorize**, depois **Close**.

A partir daqui, todo `Try it out` envia o `Authorization: Bearer <token>`
automaticamente.

---

### 6. Testar cada endpoint

O padrão é sempre o mesmo: **clique no endpoint → Try it out →
preencha → Execute**.

#### `POST /api/chamados` — abrir

- **Body**:
  ```json
  {
    "titulo": "Impressora não imprime",
    "descricao": "A impressora do 3º andar não responde desde ontem."
  }
  ```
- **Esperado:** `201 Created`, e o objeto com `"status": "ABERTO"`,
  `"dataAbertura"` preenchida.
- **Guarde o `id`** para os próximos passos.

**Teste do 400 (validação):** execute de novo com:

```json
{ "titulo": "", "descricao": "" }
```

Esperado: `400` com `descricao: A descrição é obrigatória; titulo: O título é obrigatório`.

#### `GET /api/chamados/{id}` — consultar

- **Parâmetro `id`**: cole o `id` retornado no passo anterior.
- **Esperado:** `200 OK` com o chamado.

#### `GET /api/chamados/usuario/{id}` — listar por solicitante

- **Parâmetro `id`**: `joao.silva` (o `sub` do token do técnico).
- **Esperado:** `200 OK` com array (pode ser `[]`).

#### `PUT /api/chamados/{id}/resolver` — resolver

- **Parâmetro `id`**: o mesmo do chamado.
- **Body**:
  ```json
  { "solucao": "Reiniciado o serviço de impressão." }
  ```
- **Esperado:** `200 OK` com `"status": "RESOLVIDO"`, `"tecnico": "joao.silva"`,
  `"solucao"` e `"dataResolucao"` preenchidos.

**Teste do 409 (resolver duas vezes):** execute o mesmo endpoint de novo.
Esperado: `409 Conflict` + `Chamado <id> já está resolvido`.

---

### 7. Trocar o token (técnico → usuário comum)

O Swagger guarda **um** token por aba. Para trocar:

1. Clique em **Authorize**.
2. Clique em **Logout**.
3. Cole o token da Maria (o de `roles: []`).
4. **Authorize** → **Close**.

Agora os endpoints passam a usar o token da Maria.

#### Testes que só funcionam com o token da Maria

**403 do `@PreAuthorize`** (resolver chamado alheio):

- Chame `PUT /api/chamados/{id}/resolver` num chamado **ABERTO** do João.
- Esperado: `403 Forbidden` + `"Você não tem permissão para esta operação"`
  (mensagem genérica — o `@PreAuthorize` barrou **antes** do controller).

**403 do service** (ver chamado alheio):

- Chame `GET /api/chamados/{id}` com o id de um chamado do João.
- Esperado: `403 Forbidden` + `"Você não tem permissão para ver este chamado"`
  (mensagem específica do `ChamadoService.buscar`).

> **Por que dois 403 diferentes?** As duas camadas de defesa existem de
> propósito. Se as mensagens fossem iguais, você não saberia qual delas
> barrou. Assim, o teste diferencia.

---

### 8. Voltar para o modo "sem autorização" (401)

1. **Authorize → Logout → Close**.
2. Execute qualquer endpoint autenticado.
3. Esperado: `401 Unauthorized` (sem corpo — o `HttpStatusEntryPoint`
   responde vazio).

---

### 9. Inspecionar o Mongo (opcional)

Se quiser confirmar que o documento foi persistido corretamente, use
**MongoDB Compass** (GUI, não terminal):

1. Baixe em <https://www.mongodb.com/products/compass>.
2. Conecte em `mongodb://localhost:27017`.
3. Abra o banco `ms_chamados` → coleção `chamados`.
4. O documento deve ter `status: "RESOLVIDO"`, `tecnico: "joao.silva"`,
   `solucao`, `dataResolucao` preenchidos.

---

### Resumo do fluxo

```
[site gerador]    →  JWT_SECRET (64 chars)
       ↓
[editor de texto] →  .env
       ↓
[./mvnw spring-boot:run]  (único comando shell)
       ↓
[jwt.io]          →  TOKEN_TEC + TOKEN_USER
       ↓
[Swagger UI]      →  Authorize + Try it out
```

Nenhum `openssl`, `awk`, `curl` ou `export`. Todo o teste acontece
clicando.
