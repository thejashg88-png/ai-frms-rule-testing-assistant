package com.thejas.ai_frms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
    }

    @Bean(name = "frmsWebClient")
    public WebClient frmsWebClient(
            WebClient.Builder webClientBuilder,
            @Value("${app.integration.frms.base-url:http://localhost:8083}") String frmsBaseUrl
    ) {
        return webClientBuilder
                .clone()
                .baseUrl(frmsBaseUrl)
                .build();
    }

    @Bean(name = "aiWebClient")
    public WebClient aiWebClient(
            WebClient.Builder webClientBuilder,
            @Value("${app.integration.ai.base-url:http://localhost:8000}") String aiBaseUrl
    ) {
        return webClientBuilder
                .clone()
                .baseUrl(aiBaseUrl)
                .build();
    }
}