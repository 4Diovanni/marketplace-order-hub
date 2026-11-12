# Roteiro de testes

Guia para subir a aplicação no seu computador e testar cada endpoint na mão. Serve para quem está avaliando o
projeto e para quem desenvolve: **toda issue que muda ou cria comportamento acrescenta seus testes aqui.**

As respostas de exemplo foram copiadas de execuções reais. Datas e ids podem mudar na sua máquina.

## Sumário

1. [Pré-requisitos](#1-pré-requisitos)
2. [Como subir a aplicação](#2-como-subir-a-aplicação) (3 formas)
3. [Testes automatizados](#3-testes-automatizados)
4. [Testes manuais de produtos](#4-produtos)
5. [Testes manuais de pedidos (webhooks)](#5-pedidos-webhooks)
6. [Consultas de pedidos](#6-consultas-de-pedidos)
7. [Rate limit](#7-rate-limit)
8. [Concorrência e pedidos repetidos](#8-concorrência-e-pedidos-repetidos)
9. [Erros genéricos](#9-erros-genéricos)
10. [Voltar o banco ao estado inicial](#10-voltar-o-banco-ao-estado-inicial)
11. [Problemas comuns](#11-problemas-comuns)
12. [Histórico: o que cada issue acrescentou](#12-histórico-o-que-cada-issue-acrescentou)

---

## 1. Pré-requisitos

| Ferramenta | Para quê | Obrigatório? |
|---|---|---|
| Git | clonar o repositório | sim |
| Docker Desktop | subir o MySQL (ou tudo) e rodar os testes de integração | sim nas formas A e B e para `mvnw verify` |
| Java 21 (JDK) | rodar a app fora do Docker | só nas formas B e C |
| MySQL 8 instalado no PC | banco local, sem Docker | só na forma C |
| `curl` | chamar os endpoints | sim (já vem no Windows 10+, macOS e Linux) |

Os comandos deste guia estão em **bash**. No Windows, use o **Git Bash** (vem com o Git for Windows). No PowerShell,
`curl` é um apelido para outro comando e os exemplos não funcionam como estão.

```bash
git clone https://github.com/4Diovanni/marketplace-order-hub.git
cd marketplace-order-hub
```

Defina a URL base uma vez por terminal. Todos os exemplos usam essa variável:

```bash
BASE=http://localhost:8080
```

## 2. Como subir a aplicação

Escolha **uma** das três formas.

### Forma A: tudo no Docker (mais simples)

```bash
docker compose up --build
```

Sobe o MySQL e a aplicação. O primeiro build demora alguns minutos. Para parar: `Ctrl+C` e depois
`docker compose down`.

### Forma B: MySQL no Docker, aplicação pelo Maven ou pela IDE

Bom para desenvolver, porque a app reinicia rápido e dá para depurar.

```bash
docker compose up -d mysql
./mvnw spring-boot:run          # Windows (cmd): mvnw.cmd spring-boot:run
```

Na IDE (IntelliJ): rode a classe `OrderHubApplication`. Os valores padrão do `application.yml` já apontam para o
MySQL do Compose (`localhost:3306`, banco/usuário/senha `orderhub`).

### Forma C: MySQL instalado no PC (sem Docker)

1. Crie o banco e o usuário, como root no MySQL Workbench ou no `mysql`:

   ```sql
   CREATE DATABASE orderhub;
   CREATE USER 'orderhub'@'localhost' IDENTIFIED BY 'orderhub';
   GRANT ALL PRIVILEGES ON orderhub.* TO 'orderhub'@'localhost';
   ```

2. Se usar outro banco, usuário, senha ou porta, passe por variável de ambiente:

   ```bash
   export DB_URL=jdbc:mysql://localhost:3306/orderhub
   export DB_USERNAME=orderhub
   export DB_PASSWORD=orderhub
   ./mvnw spring-boot:run
   ```

   No IntelliJ, as mesmas variáveis vão em *Run/Debug Configurations → Environment variables*.

3. O Flyway cria as tabelas e os produtos de exemplo na primeira subida.

> **Atenção:** o MySQL instalado no PC e o do Compose usam a mesma porta (3306). Use um ou outro. Se os dois
> estiverem ligados, a app conecta no que pegou a porta primeiro.

### Conferir que subiu

```bash
curl -s $BASE/actuator/health
```

Esperado: `{"status":"UP"}`. A documentação interativa (Swagger) fica em http://localhost:8080/swagger-ui.html.

## 3. Testes automatizados

```bash
./mvnw verify                   # Windows (cmd): mvnw.cmd verify
```

Precisa do Docker ligado: os testes de integração sobem um MySQL próprio com Testcontainers, sem mexer no seu banco.
Esperado: `BUILD SUCCESS` e `Tests run: ..., Failures: 0, Errors: 0`.

O teste que resume o projeto é o `OrderFlowIntegrationTest`: cadastra um produto, recebe um pedido da AlfaShop,
recebe o mesmo pedido de novo (não duplica nem baixa estoque) e, sem estoque, deixa o próximo pedido pendente. Para
rodar só ele:

```bash
./mvnw verify -Dtest=OrderFlowIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false
```

A mesma sequência, na mão, são os testes 4.4, 5.1, 5.2 e 5.4 deste roteiro.

Só os testes unitários, sem Docker:

```bash
./mvnw test '-Dtest=!*IntegrationTest,!OrderHubApplicationTests' -Dsurefire.failIfNoSpecifiedTests=false
```

---

Daqui em diante, cada teste traz o comando, o status esperado e o trecho da resposta que importa. O
`-w "\nHTTP %{http_code}\n"` imprime o status no final. Os testes das seções 4 a 6 partem do banco recém-criado
(produtos do seed); veja a [seção 10](#10-voltar-o-banco-ao-estado-inicial) para zerar.

Produtos do seed usados nos testes: `CAM-001` (50 unidades), `FON-001` (60), `GAR-001` (30), `LIV-001` (80) e
`BON-001` (**0**, para testar falta de estoque).

## 4. Produtos

### 4.1 Listar produtos (paginado)

```bash
curl -s -w "\nHTTP %{http_code}\n" "$BASE/api/products?page=0&size=3"
```

Esperado: `200`, com `content` (3 produtos em ordem de SKU) e `page`:

```json
{"content":[{"sku":"BON-001","name":"Boné aba curva","price":69.90,"stockQuantity":0,...}, ...],
 "page":{"size":3,"number":0,"totalElements":10,"totalPages":4}}
```

### 4.2 Detalhe de um produto

```bash
curl -s -w "\nHTTP %{http_code}\n" $BASE/api/products/CAM-001
```

Esperado: `200`, `"sku":"CAM-001"`, `"stockQuantity":50`.

### 4.3 Produto inexistente

```bash
curl -s -w "\nHTTP %{http_code}\n" $BASE/api/products/XXX-999
```

Esperado: `404`, `"code":"PRODUCT_NOT_FOUND"`, `"message":"Não encontramos o produto com SKU XXX-999 no seu catálogo."`

### 4.4 Criar produto

```bash
curl -s -i -X POST $BASE/api/products -H "Content-Type: application/json" \
  -d '{"sku":"MEI-001","name":"Meia esportiva","price":19.90,"stockQuantity":100}'
```

Esperado: `201`, header `Location: http://localhost:8080/api/products/MEI-001` e o produto no corpo.

### 4.5 Criar com SKU que já existe

Rode o mesmo comando do 4.4 de novo.

Esperado: `409`, `"code":"DUPLICATE_SKU"`, `"message":"Já existe um produto com o SKU MEI-001 no seu catálogo."`

### 4.6 Criar com campos inválidos

```bash
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/api/products -H "Content-Type: application/json" \
  -d '{"sku":"meia 1","name":"","price":-1,"stockQuantity":-5}'
```

Esperado: `400`, `"code":"VALIDATION_ERROR"` e a lista `errors` com um item por campo (`sku`, `name`, `price`,
`stockQuantity`).

### 4.7 Atualizar nome e preço

```bash
curl -s -w "\nHTTP %{http_code}\n" -X PUT $BASE/api/products/MEI-001 -H "Content-Type: application/json" \
  -d '{"name":"Meia esportiva cano alto","price":24.90}'
```

Esperado: `200`, nome e preço novos, `stockQuantity` sem mudança (100). O SKU e o estoque não mudam por esta rota.

### 4.8 Ajustar estoque

```bash
curl -s -w "\nHTTP %{http_code}\n" -X PATCH $BASE/api/products/MEI-001/stock -H "Content-Type: application/json" \
  -d '{"delta":-3}'
```

Esperado: `200`, `"stockQuantity":97`. `delta` positivo repõe, negativo retira.

### 4.9 Ajuste que deixaria o estoque negativo

```bash
curl -s -w "\nHTTP %{http_code}\n" -X PATCH $BASE/api/products/BON-001/stock -H "Content-Type: application/json" \
  -d '{"delta":-1}'
```

Esperado: `422`, `"code":"INSUFFICIENT_STOCK"`, `"message":"O produto BON-001 tem só 0 unidade(s) em estoque."`.
O estoque continua 0.

### 4.10 Remover produto

```bash
curl -s -w "\nHTTP %{http_code}\n" -X DELETE $BASE/api/products/MEI-001
```

Esperado: `204`, sem corpo. Rodando de novo: `404` com `"code":"PRODUCT_NOT_FOUND"`.

## 5. Pedidos (webhooks)

Os dois marketplaces são fictícios e mandam o mesmo tipo de pedido em formatos diferentes. Contrato de cada um em
[`marketplaces.md`](marketplaces.md).

### 5.1 Pedido da AlfaShop com estoque

```bash
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/api/webhooks/alfashop/orders \
  -H "Content-Type: application/json" -d @docs/exemplos/alfashop-pedido.json
```

Esperado: `201`, header `Location` com `/api/orders/{id}` e:

```json
{"code":"ORDER_CONFIRMED","message":"Pedido AS-10001 da AlfaShop confirmado. O estoque já foi reservado.",
 "order":{"id":1,"channel":"ALFASHOP","externalOrderId":"AS-10001","status":"CONFIRMED","totalAmount":169.70,...}}
```

Confira o estoque: `curl -s $BASE/api/products/CAM-001` → `"stockQuantity":48` (eram 50, o pedido levou 2).

### 5.2 Mesmo pedido enviado de novo (idempotência, issue #11)

Rode o comando do 5.1 outra vez, simulando o retry do marketplace.

Esperado: `200` (não `201`), **o mesmo `id`** do 5.1, sem header `Location`, e:

```json
{"code":"ORDER_ALREADY_RECEIVED","message":"Pedido AS-10001 da AlfaShop já tinha sido recebido. Nada foi alterado.",
 "order":{"id":1,...,"status":"CONFIRMED",...}}
```

Confira que o estoque **não** mudou: `CAM-001` continua com 48.

### 5.3 Pedido da BetaMarket (centavos e nome em dois campos)

```bash
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/api/webhooks/betamarket/orders \
  -H "Content-Type: application/json" -d @docs/exemplos/betamarket-pedido.json
```

Esperado: `201`, `"code":"ORDER_CONFIRMED"`, `"customerName":"Cliente Teste"` (juntou `first_name` + `last_name`) e
`"unitPrice":74.90` para o `GAR-001` (veio como `7490` centavos).

### 5.4 Pedido com um item sem estoque (tudo ou nada)

```bash
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/api/webhooks/betamarket/orders -H "Content-Type: application/json" \
  -d '{"order":{"external_id":"BM-779","customer":{"first_name":"Cliente"},"lines":[
        {"product_code":"CAM-001","amount":1,"unit_price_cents":5990},
        {"product_code":"BON-001","amount":1,"unit_price_cents":6990}]}}'
```

Esperado: `201`, `"code":"ORDER_PENDING_STOCK"`, `"status":"PENDING_STOCK"`. O `CAM-001` tinha estoque, mas como o
`BON-001` não tem, **nada** é baixado: confira que `CAM-001` não mudou.

### 5.5 Canal desconhecido

```bash
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/api/webhooks/gamashop/orders \
  -H "Content-Type: application/json" -d '{}'
```

Esperado: `404`, `"code":"CHANNEL_NOT_FOUND"`, `"message":"O canal \"gamashop\" não está integrado à sua conta."`

### 5.6 Payload fora do contrato do canal

```bash
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/api/webhooks/alfashop/orders \
  -H "Content-Type: application/json" -d '{"orderId":"AS-10002","items":[]}'
```

Esperado: `400`, `"code":"INVALID_MARKETPLACE_PAYLOAD"` e `errors` apontando `buyer` e `items`.

### 5.7 SKU que não existe no catálogo

```bash
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/api/webhooks/alfashop/orders -H "Content-Type: application/json" \
  -d '{"orderId":"AS-10003","buyer":{"fullName":"Cliente"},"items":[{"sku":"XYZ-999","qty":1,"price":10.00}]}'
```

Esperado: `422`, `"code":"UNKNOWN_SKU"`, mensagem citando `XYZ-999`. Nenhum pedido é gravado.

### 5.8 Pedido acima do limite de itens do canal

A AlfaShop aceita até 20 itens por pedido. Este comando monta um pedido com 21:

```bash
ITEMS=$(for i in $(seq 1 21); do printf '{"sku":"CAM-001","qty":1,"price":59.90},'; done)
curl -s -w "\nHTTP %{http_code}\n" -X POST $BASE/api/webhooks/alfashop/orders -H "Content-Type: application/json" \
  -d "{\"orderId\":\"AS-10004\",\"buyer\":{\"fullName\":\"Cliente\"},\"items\":[${ITEMS%,}]}"
```

Esperado: `400`, `"code":"INVALID_MARKETPLACE_PAYLOAD"`, com `items` na lista `errors`.

## 6. Consultas de pedidos

### 6.1 Listar com filtros

```bash
curl -s -w "\nHTTP %{http_code}\n" "$BASE/api/orders?status=PENDING_STOCK&channel=BETAMARKET"
```

Esperado: `200` e só pedidos pendentes da BetaMarket (o `BM-779` do 5.4). Os filtros são opcionais; sem eles, lista
tudo, do mais recente para o mais antigo. Aceita `page` e `size`.

### 6.2 Detalhe com itens

```bash
curl -s -w "\nHTTP %{http_code}\n" $BASE/api/orders/1
```

Esperado: `200`, o pedido `AS-10001` com a lista `items`.

### 6.3 Pedido inexistente

```bash
curl -s -w "\nHTTP %{http_code}\n" $BASE/api/orders/999999
```

Esperado: `404`, `"code":"ORDER_NOT_FOUND"`, `"message":"Não encontramos o pedido 999999."`

## 7. Rate limit

Cada canal tem um limite por minuto: AlfaShop 60, BetaMarket 30 (em `application.yml`). Este comando manda 35
pedidos da BetaMarket em sequência e conta os status:

```bash
for i in $(seq 1 35); do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST $BASE/api/webhooks/betamarket/orders \
    -H "Content-Type: application/json" \
    -d '{"order":{"external_id":"BM-RL","customer":{"first_name":"Cliente"},"lines":[{"product_code":"LIV-001","amount":1,"unit_price_cents":3490}]}}'
done | sort | uniq -c
```

Esperado: as primeiras 30 requisições do minuto passam (uma `201` e as outras `200`, porque é sempre o mesmo pedido)
e o restante recebe `429`. Para ver o header:

```bash
curl -s -i -X POST $BASE/api/webhooks/betamarket/orders -H "Content-Type: application/json" -d '{}'
```

Esperado: `429`, header `Retry-After` com os segundos que faltam, `"code":"RATE_LIMIT_EXCEEDED"` e
`"retryAfterSeconds"` no corpo. Depois desse tempo, o canal volta a aceitar. A AlfaShop não é afetada: o limite é
por canal.

## 8. Concorrência e pedidos repetidos

Testes da issue #11. O `&` dispara as requisições em paralelo.

### 8.1 Vários pedidos disputando a última unidade

```bash
curl -s -o /dev/null -X POST $BASE/api/products -H "Content-Type: application/json" \
  -d '{"sku":"ULT-001","name":"Ultima unidade","price":10.00,"stockQuantity":1}'

for i in 1 2 3 4 5; do
  curl -s -o /dev/null -w "%{http_code} " -X POST $BASE/api/webhooks/alfashop/orders \
    -H "Content-Type: application/json" \
    -d "{\"orderId\":\"AS-ULT-$i\",\"buyer\":{\"fullName\":\"Cliente\"},\"items\":[{\"sku\":\"ULT-001\",\"qty\":1,\"price\":10.00}]}" &
done; wait; echo

curl -s "$BASE/api/orders?size=50" | grep -oE '"externalOrderId":"AS-ULT-[0-9]","status":"[A-Z_]+"'
curl -s $BASE/api/products/ULT-001
```

Esperado: cinco `201`, **só um** pedido `CONFIRMED`, quatro `PENDING_STOCK` e `"stockQuantity":0` (nunca negativo).
No log da app costuma aparecer `Estoque alterado por outro pedido ao mesmo tempo, tentando de novo`: é o lock otimista
(`@Version`) segurando a corrida e o retry relendo o estoque.

### 8.2 O mesmo pedido chegando várias vezes ao mesmo tempo

```bash
for i in 1 2 3 4 5; do
  curl -s -o /dev/null -w "%{http_code} " -X POST $BASE/api/webhooks/alfashop/orders \
    -H "Content-Type: application/json" \
    -d '{"orderId":"AS-DUP","buyer":{"fullName":"Cliente"},"items":[{"sku":"LIV-001","qty":1,"price":34.90}]}' &
done; wait; echo
```

Esperado: um `201` e quatro `200` (a ordem varia). O `LIV-001` cai só 1 unidade. No log, o Hibernate registra
`Duplicate entry 'ALFASHOP-AS-DUP'` como `ERROR`: é a unique constraint pegando a corrida, e o hub responde com o
pedido original (`Pedido duplicado ignorado`). Não é falha.

## 9. Erros genéricos

| Teste | Comando | Esperado |
|---|---|---|
| JSON malformado | `curl -s -X POST $BASE/api/products -H "Content-Type: application/json" -d '{"sku":'` | `400`, `MALFORMED_REQUEST` |
| Rota inexistente | `curl -s $BASE/api/nao-existe` | `404`, `RESOURCE_NOT_FOUND` |
| Método não suportado | `curl -s -X DELETE $BASE/api/orders/1` | `405`, `REQUEST_NOT_SUPPORTED` |

Todo erro segue o mesmo formato (RFC 7807): `code` para o front decidir o que fazer, `message` para o seller e
`detail` técnico. Nenhum erro devolve stack trace.

## 10. Voltar o banco ao estado inicial

- **Formas A e B (Docker):** `docker compose down -v` apaga o volume do MySQL. Na próxima subida, o Flyway recria
  tudo com o seed.
- **Forma C (MySQL local):** com a app parada, rode `DROP DATABASE orderhub; CREATE DATABASE orderhub;` e suba a app
  de novo.

O rate limit fica em memória: reiniciar a app zera os contadores.

## 11. Problemas comuns

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| `Communications link failure` ao subir a app | MySQL ainda não subiu ou está em outra porta | `docker compose ps` deve mostrar o mysql `healthy`; na forma C, confira se o serviço MySQL está rodando |
| `Access denied for user 'orderhub'` | usuário ou senha diferentes | crie o usuário (forma C, passo 1) ou ajuste `DB_USERNAME` / `DB_PASSWORD` |
| `Port 3306 is already in use` no `docker compose up` | MySQL do PC ligado | pare o serviço MySQL do Windows ou use a forma C |
| `Port 8080 is already in use` | outra app na porta | pare a outra app ou suba com `--server.port=8081` e ajuste `BASE` |
| `Validate failed: Migrations have failed validation` | uma migration já aplicada foi alterada | zere o banco ([seção 10](#10-voltar-o-banco-ao-estado-inicial)) |
| Testes de integração falham com `Could not find a valid Docker environment` | Docker parado | ligue o Docker Desktop e rode de novo |
| Exemplos com `curl` dão erro no PowerShell | `curl` é outro comando lá | use o Git Bash |

## 12. Histórico: o que cada issue acrescentou

Ao fechar uma issue, acrescente aqui os testes novos e uma linha nesta tabela.

| Issue | O que entrou neste roteiro |
|---|---|
| #1 a #7 | Setup, Docker, CRUD de produtos, ajuste de estoque, formato de erro (seções 2, 3, 4, 9) |
| #8 a #10 | Webhooks dos dois marketplaces, regra tudo ou nada, consultas de pedidos (seções 5.1, 5.3 a 5.7, 6) |
| #23 a #25 | Mensagens para o seller, rate limit e limite de itens por canal (seções 5.8, 7) |
| #11 | Pedido repetido devolve `200` com o original; concorrência no estoque e na chegada do mesmo pedido (seções 5.2, 8) |
| #12 | Teste automatizado do fluxo completo (`OrderFlowIntegrationTest`, seção 3). Nenhum comportamento novo |
