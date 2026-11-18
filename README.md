# SaveUs — Backend Java (Spring Boot)

**Global Solution 2026/2 · CitizenTech · Java Advanced**

O SaveUs ajuda pessoas idosas a passar as ondas de calor em segurança: o idoso encontra e reserva vaga em um
**refúgio climático** (biblioteca, CEU, UBS, centro comunitário…) perto de casa, e o **gestor** mantém os refúgios.
Idosos são o grupo mais vulnerável ao calor extremo, e muitos não têm ar-condicionado nem informação clara sobre
onde se abrigar.

Este repositório tem a parte em **Java**: a **área do gestor** (CRUD de refúgios e consulta de CEP), o **login** e
a funcionalidade de **IA generativa**, o **Guia do Calor personalizado**. A área do idoso continua no backend
.NET; os dois backends usam **o mesmo banco Oracle, o mesmo JWT e o mesmo formato de JSON e de erro**, então o
app mobile fala com os dois sem diferença.

| Repositório | Papel |
|---|---|
| `saveUs-java` (este) | Gestor, login, CEP e Guia do Calor com IA |
| `refugio_.net` | Idoso: refúgios (leitura), reservas, perfil |
| `saveUs-mobile` | App React Native (Expo) que consome as duas APIs |

---

## Arquitetura (requisito 05 — opção **B: microsserviços**)

```
                          ┌──────────────────────┐
                          │    eureka-server     │  registro e descoberta
                          │        :8761         │
                          └──────────▲───────────┘
                     registra        │        registra
             ┌───────────────────────┴─────────────────────┐
             │                                             │
┌────────────┴─────────────┐   OpenFeign (nome no   ┌──────┴───────────────┐
│     refugio-service      │   Eureka, sem URL fixa)│      ia-service      │
│          :8080           │───────────────────────▶│        :8082         │
│ login, CRUD refúgios,    │  POST /interno/guia-   │ Spring AI ChatClient │──▶ LLM (OpenAI)        
│ CEP, Guia do Calor       │  do-calor + JWT do     │ saída estruturada    │
└───┬───────────┬──────────┘  idoso repassado       └──────────────────────┘
    │           │ RestClient
    │           └────────────▶ ViaCEP (viacep.com.br)
    │ JPA
    ▼
┌──────────────────────────┐        ┌──────────────────────┐
│ Oracle FIAP (T_RFG_*)    │◀───────│   backend .NET       │  mesmo schema e mesmo JWT
└──────────────────────────┘        └──────────────────────┘
             ▲
             │ app mobile: telas do gestor e Guia do Calor → refugio-service; telas do idoso → .NET
```

- **2 serviços de negócio** (`refugio-service` e `ia-service`) registrados no **Eureka**.
- O `refugio-service` chama o `ia-service` por **OpenFeign** com `@FeignClient(name = "ia-service")`, resolvido
  pelo Spring Cloud LoadBalancer via Eureka. Um `RequestInterceptor` repassa o header `Authorization` do idoso, e
  o `ia-service` valida o mesmo JWT: o endpoint interno não fica aberto.
- O `refugio-service` também consome o **ViaCEP** com **RestClient**.
- O app só conhece o `refugio-service`; o `ia-service` nunca é chamado direto pelo celular.

### Módulos (Gradle multi-module)

| Módulo | Porta | O que faz |
|---|---|---|
| `eureka-server` | 8761 | `@EnableEurekaServer` |
| `refugio-service` | 8080 | Web, Data JPA (Oracle), Security + OAuth2 Resource Server (JWT), Validation, Eureka Client, OpenFeign, Cache, springdoc |
| `ia-service` | 8082 | Web, Spring AI (OpenAI; Ollama opcional), Security + Resource Server, Validation, Eureka Client, springdoc |
| `saveus-common` | — | Biblioteca compartilhada: validação do JWT, ProblemDetails (advice + 401/403), contrato JSON, OpenAPI |

---

## Como rodar

