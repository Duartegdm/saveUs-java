# Guia do Calor: IA generativa com RAG

Documento da funcionalidade de IA do SaveUs (disciplina Disruptive Architectures: IoT, IoB & Generative IA).

## Funcionalidade e público

**Para quem:** pessoas idosas em São Paulo durante ondas de calor, o grupo que mais adoece no calor extremo.

**O que faz:** ao abrir a tela Guia do Calor, o idoso recebe um guia curto para o dia, feito a partir da
**reserva real dele** (qual refúgio, endereço, horário, se é acessível). Não é um chat: ninguém digita nada e a
resposta tem estrutura fixa (resumo + 3 a 5 dicas) que o app desenha com fonte grande e linguagem simples.

**Por que IA:** as orientações gerais de saúde são as mesmas para todos, mas o idoso age melhor com instruções
concretas ("chegue ao CEU Penha às 7h, antes do sol forte") do que com uma lista genérica. O modelo adapta as
orientações à situação de cada pessoa.

## Arquitetura

```
app ──JWT──▶ refugio-service ──OpenFeign + JWT──▶ ia-service ──Spring AI ChatClient──▶ LLM (gpt-4o-mini)
              │ Usuario + próxima Reserva + Refugio      │
              │ (Oracle, tabelas T_RFG_*)                 ├─ BaseDeConhecimento (BM25)
              │                                          │   conhecimento/calor-e-saude.md
              └─ junta campos fixos: sinais de alerta    └─ prompt = regras + <contexto> + <referencias>
                 e SAMU 192 (nunca gerados)
```

1. O `refugio-service` lê o idoso do token, busca a próxima reserva ATIVA e monta o contexto.
2. O `ia-service` **recupera** da base de conhecimento os 4 trechos mais relevantes para a situação (com ou sem
   reserva, refúgio acessível ou não) e os coloca no prompt entre `<referencias>`.
3. O modelo **gera** o resumo e as dicas usando só o contexto e as referências; o Spring AI converte a resposta
   em `GuiaGerado` (saída estruturada) e o serviço confere o formato.
4. Se o modelo falhar, demorar mais de 20 s ou responder fora do formato, o app recebe um guia padrão escrito no
   código, com `geradoPorIa: false`.

## Aprofundamento técnico: RAG básico

**Fonte dos dados:** `ia-service/src/main/resources/conhecimento/calor-e-saude.md`, 12 trechos escritos pelo grupo
a partir das recomendações públicas sobre calor e saúde (Ministério da Saúde e OMS): hidratação, horário do sol,
ida ao refúgio, ausência de reserva, acessibilidade, roupas, casa fresca, alimentação, remédios, companhia,
sinais de alerta e noites quentes. Não há dado pessoal na base.

**Preparação:** cada seção `##` vira um trecho; o texto é normalizado (minúsculas, sem acento, sem stopwords e
sem plural) e indexado com **BM25**. A linha `palavras:` de cada trecho acrescenta sinônimos que o idoso usaria
("cadeira de rodas", "moro sozinho").

**Por que RAG, e por que busca léxica:**

- O prompt sozinho deixaria o modelo escolher as orientações de saúde. Com RAG, o conteúdo de saúde vem de uma
  base revisada pelo grupo e o modelo só adapta o texto; o prompt proíbe acrescentar orientações que não estejam
  nas referências.
- A base tem 12 trechos curtos com vocabulário controlado, então BM25 recupera bem sem embeddings. Isso evita um
  segundo modelo (custo e latência a mais dentro dos 20 s de limite), funciona com OpenAI e com Ollama sem mudar
  nada e é determinístico, o que permite testar a recuperação.
- Se a base crescer (por exemplo, com boletins da Defesa Civil), a troca para `VectorStore` do Spring AI com
  embeddings fica isolada em `BaseDeConhecimento.buscar`.

## Avaliação

Testes automatizados (`./gradlew :ia-service:test`):

| Teste | Critério | Resultado |
|---|---|---|
| `BaseDeConhecimentoTest` | 12 perguntas reais de idosos, cada uma rotulada com o trecho esperado; **hit@3** | 12/12 (100%) |
| `BaseDeConhecimentoTest` | Pergunta fora do assunto ("resultado do jogo de futebol") não recupera nada | ok |
| `GuiaDoCalorServiceTest` | Prompt leva contexto real e referências certas (com e sem reserva) | ok |
| `GuiaDoCalorServiceTest` | Resposta que não converte ou com menos de 3 dicas vira erro tratado (503 → guia padrão) | ok |
| `GuiaIaGatewayTest` (refugio-service) | Aceita de 3 a 5 dicas, corta o excesso e recusa guia incompleto | ok |
| `RefugioApiIntegrationTest` (refugio-service) | Guia com IA (`geradoPorIa: true`) e fallback com o ia-service fora (`geradoPorIa: false`) | ok |

Perguntas avaliadas: "Não tenho reserva, onde posso ficar?", "Uso cadeira de rodas, como chego lá?", "Estou com
tontura e dor de cabeça", "Tomo remédio para pressão", "Moro sozinho e tenho medo de passar mal", "Minha casa
está muito quente", "Que roupa devo vestir?", "Que horas devo ir ao refúgio?", "Quanta água devo beber?",
"Posso sair para caminhar ao meio-dia?", "O que comer nos dias quentes?", "Como dormir com a noite quente?".

**Avaliação manual da geração** (pela collection `docs/insomnia-saveus.json`, pasta 5): guia da Maria (com
reserva), guia a partir de uma data, guia do José (sem reserva) e o LLM desligado (fallback). Critérios: usa o
nome, o horário e o endereço reais; não inventa telefone, temperatura nem remédio; 3 a 5 dicas curtas; menciona
procurar um refúgio quando não há reserva.

## Uso responsável e limitações

- **Saúde fora do modelo:** sinais de alerta e o telefone do SAMU (192) são fixos no código e no app.
- **Dados mínimos:** o modelo recebe só o primeiro nome, o bairro e a reserva; nada de e-mail, telefone ou
  contato de emergência. A chave do LLM vem de variável de ambiente.
- **Contexto como dado:** contexto e referências vão delimitados e o prompt manda ignorar instruções dentro deles.
- **Limitações:** o modelo ainda pode reescrever uma orientação de forma imprecisa; a busca léxica não entende
  paráfrases sem palavras em comum com a base; a base é geral e não substitui orientação médica; sem previsão
  do tempo real, o guia não sabe a temperatura do dia.
