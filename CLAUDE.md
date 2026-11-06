# CLAUDE.md · marketplace-order-hub

Regras deste repositório. Valem para qualquer pessoa ou agente que trabalhe aqui.

## Branches

Toda branch tem um nome que diz o que está sendo feito. Nada de nomes genéricos ou gerados automaticamente (`claude/...`, `patch-1`, `teste`, `minha-branch`).

Formato: `<tipo>/<nº da issue>-<descricao-curta>`

| Tipo | Quando usar |
|---|---|
| `feat` | funcionalidade nova |
| `fix` | correção de bug |
| `refactor` | mudança de código sem mudar comportamento |
| `test` | só testes |
| `docs` | só documentação |
| `chore` | setup, build, CI, dependências |

- Descrição em português, minúsculas, sem acento, palavras separadas por hífen, no máximo umas 5 palavras.
- Uma branch por issue (ou por grupo pequeno de issues ligadas). Se juntar issues, use o número da primeira.
- Exemplos: `chore/1-setup-projeto`, `feat/5-crud-produtos`, `feat/9-adapters-marketplaces`, `fix/23-estoque-negativo`.

## Pull requests

- Um PR por branch, pequeno e completo: código, testes e README atualizados quando fizer sentido.
- O corpo do PR fecha as issues (`Closes #n`) e diz como foi testado.
- `main` só recebe código via PR com CI verde.

## Projeto

- Java 21 · Spring Boot 3 · Maven · MySQL 8 · Flyway.
- Código em inglês; README, issues e PRs em português.
- Pacotes **por camada** em `com.giovanni.orderhub`. Toda classe nova entra na pasta da sua camada:

| Pacote | O que fica |
|---|---|
| `controller` | `@RestController`: recebe a requisição, valida o DTO, chama o service |
| `service` | regra de negócio e transações |
| `repository` | interfaces Spring Data JPA |
| `model` | entidades JPA, enums e modelos de domínio |
| `dto` | records de entrada e saída da API, um subpacote por recurso (`dto/product`, `dto/order`) |
| `exception` | exceções de negócio e o `GlobalExceptionHandler` |
| `integration` | adapters dos marketplaces, um subpacote por canal (`integration/alfashop`, ...) |
| `config` | classes `@Configuration` |

- Testes espelham o pacote da classe testada (`service/OrderServiceTest`, `controller/OrderApiIntegrationTest`).
- Schema só via migrations Flyway (`ddl-auto: validate`).

## Boas práticas de código

### Design e organização
- **Controller fino:** só recebe a requisição, valida o DTO e chama o service. Regra de negócio fica no service (ou na entidade, quando é regra da própria entidade).
- **Entidade nunca sai pela API.** Entrada e saída usam DTOs (`record`), com conversão explícita.
- **Responsabilidade única:** classe e método fazem uma coisa. Método passando de ~20 linhas ou com `if` aninhado em mais de 2 níveis é sinal para extrair.
- **Injeção por construtor**, com campos `final`. Nada de `@Autowired` em campo.
- **Sem números e strings mágicas:** constantes ou enums com nome que explica.
- **Strategy em vez de `if/switch` por canal:** novo marketplace = nova classe, sem mexer no que já funciona.

### Nomes
- Nomes descrevem intenção: `reserveStock`, `findPendingOrders`, não `process`, `handle2`, `data`.
- Classes são substantivos, métodos são verbos, booleanos começam com `is`/`has`/`can`.
- Sem abreviações que só o autor entende.

### Dados e regras
- Dinheiro sempre em `BigDecimal`, nunca `double`.
- Datas em `Instant` (UTC) no banco e na API.
- `Optional` só como retorno; nunca como campo ou parâmetro. Nunca `Optional.get()` sem checar.
- `@Transactional` no service, no método que precisa ser atômico, nunca no controller.
- Validar entrada na borda (Bean Validation no DTO) e regra de negócio no service.

### Erros
- Exceções de negócio próprias (`ProductNotFoundException`, `InsufficientStockException`), tratadas num `@RestControllerAdvice` que devolve `ProblemDetail`.
- Nunca engolir exceção (`catch` vazio ou só `printStackTrace`). Ou trata, ou relança com contexto.
- Status HTTP corretos: `201` criou, `200` ok, `204` sem corpo, `400` entrada inválida, `404` não existe, `409` conflito, `422` regra de negócio violada.

### Testes
- Todo PR com regra nova traz teste da regra.
- Unitário (JUnit 5 + Mockito) para regra de service e mappers; integração (Testcontainers + MySQL) para o fluxo de ponta a ponta.
- Nome do teste diz o cenário e o esperado: `shouldSetPendingStockWhenAnyItemIsMissing`.
- Estrutura Arrange / Act / Assert, um comportamento por teste.
- Nunca desabilitar ou apagar teste para o CI passar.

### Logs
- SLF4J (`private static final Logger log = LoggerFactory.getLogger(...)`), nunca `System.out`.
- Logar eventos de negócio relevantes (pedido recebido, pedido duplicado, estoque insuficiente) com o id do pedido e o canal.
- Nunca logar dado pessoal completo ou segredo.

### Commits
- Mensagem no imperativo, em português, dizendo o que muda: `Adiciona baixa de estoque tudo-ou-nada`.
- Commits pequenos e com sentido; nada de `wip`, `ajustes`, `fix2` no histórico do PR final.

## Regra fixa

Projeto de portfólio com domínio inspirado em integração de marketplaces. **Nenhum código, payload, regra ou dado de empresa real** (Magis5, Allcare ou outra). Marketplaces, formatos de JSON e dados são inventados.