**Pré-requisitos:** Java 21 (o Gradle Wrapper baixa o resto). Para a IA: uma chave da [API da OpenAI](https://platform.openai.com/api-keys)
(não precisa de GPU). Sem LLM tudo funciona: o guia sai no modo padrão (`geradoPorIa: false`).

### 1. Variáveis de ambiente

Segredos nunca ficam no repositório: tudo vem de variáveis de ambiente (veja `.env.example`).

| Variável | Onde | Para quê |
|---|---|---|
| `JWT_CHAVE` | os dois serviços | **A mesma `Jwt__Chave` do .NET**, ≥ 32 caracteres |
| `SPRING_DATASOURCE_URL` | refugio-service | `jdbc:oracle:thin:@oracle.fiap.com.br:1521/ORCL` |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | refugio-service | Schema Oracle onde estão as tabelas `T_RFG_*` |
| `OPENAI_API_KEY` | ia-service | Chave da API da OpenAI |
| `OPENAI_MODELO` | ia-service | Padrão `gpt-4o-mini` |
| `SPRING_AI_PROVEDOR` | ia-service | `openai` (padrão) ou `ollama` para rodar offline |
| `OLLAMA_BASE_URL`, `OLLAMA_MODELO` | ia-service | Só com `ollama`: padrão `http://localhost:11434` e `llama3.2` |
| `EUREKA_URL` | os dois serviços | Padrão `http://localhost:8761/eureka/` |
| `IA_TIMEOUT_MS` | refugio-service | Tempo máximo de espera pela IA; padrão `180000` (3 min) |

### 2. Pelo terminal

```bash
cp .env.example .env     # preencha JWT_CHAVE, SPRING_DATASOURCE_USERNAME/PASSWORD e OPENAI_API_KEY
./gradlew bootRun        # sobe eureka-server, ia-service e refugio-service juntos (Ctrl+C para parar)
```

O `bootRun` lê o `.env` da raiz sozinho; variáveis já exportadas no terminal têm prioridade. Para subir um
serviço por vez (um por terminal): `./gradlew :eureka-server:bootRun`, `./gradlew :ia-service:bootRun` e
`./gradlew :refugio-service:bootRun`.

Se aparecer `Failed to exec spawn helper` / `JDK version mismatch`, o JDK foi atualizado com o daemon do Gradle
aberto: rode `./gradlew --stop` e tente de novo.

No Windows, use `gradlew.bat`. O registro dos serviços no Eureka leva até ~30 s. Se as tabelas ainda não existirem no Oracle, rode
`scripts/script-bd.sql` do repositório .NET (ele também cria os usuários de teste). O Java **não cria nem altera**
tabelas (`ddl-auto=none`).

### 3. Com Docker

```bash
cp .env.example .env              # preencha JWT_CHAVE e as credenciais do Oracle
docker compose up -d --build
# opcional, sem chave da OpenAI: SPRING_AI_PROVEDOR=ollama no .env e
# docker compose --profile ollama up -d --build && docker exec saveus-ollama ollama pull llama3.2
```

### 4. Testes

```bash
./gradlew test
```

46 testes: hash de senha contra o hash real do .NET, JWT (claims, expiração, papéis), contrato JSON e de erro
de todos os endpoints (MockMvc; nos testes automatizados o Oracle é substituído por um H2 em memória), paginação, regras de exclusão, fallback do guia, limite de requisições e o
fluxo do Spring AI com um `ChatModel` falso e o documento OpenAPI dos dois serviços.

### Swagger / OpenAPI

| Serviço | Swagger UI | OpenAPI (JSON) | Cópia no repositório |
|---|---|---|---|
| refugio-service | http://localhost:8080/swagger-ui.html | http://localhost:8080/v3/api-docs | [`docs/refugio-service.json`](docs/refugio-service.json) |
| ia-service | http://localhost:8082/swagger-ui.html | http://localhost:8082/v3/api-docs | [`docs/ia-service.json`](docs/ia-service.json) |

Eureka: http://localhost:8761

**Como testar pelo Swagger:** em `Autenticação`, execute `POST /auth/login` com `gestor@refugio.com` / `Senha@123`
(ou `maria@email.com` para o Guia do Calor), copie o `token`, clique em **Authorize** e cole só o token. A
autorização fica salva ao recarregar a página.

O que a documentação traz:

- **Todos os endpoints** agrupados por tag (Autenticação, Refúgios, CEP, IA: Guia do Calor), com descrição das
  regras de negócio, quem pode chamar (papel) e o cadeado só nas rotas protegidas.
- **Schemas com exemplos** de cada request e response (`Refugio`, `RefugioRequest`, `PaginaResponseRefugio`,
  `LoginResponse`, `GuiaDoCalor`...), com formatos (`HH:mm`, `yyyy-MM-dd`, enums) e restrições.
- **Erros documentados em todas as operações** com o schema `ProblemDetails` (`application/problem+json`) e
  exemplos reais: 400 com `errors` por campo, 401 (sem token / sessão expirada), 403, 404, 409, 429 com
  `Retry-After`, 503 e 500.
- No ia-service, o endpoint interno `/interno/guia-do-calor`, explicando a chamada via OpenFeign e o uso do Spring AI.

Os arquivos em `docs/` são gerados pelos testes (`./gradlew test` grava em `*/build/openapi/`) e podem ser
abertos em qualquer visualizador OpenAPI ou importados no Postman/Insomnia.

### Insomnia

A collection [`docs/insomnia-saveus.json`](docs/insomnia-saveus.json) testa o fluxo inteiro (41 requisições):

1. No Insomnia: **Import** → arquivo `docs/insomnia-saveus.json`.
2. Escolha o ambiente **Local** (ou **Emulador / rede local**, ou preencha **Nuvem**).
3. Rode as pastas em ordem:

| Pasta | O que testa |
|---|---|
| 0. Saúde e documentação | Health dos dois serviços, registro no Eureka e OpenAPI |
| 1. Autenticação | Login dos 3 usuários de teste, credenciais erradas (401), campos vazios (400), cadastro (201), e-mail repetido (409), senha fraca |
| 2. Gestor — Refúgios (CRUD) | Listar paginado e com filtros → criar → obter → editar → pausar → reativar → excluir (204) → 404; excluir com reserva (409) |
| 3. Validação e segurança | Erros por campo, número em string, enum inválido, data errada, sem token / token inválido (401), idoso no CRUD e no CEP (403) |
| 4. CEP | ViaCEP: encontrado, formato inválido (400), inexistente (404) |
| 5. IA — Guia do Calor | Guia da Maria (com reserva), a partir de uma data, do José (sem reserva), gestor (403) |
| 6. ia-service direto | Endpoint interno com o token do idoso e sem token (401) |

Não precisa copiar token: as variáveis `token_gestor`, `token_maria` e `token_jose` pegam o `token` da resposta
do login (e fazem o login sozinhas, se ainda não houver resposta), e `refugio_id` pega o `id` do refúgio criado
em **Criar refúgio**. O "Excluir refúgio com reserva" usa o id `1`: troque por um refúgio que tenha reserva ATIVA
de hoje em diante (crie a reserva pelo app ou pelo .NET).

---

## Usuários de teste

Todos com a senha **`Senha@123`** (criados pelo `scripts/script-bd.sql` do .NET):

| E-mail | Papel | Observação |
|---|---|---|
| `gestor@refugio.com` | GESTOR | Cadastra, edita, pausa e exclui refúgios; consulta CEP |
| `maria@email.com` | IDOSO | Bairro Penha. Reserve um refúgio pelo app (.NET) para ver o Guia do Calor com refúgio |
| `jose@email.com` | IDOSO | Sem reserva (Guia do Calor sugere procurar um refúgio) |

---

## Endpoints principais (refugio-service)

| # | Método e rota | Papel | Uso no app |
|---|---|---|---|
| 1 | `POST /auth/login` | público | Login (token vale também no .NET) |
| 2 | `POST /auth/registrar` | público | Cadastro de idoso (o app usa o do .NET) |
| 3 | `GET /api/v1/refugios?bairro=&data=&ativo=&pagina=&tamanho=` | logado | Lista do gestor, paginada |
| 4 | `GET /api/v1/refugios/{id}?data=` | logado | Editar refúgio |
| 5 | `POST /api/v1/refugios` | GESTOR | Novo refúgio |
| 6 | `PUT /api/v1/refugios/{id}` | GESTOR | Editar, pausar e reativar (`ativo`) |
| 7 | `DELETE /api/v1/refugios/{id}` | GESTOR | Excluir (409 se houver reserva ativa de hoje em diante) |
| 8 | `GET /api/v1/cep/{cep}` | GESTOR | Preencher endereço pelo ViaCEP |
| 9 | `POST /api/v1/ia/guia-do-calor` | IDOSO | Tela Guia do Calor (Spring AI) |

Interno (só via Feign): `POST /interno/guia-do-calor` no ia-service, papel IDOSO.

### Exemplo rápido

```bash
TOKEN=$(curl -s localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"gestor@refugio.com","senha":"Senha@123"}' | jq -r .token)

curl -s "localhost:8080/api/v1/refugios?bairro=penha&tamanho=5" -H "Authorization: Bearer $TOKEN"

curl -s localhost:8080/api/v1/refugios -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{
  "nome": "Biblioteca Municipal da Penha", "tipo": "BIBLIOTECA", "cep": "03601-000",
  "endereco": "Rua Exemplo, 100", "bairro": "Penha", "capacidade": 20,
  "abre": "08:00", "fecha": "20:00", "acessivel": true, "ativo": true }'
```

---

## Como cada requisito de Java Advanced é atendido

| Requisito | Onde |
|---|---|
| **01. API REST, 3 entidades relacionadas, CRUD com JPA e paginação** | `Usuario`, `Refugio` e `Reserva` (`@ManyToOne` para idoso e refúgio) mapeadas nas tabelas `T_RFG_*` do .NET; CRUD completo de refúgios; `GET /api/v1/refugios` paginado com o envelope `{itens, pagina, tamanho, totalItens, totalPaginas}` (página começa em 1) e filtros por `Specification`; vagas da página calculadas numa única consulta agrupada |
| **02. Bean Validation e `@RestControllerAdvice`** | DTOs com `@NotBlank`, `@Size`, `@Positive`, `@Max` e validadores próprios (`@CepValido`, `@SenhaForte`, `@FechaDepoisDeAbre` de classe ligado ao campo `fecha`); `TratamentoGlobalDeErros` devolve **ProblemDetails (RFC 7807)** em `application/problem+json`, com `errors` por campo em camelCase e mensagens em português, idêntico ao .NET. 401/403/429 saem no mesmo formato pelo `AuthenticationEntryPoint`, `AccessDeniedHandler` e filtro de limite |
| **03. Spring Security + JWT, 2 papéis** | `IDOSO` e `GESTOR`. HS256 com a mesma chave, `iss`, `aud` e claims do .NET (`NimbusJwtEncoder`/`NimbusJwtDecoder`, `role` → `ROLE_*`); senhas no formato **PBKDF2 do ASP.NET Core Identity** (`AspNetIdentityPasswordEncoder`), então quem se cadastrou no .NET loga no Java. Escrita de refúgios e CEP só para GESTOR; guia só para IDOSO. Sessão stateless, limite de 10 tentativas/min no login |
| **04. Spring AI integrado ao fluxo** | **Guia do Calor personalizado** (abaixo): usa a reserva real do idoso, com `ChatClient` e saída estruturada `entity(GuiaGerado.class)`, validação da saída, fallback e cache |
| **05. Arquitetura distribuída — opção B** | `eureka-server` + `refugio-service` + `ia-service`; comunicação por **OpenFeign** via Eureka (com repasse do JWT) e **RestClient** para o ViaCEP |
| **06. Swagger, README, consumo pelo mobile** | springdoc nos dois serviços: Bearer, schemas com exemplos e todos os erros em ProblemDetails documentados; OpenAPI exportado em `docs/`; este README; o app usa as rotas do gestor e o guia (seção abaixo) |

---

## Spring AI: Guia do Calor personalizado

Não é um chat: o idoso abre a tela e recebe um guia curto **feito a partir da reserva dele**.

1. O app chama `POST /api/v1/ia/guia-do-calor` com o token do idoso (corpo opcional `{ "data": "2026-11-27" }`).
2. O `refugio-service` lê o `sub` do token, busca o **Usuario** e a próxima **Reserva** ATIVA (data ≥ hoje em
   São Paulo) com o **Refugio**.
3. Monta o contexto (primeiro nome, data, bairro, refúgio, endereço, horário, acessibilidade) e chama o
   `ia-service` por OpenFeign.
4. O `ia-service` usa `chatClient.prompt().system(...).user(contexto).call().entity(GuiaGerado.class)`: o Spring AI
   envia o JSON Schema, o modelo responde e o Spring AI converte. A saída é conferida (resumo + 3 a 5 dicas).
5. O `refugio-service` junta os campos **fixos no código** — `sinaisDeAlerta` e `emergencia` (SAMU 192) —
   porque informação de saúde e telefone de emergência não podem depender do modelo.

**Proteções:** o prompt de sistema proíbe inventar endereços, horários e telefones, diagnóstico ou remédio; o
contexto vai delimitado como dado; temperatura 0,3. **Fallback:** se o LLM falhar, passar de 3 minutos ou devolver algo
que não converte, a resposta é 200 com um guia padrão escrito no código e `geradoPorIa: false` — para o idoso, um
guia genérico é melhor que uma tela de erro. **Cache:** o guia fica guardado por contexto (idoso, data e reserva)
com `@Cacheable`, então reabrir a tela não chama o LLM de novo; se a reserva mudar, o contexto muda e um guia novo
é gerado.

Provedor: **OpenAI** (`gpt-4o-mini` por padrão, configurável em `OPENAI_MODELO`) pelo starter
`spring-ai-starter-model-openai`. O modelo roda nos servidores da OpenAI, então o ia-service não precisa de GPU
e responde em poucos segundos. Para rodar offline, `SPRING_AI_PROVEDOR=ollama` troca para um modelo local
(como no exemplo da disciplina) sem mudar código: o `ChatClient` é o mesmo para os dois provedores.

---

## Contrato comum com o app (igual ao .NET)

- Campos em camelCase; enums em MAIÚSCULAS (`CENTRO_COMUNITARIO`); data `yyyy-MM-dd`; horário `HH:mm`;
  data e hora ISO 8601 com o offset de São Paulo (`"2026-11-17T17:00:00-03:00"`).
- "Hoje" é sempre calculado em `America/Sao_Paulo`, nunca no fuso do servidor.
- Números só como números (`"capacidade": "20"` é recusado com 400).
- Erros:

```json
{ "status": 409, "title": "Refúgio com reservas",
  "detail": "Este refúgio tem reservas ativas para hoje ou datas futuras. Desative-o em vez de excluir." }

{ "status": 400, "title": "Dados inválidos",
  "errors": { "capacidade": ["A capacidade deve ser maior que zero."],
              "fecha": ["O horário de fechamento deve ser depois da abertura."] } }
```

| Status | Quando |
|---|---|
| 400 | Bean Validation, JSON malformado, parâmetro em formato errado |
| 401 | Sem token, token inválido ou expirado ("Sua sessão expirou. Faça login novamente."), login errado |
| 403 | Papel sem permissão |
| 404 | Refúgio ou CEP não encontrado |
| 409 | Refúgio com reservas, e-mail já cadastrado |
| 429 | Muitas tentativas de login (header `Retry-After`) |
| 503 | ViaCEP fora do ar |
| 500 | Qualquer outra exceção, sem stack trace |

---

## Integração com o app mobile

O app ganha um segundo cliente HTTP apontando para o `refugio-service`. O login continua no .NET, e o token dele
vale aqui porque chave e claims são os mesmos.

| Arquivo do app | Mudança |
|---|---|
| `.env` | `EXPO_PUBLIC_JAVA_API_URL` com a URL do refugio-service |
| `src/services/api/client.ts` | Fábrica `criarCliente(baseURL)` com os mesmos interceptors; exporta `api` (.NET) e `apiJava` |
| `src/features/gestor/*.service.ts` | Listar, criar, editar, excluir e CEP usam `apiJava` |
| `src/features/guia-calor/guia.service.ts` | `apiJava.post('/api/v1/ia/guia-do-calor', ..., { timeout: 190_000 })` |

Rodando local: emulador Android usa `http://10.0.2.2:8080`; celular físico usa `http://<IP do PC>:8080` na mesma
rede. A versão web do Expo (`http://localhost:8081`) já está liberada no CORS (`CORS_ORIGENS`).

---

## Deploy

Link do deploy: _a preencher_ (o refugio-service precisa de uma URL acessível pelo celular — nuvem ou o PC na
mesma rede Wi-Fi).

## Vídeo pitch

Link: _a preencher_

---

## Declaração de uso de IA

Usamos ferramentas de IA generativa (assistente de código) como apoio no desenvolvimento deste backend: geração
inicial de código, testes e documentação, a partir do documento de escopo escrito pelo grupo e dos exemplos de
Spring AI e microsserviços da disciplina. Todo o código foi revisado, executado e testado pelo grupo, que é
responsável pelas decisões de arquitetura e pelo conteúdo entregue. Dentro da solução, a IA generativa é usada
na funcionalidade Guia do Calor, via Spring AI, como descrito acima.
