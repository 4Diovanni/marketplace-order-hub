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
