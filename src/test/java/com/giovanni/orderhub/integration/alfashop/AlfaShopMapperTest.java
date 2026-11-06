package com.giovanni.orderhub.integration.alfashop;

import static com.giovanni.orderhub.integration.MapperTestSupport.example;
import static com.giovanni.orderhub.integration.MapperTestSupport.json;
import static com.giovanni.orderhub.integration.MapperTestSupport.payloadReader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.giovanni.orderhub.exception.InvalidRequestException;
import com.giovanni.orderhub.model.Channel;
import com.giovanni.orderhub.model.IncomingOrder;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class AlfaShopMapperTest {

    private final AlfaShopMapper mapper = new AlfaShopMapper(payloadReader());

    @Test
    void shouldMapDocumentedExampleToIncomingOrder() {
        IncomingOrder order = mapper.toIncomingOrder(example("alfashop-pedido.json"));

        assertThat(order.channel()).isEqualTo(Channel.ALFASHOP);
        assertThat(order.externalOrderId()).isEqualTo("AS-10001");
        assertThat(order.customerName()).isEqualTo("Cliente Teste");
        assertThat(order.items()).containsExactly(
                new IncomingOrder.Item("CAM-001", 2, new BigDecimal("59.90")),
                new IncomingOrder.Item("FON-001", 1, new BigDecimal("49.90")));
    }

    @Test
    void shouldIgnoreFieldsTheHubDoesNotUse() {
        IncomingOrder order = mapper.toIncomingOrder(json("""
                {"orderId":"AS-1","campanha":"black-friday","buyer":{"fullName":"Ana"},
                 "items":[{"sku":"CAM-001","qty":1,"price":10.00,"cor":"preta"}]}
                """));

        assertThat(order.externalOrderId()).isEqualTo("AS-1");
    }

    @Test
    void shouldRejectPayloadWithoutItemsAndListTheInvalidFields() {
        assertThatThrownBy(() -> mapper.toIncomingOrder(json("""
                {"orderId":"","buyer":{"fullName":"Ana"},"items":[]}
                """)))
                .isInstanceOfSatisfying(InvalidRequestException.class, ex ->
                        assertThat(ex.getViolations()).extracting("field").containsExactly("items", "orderId"));
    }

    @Test
    void shouldRejectPriceWithMoreThanTwoDecimals() {
        assertThatThrownBy(() -> mapper.toIncomingOrder(json("""
                {"orderId":"AS-3","buyer":{"fullName":"Ana"},"items":[{"sku":"CAM-001","qty":1,"price":10.999}]}
                """)))
                .isInstanceOfSatisfying(InvalidRequestException.class, ex ->
                        assertThat(ex.getViolations()).extracting("field").containsExactly("items[0].price"));
    }

    @Test
    void shouldRejectNonPositiveQuantity() {
        assertThatThrownBy(() -> mapper.toIncomingOrder(json("""
                {"orderId":"AS-2","buyer":{"fullName":"Ana"},"items":[{"sku":"CAM-001","qty":0,"price":10.00}]}
                """)))
                .isInstanceOfSatisfying(InvalidRequestException.class, ex ->
                        assertThat(ex.getViolations()).extracting("field").containsExactly("items[0].qty"));
    }
}
