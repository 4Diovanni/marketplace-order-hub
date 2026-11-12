package com.giovanni.orderhub.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** General information shown at the top of the Swagger UI. Each endpoint is described in its controller's interface. */
@Configuration
public class OpenApiConfig {

    private static final String API_VERSION = "1.0.0";
    private static final String REPOSITORY_URL = "https://github.com/4Diovanni/marketplace-order-hub";
    private static final String LICENSE_URL = REPOSITORY_URL + "/blob/main/LICENSE";

    private static final String DESCRIPTION = """
            Hub que recebe pedidos de marketplaces com formatos diferentes, converte para um modelo único, \
            controla o estoque (tudo ou nada) e não deixa o mesmo pedido entrar duas vezes.

            **Marketplaces fictícios:** AlfaShop e BetaMarket foram inventados para este projeto.

            **Formato dos erros:** RFC 7807 (`application/problem+json`) com `code` (estável, para o front decidir \
            o que fazer), `message` (texto pronto para o seller) e `detail` (técnico, para o suporte).
            """;

    @Bean
    public OpenAPI orderHubOpenApi() {
        return new OpenAPI().info(new Info()
                .title("marketplace-order-hub")
                .version(API_VERSION)
                .description(DESCRIPTION)
                .contact(new Contact().name("Giovanni Moreira").url(REPOSITORY_URL))
                .license(new License().name("MIT").url(LICENSE_URL)));
    }
}
