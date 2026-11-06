package com.giovanni.orderhub.enums;

import com.giovanni.orderhub.exception.integration.UnknownChannelException;

/**
 * Marketplaces fictícios integrados ao hub.
 * O {@code slug} é o trecho usado na URL do webhook; o {@code displayName} é o nome mostrado para o seller.
 */
public enum Channel {

    ALFASHOP("alfashop", "AlfaShop"),
    BETAMARKET("betamarket", "BetaMarket");

    private final String slug;
    private final String displayName;

    Channel(String slug, String displayName) {
        this.slug = slug;
        this.displayName = displayName;
    }

    public String getSlug() {
        return slug;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** @throws UnknownChannelException se nenhum canal usa esse slug. */
    public static Channel fromSlug(String slug) {
        for (Channel channel : values()) {
            if (channel.slug.equalsIgnoreCase(slug)) {
                return channel;
            }
        }
        throw new UnknownChannelException(slug);
    }
}
