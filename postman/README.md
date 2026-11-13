# Coleção do Postman

Requisições prontas para testar a API do `marketplace-order-hub` sem digitar `curl`. Cada requisição tem **testes** que
conferem o status e o `code` esperados, então rodar a coleção inteira diz na hora se algo quebrou.

> Os marketplaces (AlfaShop e BetaMarket) e os dados são fictícios. Para as regras de cada canal, veja
> [`docs/marketplaces.md`](../docs/marketplaces.md).

## O que tem aqui

| Arquivo | Para quê |
|---|---|
| `marketplace-order-hub.postman_collection.json` | A coleção: 28 requisições em 6 pastas, com testes e descrição em cada uma |
| `local.postman_environment.json` | Ambiente com a `baseUrl` (`http://localhost:8080`). Troque se a app estiver em outra porta |

## Como usar

### 1. Suba a aplicação

Siga o passo a passo de [`docs/roteiro-de-testes.md`](../docs/roteiro-de-testes.md#2-como-subir-a-aplicação). O jeito
mais curto:

```bash
docker compose up --build
```

Confira em http://localhost:8080/actuator/health (esperado: `{"status":"UP"}`).

### 2. Importe no Postman

1. No Postman, clique em **Import** e arraste os dois arquivos `.json` desta pasta.
2. No canto superior direito, escolha o ambiente **marketplace-order-hub (local)**.

### 3. Rode

- **Uma requisição:** clique nela e em **Send**. A aba **Test Results** mostra o que foi conferido.
- **A coleção inteira:** clique nos três pontos da coleção → **Run collection** → **Run**. Tudo deve ficar verde.

> **Rode na ordem.** As requisições se encadeiam: o produto criado no 1.4 é usado até o 1.11, o pedido do 2.1 é
> reenviado no 2.2 e consultado no 3.3. O Postman guarda esses valores em variáveis da coleção (`newSku`,
> `alfaOrderId`, `orderId`...), então não precisa copiar nada na mão.

### Sem abrir o Postman (Newman)

Os mesmos testes rodam no terminal, bom para conferir rápido ou para CI:

```bash
npx newman run postman/marketplace-order-hub.postman_collection.json \
  -e postman/local.postman_environment.json
```

Esperado: `failed` zerado em `requests` e `assertions`.

## Estado do banco

Os pedidos usam os produtos do seed (`CAM-001`, `FON-001`, `GAR-001`, `LIV-001` com estoque; `BON-001` com **estoque 0**,
de propósito, para o teste de falta de estoque). Cada rodada da coleção baixa poucas unidades. Se mexer no estoque
desses produtos, ou depois de muitas rodadas, o 2.1 e o 2.3 podem deixar de ser `ORDER_CONFIRMED`. Para voltar ao estado
inicial, veja a [seção 10 do roteiro](../docs/roteiro-de-testes.md#10-voltar-o-banco-ao-estado-inicial).

## As requisições

Cada requisição também traz o esperado na aba **Description**.

### 0. Verificação

| # | Requisição | Esperado |
|---|---|---|
| 0.1 | `GET /actuator/health` | `200`, `status` = `UP` |

### 1. Produtos

| # | Requisição | O que mostra | Esperado |
|---|---|---|---|
| 1.1 | `GET /api/products?page=0&size=3` | Lista paginada, ordenada por SKU | `200`, `content` e `page` |
| 1.2 | `GET /api/products/CAM-001` | Detalhe de um produto do seed | `200` |
| 1.3 | `GET /api/products/XXX-999` | SKU que não existe | `404` `PRODUCT_NOT_FOUND` |
| 1.4 | `POST /api/products` | Cria produto com SKU novo (gerado a cada rodada) | `201` e header `Location` |
| 1.5 | `POST /api/products` | Mesmo SKU de novo | `409` `DUPLICATE_SKU` |
| 1.6 | `POST /api/products` | Campos inválidos | `400` `VALIDATION_ERROR`, `errors` com os 4 campos |
| 1.7 | `PUT /api/products/{sku}` | Atualiza nome e preço (SKU e estoque não mudam) | `200` |
| 1.8 | `PATCH /api/products/{sku}/stock` | Retira 3 unidades (`{"delta": -3}`) | `200`, estoque 97 |
| 1.9 | `PATCH /api/products/{sku}/stock` | Retira mais do que existe | `422` `INSUFFICIENT_STOCK` |
| 1.10 | `DELETE /api/products/{sku}` | Remove | `204` |
| 1.11 | `DELETE /api/products/{sku}` | Remove de novo | `404` `PRODUCT_NOT_FOUND` |

### 2. Webhooks (pedidos dos marketplaces)

| # | Requisição | O que mostra | Esperado |
|---|---|---|---|
| 2.1 | `POST /api/webhooks/alfashop/orders` | Pedido novo da AlfaShop (JSON plano, reais) | `201` `ORDER_CONFIRMED`, `Location` |
| 2.2 | `POST /api/webhooks/alfashop/orders` | **Mesmo pedido de novo** (retry do marketplace) | `200` `ORDER_ALREADY_RECEIVED`, mesmo `id`, estoque igual |
| 2.3 | `POST /api/webhooks/betamarket/orders` | Pedido da BetaMarket (JSON aninhado, **centavos**, nome em dois campos) | `201` `ORDER_CONFIRMED`, `unitPrice` 74.90 |
| 2.4 | `POST /api/webhooks/betamarket/orders` | Um item sem estoque: **tudo ou nada**, nada é baixado | `201` `ORDER_PENDING_STOCK` |
| 2.5 | `POST /api/webhooks/gamashop/orders` | Canal que não existe | `404` `CHANNEL_NOT_FOUND` |
| 2.6 | `POST /api/webhooks/alfashop/orders` | JSON fora do contrato do canal | `400` `INVALID_MARKETPLACE_PAYLOAD`, `errors` com `buyer` e `items` |
| 2.7 | `POST /api/webhooks/alfashop/orders` | SKU fora do catálogo | `422` `UNKNOWN_SKU` |
| 2.8 | `POST /api/webhooks/alfashop/orders` | 21 itens (o limite da AlfaShop é 20) | `400` `INVALID_MARKETPLACE_PAYLOAD` |

### 3. Pedidos (consulta)

| # | Requisição | O que mostra | Esperado |
|---|---|---|---|
| 3.1 | `GET /api/orders?size=5` | Mais recentes primeiro | `200` |
| 3.2 | `GET /api/orders?status=PENDING_STOCK&channel=BETAMARKET` | Filtros opcionais | `200`, só os pendentes da BetaMarket |
| 3.3 | `GET /api/orders/{{orderId}}` | Detalhe do pedido do 2.1, com itens | `200` |
| 3.4 | `GET /api/orders/999999` | Pedido que não existe | `404` `ORDER_NOT_FOUND` |

### 4. Erros genéricos

| # | Requisição | Esperado |
|---|---|---|
| 4.1 | `POST /api/products` com JSON malformado | `400` `MALFORMED_REQUEST` |
| 4.2 | `GET /api/nao-existe` | `404` `RESOURCE_NOT_FOUND` |
| 4.3 | `DELETE /api/orders/1` | `405` `REQUEST_NOT_SUPPORTED` |

### 5. Rate limit

| # | Requisição | Esperado |
|---|---|---|
| 5.1 | `POST /api/webhooks/betamarket/orders` (sempre o mesmo pedido) | Uma vez só: `201` ou `200`. Para ver o `429`, veja abaixo |

A BetaMarket aceita 30 requisições por minuto. Para estourar o limite, rode **só a pasta 5** com **35 iterações**:

- No Postman: clique nos três pontos da pasta **5. Rate limit** → **Run folder** → em **Iterations**, digite `35` → **Run**.
- No terminal:

  ```bash
  npx newman run postman/marketplace-order-hub.postman_collection.json \
    -e postman/local.postman_environment.json --folder "5. Rate limit" -n 35
  ```

Esperado: as primeiras passam (`201`/`200`) e o restante recebe `429` com o header `Retry-After` (segundos até o canal
voltar), `code` = `RATE_LIMIT_EXCEEDED` e `retryAfterSeconds` no corpo. Deixe esta pasta por último: até a janela de
1 minuto virar, qualquer pedido da BetaMarket recebe `429`. A AlfaShop não é afetada, porque o limite é por canal.

## Usando junto com o Swagger

O Swagger (http://localhost:8080/swagger-ui.html) e esta coleção se complementam:

| | Swagger | Coleção do Postman |
|---|---|---|
| Para quê | Conhecer a API: cada endpoint com descrição, códigos de resposta e exemplo de cada marketplace | Testar a API: fluxos encadeados, com testes que conferem o resultado |
| Precisa instalar algo | Não, abre no navegador | Postman (ou `npx newman`) |
| Fluxo de vários passos | Manual | Automático, em ordem |

Uma boa sequência para quem está conhecendo o projeto:

1. Abra o Swagger, leia os grupos **Produtos**, **Webhooks** e **Pedidos** e teste o webhook pelo **Try it out** (passo a
   passo na [seção 2 do roteiro](../docs/roteiro-de-testes.md#testar-pelo-swagger-sem-terminal)).
2. Rode a coleção do Postman inteira para ver todos os casos, felizes e de erro, passando.
3. Para entender uma resposta, volte ao Swagger: ele lista todos os `code` que cada endpoint pode devolver.

**Dica:** o Postman também importa o OpenAPI direto da aplicação. Em **Import**, escolha **Link** e cole
`http://localhost:8080/v3/api-docs`. Isso gera uma coleção crua, com todas as rotas e sem testes nem encadeamento. A
coleção desta pasta é a curada, com exemplos, testes e a ordem certa.

## O que a coleção não cobre

- **Concorrência** (vários pedidos ao mesmo tempo): o Postman envia uma requisição por vez. Esses testes estão na
  [seção 8 do roteiro](../docs/roteiro-de-testes.md#8-concorrência-e-pedidos-repetidos) (com `curl`) e no teste
  automatizado `OrderApiIntegrationTest`.

## Mantendo a coleção em dia

Todo PR que cria ou muda um endpoint, uma regra ou um código de erro atualiza esta coleção (com os testes da nova
requisição) e esta tabela. Regra no [`CLAUDE.md`](../CLAUDE.md).
