package com.giovanni.orderhub.integration.betamarket;

import static com.giovanni.orderhub.integration.MapperTestSupport.example;
import static com.giovanni.orderhub.integration.MapperTestSupport.json;
import static com.giovanni.orderhub.integration.MapperTestSupport.payloadReader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.giovanni.orderhub.enums.Channel;
import com.giovanni.orderhub.exception.integration.InvalidPayloadException;
import com.giovanni.orderhub.model.IncomingOrder;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BetaMarketMapperTest {

    private final BetaMarketMapper mapper = new BetaMarketMapper(payloadReader());

    @Test
    void shouldMapDocumentedExampleConvertingCentsAndJoiningName() {
        IncomingOrder order = mapper.toIncomingOrder(example("betamarket-pedido.json"));

        assertThat(order.channel()).isEqualTo(Channel.BETAMARKET);
        assertThat(order.externalOrderId()).isEqualTo("BM-778");
        assertThat(order.customerName()).isEqualTo("Cliente Teste");
        assertThat(order.items()).containsExactly(
                new IncomingOrder.Item("CAM-001", 1, new BigDecimal("59.90")),
                new IncomingOrder.Item("GAR-001", 2, new BigDecimal("74.90")));
    }

    @Test
    void shouldConvertCentsToReaisWithTwoDecimals() {
        assertThat(BetaMarketMapper.centsToReais(5990)).isEqualByComparingTo("59.90");
        assertThat(BetaMarketMapper.centsToReais(5)).isEqualTo(new BigDecimal("0.05"));
        assertThat(BetaMarketMapper.centsToReais(10000)).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    void shouldAcceptCustomerWithoutLastName() {
        IncomingOrder order = mapper.toIncomingOrder(json("""
                {"order":{"external_id":"BM-1","customer":{"first_name":"Ana"},
                 "lines":[{"product_code":"CAM-001","amount":1,"unit_price_cents":1000}]}}
                """));

        assertThat(order.customerName()).isEqualTo("Ana");
    }

    @Test
    void shouldRejectPayloadInAlfaShopFormat() {
        assertThatThrownBy(() -> mapper.toIncomingOrder(example("alfashop-pedido.json")))
                .isInstanceOfSatisfying(InvalidPayloadException.class, ex ->
                        assertThat(ex.getViolations()).extracting("field").containsExactly("order"));
    }
}
