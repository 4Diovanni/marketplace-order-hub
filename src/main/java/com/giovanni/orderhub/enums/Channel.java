package com.giovanni.orderhub.enums;

import com.giovanni.orderhub.exception.integration.UnknownChannelException;

/**
 * Fictional marketplaces integrated with the hub.
 * The {@code slug} is the part used in the webhook URL; the {@code displayName} is the name shown to the seller.
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

    /** @throws UnknownChannelException if no channel uses this slug. */
    public static Channel fromSlug(String slug) {
        for (Channel channel : values()) {
            if (channel.slug.equalsIgnoreCase(slug)) {
                return channel;
            }
        }
        throw new UnknownChannelException(slug);
    }
}
