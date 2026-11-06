package com.giovanni.orderhub.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/** Serializa {@code Page} como DTO estável ({@code content} + {@code page}), em vez do formato interno do {@code PageImpl}. */
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfig {
}
