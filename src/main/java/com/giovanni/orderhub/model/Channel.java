package com.giovanni.orderhub.model;

import com.giovanni.orderhub.exception.UnknownChannelException;
import java.util.Arrays;

/** Marketplaces fictícios integrados ao hub. O {@code slug} é o trecho usado na URL do webhook. */
public enum Channel {

    ALFASHOP("alfashop"),
    BETAMARKET("betamarket");

    private final String slug;

    Channel(String slug) {
        this.slug = slug;
    }

    public String getSlug() {
        return slug;
    }

    /** @throws UnknownChannelException se nenhum canal usa esse slug. */
    public static Channel fromSlug(String slug) {
        return Arrays.stream(values())
                .filter(channel -> channel.slug.equalsIgnoreCase(slug))
                .findFirst()
                .orElseThrow(() -> new UnknownChannelException(slug));
    }
}
