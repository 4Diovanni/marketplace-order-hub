package com.giovanni.orderhub.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ChannelTest {

    @Test
    void shouldResolveChannelFromSlugIgnoringCase() {
        assertThat(Channel.fromSlug("alfashop")).isEqualTo(Channel.ALFASHOP);
        assertThat(Channel.fromSlug("BetaMarket")).isEqualTo(Channel.BETAMARKET);
    }

    @Test
    void shouldRejectUnknownSlug() {
        assertThatThrownBy(() -> Channel.fromSlug("gamashop")).isInstanceOf(UnknownChannelException.class);
    }
}
