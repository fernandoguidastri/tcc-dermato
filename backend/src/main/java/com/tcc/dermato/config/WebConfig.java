package com.tcc.dermato.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Permite acessar as imagens enviadas (ex: /uploads/xxx.jpg) diretamente pelo navegador,
    // sem exigir autenticacao - necessario para exibir a foto no preview do resultado da IA.
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
