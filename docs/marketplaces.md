# Contratos dos marketplaces

> **Marketplaces fictícios.** AlfaShop e BetaMarket foram inventados para este projeto. Os formatos, limites e
> comportamentos abaixo imitam o tipo de regra que marketplaces reais têm, sem copiar nenhum deles.

Cada marketplace chama o mesmo endpoint, `POST /api/webhooks/{canal}/orders`, mas cada um tem seu próprio contrato.

## Resumo

| Regra | AlfaShop | BetaMarket |
|---|---|---|
| Slug na URL | `alfashop` | `betamarket` |
| Estrutura do JSON | plana, camelCase | aninhada em `order`, snake_case |
| Valores | reais com até 2 casas (`59.90`) | centavos inteiros (`5990`) |
| Nome do cliente | `buyer.fullName` | `customer.first_name` + `customer.last_name` (sobrenome opcional) |
| Máximo de itens por pedido | 20 (`items`) | 50 (`lines`) |
| Limite de requisições | 60 por minuto | 30 por minuto |
| O que faz ao receber `429` | reenvia depois de 30 s, até 3 vezes | respeita o header `Retry-After` e reenvia |
| O que faz em timeout ou `5xx` | reenvia o mesmo pedido, até 3 vezes | reenvia o mesmo pedido, até 5 vezes |

Os limites de requisição ficam no `application.yml` (`orderhub.rate-limit.requests-per-minute`). Os limites de itens
ficam nos records de payload (`AlfaShopOrderPayload.MAX_ITEMS`, `BetaMarketOrderPayload.MAX_LINES`).

Como os dois reenviam o mesmo pedido em caso de falha, o hub precisa ser idempotente: o mesmo pedido não pode
baixar estoque duas vezes. Hoje a unique constraint `(channel, external_order_id)` impede a duplicação; a issue #11
troca o erro `409` por `200` com o pedido original.

## O que o hub responde

Toda resposta traz um `code` estável (para o front decidir ícone e cor) e uma `message` pronta para o seller.
Nos erros, o `detail` guarda o detalhe técnico para o suporte.

| Situação | Status | `code` | Exemplo de `message` |
|---|---|---|---|
| Pedido confirmado | `201` | `ORDER_CONFIRMED` | Pedido AS-10001 da AlfaShop confirmado. O estoque já foi reservado. |
| Pedido sem estoque | `201` | `ORDER_PENDING_STOCK` | Pedido BM-778 da BetaMarket recebido, mas aguardando estoque. Nenhum item foi reservado ainda. |
| JSON fora do contrato do canal | `400` | `INVALID_MARKETPLACE_PAYLOAD` | O pedido da AlfaShop chegou incompleto ou fora do formato esperado e não foi importado. |
| Canal não integrado | `404` | `CHANNEL_NOT_FOUND` | O canal "gamashop" não está integrado à sua conta. |
| SKU fora do catálogo | `422` | `UNKNOWN_SKU` | O pedido tem produtos que não estão no seu catálogo: XYZ-999. Cadastre os SKUs e peça o reenvio do pedido. |
| Passou do limite | `429` | `RATE_LIMIT_EXCEEDED` | A BetaMarket enviou muitos pedidos em pouco tempo. Eles voltam a ser aceitos em 60 segundo(s). |
| Erro inesperado | `500` | `INTERNAL_ERROR` | Não foi possível concluir agora. Tente de novo em alguns minutos e, se continuar, fale com o suporte. |

O catálogo completo de erros está em `enums/ErrorCode.java`, e as mensagens de sucesso em `enums/OrderResultCode.java`.

## Exemplo de 429

```http
HTTP/1.1 429 Too Many Requests
Retry-After: 60
Content-Type: application/problem+json
```

```json
{
  "type": "about:blank",
  "title": "Limite de requisições atingido",
  "status": 429,
  "detail": "Canal BETAMARKET passou do limite de 30 requisições por minuto.",
  "instance": "/api/webhooks/betamarket/orders",
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "A BetaMarket enviou muitos pedidos em pouco tempo. Eles voltam a ser aceitos em 60 segundo(s).",
  "retryAfterSeconds": 60
}
```

## Limitações conhecidas

- O rate limit usa janela fixa de 1 minuto, em memória. Com várias instâncias da aplicação, o contador teria que ir
  para um lugar compartilhado (ex.: Redis).
- Requisições recusadas por canal desconhecido não contam no limite, porque não dá para saber de qual canal vieram.
