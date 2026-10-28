package com.tcc.dermato;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@SpringBootApplication
public class DermatoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DermatoApplication.class, args);
    }

    // Timeouts generosos: no plano gratuito do Render o microservico de IA "dorme"
    // apos 15 min sem uso e leva cerca de 1 minuto para acordar na primeira chamada.
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(20))
                .setReadTimeout(Duration.ofSeconds(120))
                .build();
    }
}
