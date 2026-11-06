# marketplace-order-hub

[![CI](https://github.com/4Diovanni/marketplace-order-hub/actions/workflows/ci.yml/badge.svg)](https://github.com/4Diovanni/marketplace-order-hub/actions/workflows/ci.yml)

API REST que recebe pedidos de marketplaces com formatos diferentes, converte tudo para um modelo único, controla o estoque e garante que o mesmo pedido não seja processado duas vezes.

> Projeto de portfólio, em construção. Os marketplaces (`AlfaShop`, `BetaMarket`), os formatos de payload e os dados são fictícios. O domínio é inspirado na minha experiência com integração de marketplaces; nenhum código ou regra de empresa foi usado.

## Stack

Java 21 · Spring Boot 3 · Spring Data JPA · MySQL 8 · Flyway · Bean Validation · JUnit 5 · Testcontainers · Docker Compose · springdoc-openapi (Swagger)

## Como rodar

Pré-requisito: Docker.

```bash
git clone https://github.com/4Diovanni/marketplace-order-hub.git
cd marketplace-order-hub
docker compose up --build
```

- Health: http://localhost:8080/actuator/health
- Swagger: http://localhost:8080/swagger-ui.html

Rodar os testes (precisa de Docker rodando, por causa do Testcontainers):

```bash
./mvnw verify
```

Rodar a app fora do Docker, usando só o banco do Compose:

```bash
docker compose up -d mysql
./mvnw spring-boot:run
```

## Endpoints

Produtos (o banco já sobe com 10 produtos fictícios, ex.: `CAM-001`):

| Método | Rota | Descrição | Respostas |
|---|---|---|---|
| `POST` | `/api/products` | Cria produto | `201` · `400` · `409` SKU duplicado |
| `GET` | `/api/products?page=0&size=20&sort=sku` | Lista paginada | `200` |
| `GET` | `/api/products/{sku}` | Detalhe | `200` · `404` |
| `PUT` | `/api/products/{sku}` | Atualiza nome e preço | `200` · `400` · `404` |
| `PATCH` | `/api/products/{sku}/stock` | Ajusta estoque com `{"delta": n}` | `200` · `404` · `422` se ficar negativo |
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

Erros seguem a RFC 7807 (`application/problem+json`):

```json
{
  "type": "about:blank",
  "title": "Regra de negócio violada",
  "status": 422,
  "detail": "Estoque insuficiente para o SKU 'BON-001': disponível 0, alteração pedida -1.",
  "instance": "/api/products/BON-001/stock"
}
```

## Configuração

| Variável | Padrão |
|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/orderhub` |
| `DB_USERNAME` | `orderhub` |
| `DB_PASSWORD` | `orderhub` |

## Roadmap

O plano do MVP está nas [issues](https://github.com/4Diovanni/marketplace-order-hub/issues): CRUD de produtos, webhooks de 2 marketplaces, regra de estoque, idempotência, testes e documentação.

## Autor

Giovanni Moreira · [GitHub](https://github.com/4Diovanni)
