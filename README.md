# marketplace-order-hub

[![CI](https://github.com/4Diovanni/marketplace-order-hub/actions/workflows/ci.yml/badge.svg)](https://github.com/4Diovanni/marketplace-order-hub/actions/workflows/ci.yml)

API REST que recebe pedidos de marketplaces com formatos diferentes, converte tudo para um modelo único, controla o estoque e garante que o mesmo pedido não seja processado duas vezes.

> Projeto de portfólio. Os marketplaces (`AlfaShop`, `BetaMarket`), os formatos de payload e os dados são fictícios. O domínio é inspirado na minha experiência com integração de marketplaces; nenhum código ou regra de empresa foi usado.

## O problema

Um lojista que vende em vários marketplaces recebe pedidos em JSONs diferentes: nomes de campo, estrutura e até
unidade de valor (reais × centavos) mudam de um canal para outro. Se o mesmo pedido chegar duas vezes (retry do
marketplace, timeout), o estoque não pode ser baixado duas vezes. E dois pedidos simultâneos do mesmo produto não podem
deixar o estoque negativo.

## Stack

Java 21 · Spring Boot 3 · Spring Data JPA · MySQL 8 · Flyway · Bean Validation · JUnit 5 · Mockito · Testcontainers · Docker Compose · springdoc-openapi (Swagger) · GitHub Actions

## Como rodar

Pré-requisito: Docker.

```bash
git clone https://github.com/4Diovanni/marketplace-order-hub.git
cd marketplace-order-hub
docker compose up --build
```

- Health: http://localhost:8080/actuator/health
- Swagger: http://localhost:8080/swagger-ui.html (descrição, códigos de resposta e exemplos de cada marketplace)

Rodar os testes (precisa de Docker rodando, por causa do Testcontainers):

```bash
./mvnw verify
```

Rodar a app fora do Docker, usando só o banco do Compose:

```bash
docker compose up -d mysql
./mvnw spring-boot:run
```

Passo a passo para subir no seu PC (Docker, Maven/IDE ou MySQL local) e testar cada endpoint na mão, com as
respostas esperadas: [`docs/roteiro-de-testes.md`](docs/roteiro-de-testes.md).

## Arquitetura

![Fluxo do pedido: marketplaces → webhook → rate limit → mapper por canal → modelo único → regra de estoque → MySQL](docs/arquitetura.png)

1. Cada marketplace manda o pedido no próprio formato para `POST /api/webhooks/{canal}/orders`.
2. O `RateLimitService` confere o limite por minuto do canal (acima dele, `429` com `Retry-After`).
3. O mapper do canal (Strategy) valida o JSON e converte para o modelo único `IncomingOrder`.
4. O `OrderService` devolve o pedido original se ele já foi recebido; senão, o `OrderRecordingService` aplica a regra
   de estoque e grava tudo numa transação.
5. Qualquer erro sai no mesmo formato (RFC 7807) pelo `GlobalExceptionHandler`.

O fonte do diagrama é [`docs/arquitetura.mmd`](docs/arquitetura.mmd) (Mermaid).

## Endpoints

Produtos (o banco já sobe com 10 produtos fictícios, ex.: `CAM-001`):

| Método | Rota | Descrição | Respostas |
|---|---|---|---|
| `POST` | `/api/products` | Cria produto | `201` · `400` · `409` SKU duplicado |
| `GET` | `/api/products?page=0&size=20&sort=sku` | Lista paginada | `200` |
| `GET` | `/api/products/{sku}` | Detalhe | `200` · `404` |
| `PUT` | `/api/products/{sku}` | Atualiza nome e preço | `200` · `400` · `404` |
| `PATCH` | `/api/products/{sku}/stock` | Ajusta estoque com `{"delta": n}`; ao repor, confirma os pedidos pendentes desse SKU | `200` · `404` · `422` se ficar negativo |
| `DELETE` | `/api/products/{sku}` | Remove | `204` · `404` |

Exemplo:

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"sku":"MEI-001","name":"Meia esportiva","price":19.90,"stockQuantity":100}'

curl -X PATCH http://localhost:8080/api/products/MEI-001/stock \
  -H "Content-Type: application/json" \
  -d '{"delta":-3}'
```

Pedidos:

| Método | Rota | Descrição | Respostas |
|---|---|---|---|
| `POST` | `/api/webhooks/{channel}/orders` | Recebe pedido de `alfashop` ou `betamarket`, cada um no seu formato | `201` novo · `200` já recebido · `400` payload inválido · `404` canal desconhecido · `422` SKU inexistente · `429` limite do canal · `503` estoque em disputa |
| `GET` | `/api/orders?status=&channel=` | Lista paginada, mais recentes primeiro | `200` |
| `GET` | `/api/orders/{id}` | Detalhe com itens | `200` · `404` |

Os dois marketplaces são fictícios e mandam o mesmo tipo de pedido em formatos diferentes. Exemplos em [`docs/exemplos/`](docs/exemplos):

| | AlfaShop | BetaMarket |
|---|---|---|
| Estrutura | plana, camelCase | aninhada em `order`, snake_case |
| Valores | reais (`59.90`) | centavos (`5990`) |
| Cliente | `fullName` | `first_name` + `last_name` |

```bash
curl -X POST http://localhost:8080/api/webhooks/alfashop/orders \
  -H "Content-Type: application/json" -d @docs/exemplos/alfashop-pedido.json

curl -X POST http://localhost:8080/api/webhooks/betamarket/orders \
  -H "Content-Type: application/json" -d @docs/exemplos/betamarket-pedido.json
```

**Regra de estoque (tudo ou nada):** se todos os itens têm estoque, o estoque é baixado e o pedido fica `CONFIRMED`. Se qualquer item falta, nada é baixado e o pedido fica `PENDING_STOCK`. O mesmo SKU em mais de uma linha do pedido é somado antes da checagem.

**Reposição confirma pendentes:** quando o estoque de um SKU é reposto (`PATCH .../stock` com `delta` positivo), o hub
tenta confirmar os pedidos `PENDING_STOCK` que têm esse SKU, do mais antigo para o mais novo, com a mesma regra de
tudo ou nada. Um pedido que ainda não cabe continua pendente e não trava os seguintes: um pedido mais novo e menor que
caiba no estoque é confirmado. A resposta do `PATCH` já mostra o estoque depois dessas confirmações.

**Idempotência:** o mesmo pedido (`channel` + `externalOrderId`) enviado de novo devolve `200` com o pedido original
(`code: ORDER_ALREADY_RECEIVED`) e não mexe no estoque. Duas cópias chegando ao mesmo tempo são barradas pela unique
constraint no banco, e a segunda também recebe o original.

**Concorrência:** o produto tem lock otimista (`@Version`). Se dois pedidos baixam o mesmo SKU ao mesmo tempo, o que
perde a corrida relê o estoque e tenta de novo (até 3 vezes). O estoque nunca fica negativo. Se ainda assim não
conseguir, responde `503` (`CONCURRENT_STOCK_UPDATE`), e o marketplace reenvia o pedido.

O webhook responde com uma mensagem pronta para o seller, além do pedido:

```json
{
  "code": "ORDER_CONFIRMED",
  "message": "Pedido AS-10001 da AlfaShop confirmado. O estoque já foi reservado.",
  "order": { "id": 1, "channel": "ALFASHOP", "externalOrderId": "AS-10001", "status": "CONFIRMED", "totalAmount": 169.70, "...": "..." }
}
```

Cada marketplace tem um contrato próprio (formato, limite de itens, limite de requisições e comportamento de retry).
Está tudo em [`docs/marketplaces.md`](docs/marketplaces.md). Acima do limite, o hub responde `429` com `Retry-After`.

### Formato dos erros

Erros seguem a RFC 7807 (`application/problem+json`) com dois campos extras: `code`, um código estável para o front
decidir o que fazer, e `message`, um texto pronto para o seller. O `detail` fica com o detalhe técnico.

```json
{
  "type": "about:blank",
  "title": "Estoque insuficiente",
  "status": 422,
  "detail": "Estoque insuficiente para o SKU 'BON-001': disponível 0, alteração pedida -1.",
  "instance": "/api/products/BON-001/stock",
  "code": "INSUFFICIENT_STOCK",
  "message": "O produto BON-001 tem só 0 unidade(s) em estoque."
}
```

Erro não previsto vira `500` com `code: INTERNAL_ERROR` e mensagem padrão, sem stack trace na resposta.
O catálogo de códigos está em `enums/ErrorCode.java`.

## Decisões técnicas

1. **Strategy por marketplace.** Cada canal tem um `OrderPayloadMapper`; o Spring injeta todos e o hub escolhe pelo
   canal da URL. Um terceiro marketplace é uma classe nova, sem alterar o `OrderService`.
2. **Idempotência em duas camadas.** Checagem de `(channel, externalOrderId)` no service e unique constraint no banco
   para cobrir duas requisições simultâneas.
3. **Tudo ou nada no estoque.** Pedido parcialmente atendido é mais difícil de tratar do que um pedido pendente
   inteiro.
4. **Lock otimista (`@Version`) com retry.** Evita estoque negativo em concorrência sem travar a linha do produto
   durante a transação. Cada tentativa roda numa transação nova (`OrderRecordingService`).
5. **O pedido guarda o preço do marketplace**, não o preço do cadastro: é o que o cliente pagou.
6. **Erros com `code` e `message`.** O `code` é estável para o front decidir o que fazer; a `message` já vem pronta
   para o seller, separada do `detail` técnico.
7. **Flyway** para versionar o schema, em vez de `ddl-auto`.
8. **Reposição em transações separadas.** O ajuste de estoque é gravado primeiro; depois, cada pedido pendente é
   confirmado na própria transação, com `SELECT ... FOR UPDATE` no pedido (duas reposições simultâneas não confirmam
   o mesmo pedido duas vezes) e o mesmo lock otimista no produto. Se a confirmação falhar, o ajuste do seller não é
   desfeito e o pedido fica para a próxima reposição.
9. **Testcontainers com MySQL** nos testes de integração, para testar no mesmo banco da aplicação, incluindo pedidos
   simultâneos de verdade.

## Estrutura

```
src/main/java/com/giovanni/orderhub/
├── controller/    endpoints REST + interfaces *Api com a documentação do Swagger
├── service/       regras de negócio (estoque, pedidos)
├── repository/    acesso ao banco (Spring Data JPA)
├── model/         entidades (Product, Order, OrderItem) e IncomingOrder
├── enums/         Channel, OrderStatus, ErrorCode, OrderResultCode
├── dto/           entrada e saída da API, por recurso (product/, order/, error/)
├── exception/     ApiException + subpastas handler/, product/, order/, integration/
├── integration/   adapters dos marketplaces (alfashop/, betamarket/)
└── config/        configurações do Spring
```

## Configuração

| Variável | Padrão |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/orderhub` |
| `DB_USERNAME` | `orderhub` |
| `DB_PASSWORD` | `orderhub` |

## Próximos passos

O MVP está completo. A evolução está nas [issues abertas](https://github.com/4Diovanni/marketplace-order-hub/issues):

- [x] [#14](https://github.com/4Diovanni/marketplace-order-hub/issues/14) Reprocessar pedidos `PENDING_STOCK` quando o estoque for reposto
- [ ] [#15](https://github.com/4Diovanni/marketplace-order-hub/issues/15) Fila com retry e dead letter (RabbitMQ)
- [ ] [#16](https://github.com/4Diovanni/marketplace-order-hub/issues/16) Logs estruturados com correlation id
- [ ] [#17](https://github.com/4Diovanni/marketplace-order-hub/issues/17) Assinatura HMAC nos webhooks
- [ ] [#18](https://github.com/4Diovanni/marketplace-order-hub/issues/18) Cancelamento de pedido com devolução de estoque

## Autor

Giovanni Moreira · [GitHub](https://github.com/4Diovanni)
