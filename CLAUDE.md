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
- Pacotes por feature em `com.giovanni.orderhub`: `product`, `order`, `integration`, `shared`.
- Schema só via migrations Flyway (`ddl-auto: validate`).

## Regra fixa

Projeto de portfólio com domínio inspirado em integração de marketplaces. **Nenhum código, payload, regra ou dado de empresa real** (Magis5, Allcare ou outra). Marketplaces, formatos de JSON e dados são inventados.
