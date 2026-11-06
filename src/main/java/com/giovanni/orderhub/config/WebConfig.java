package com.giovanni.orderhub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/** Serializes {@code Page} as a stable DTO ({@code content} + {@code page}) instead of the {@code PageImpl} format. */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfig {
}
