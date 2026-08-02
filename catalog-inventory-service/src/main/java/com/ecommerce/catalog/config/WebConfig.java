package com.ecommerce.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.SortHandlerMethodArgumentResolverCustomizer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // This doesn't fully solve Swagger UI's array-input rendering for sort, but it does make the expected format explicit (comma-separated property,direction) — the more complete fix for the confusing array UI itself is to also add explicit @Parameter annotations on Pageable in each controller method to override how springdoc documents it, which is a larger change we are deferring.
    @Bean
    public SortHandlerMethodArgumentResolverCustomizer sortCustomizer() {
        return resolver -> resolver.setPropertyDelimiter(",");
    }
}
