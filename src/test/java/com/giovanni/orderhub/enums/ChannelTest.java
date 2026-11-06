package com.giovanni.orderhub.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.giovanni.orderhub.exception.integration.UnknownChannelException;
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
