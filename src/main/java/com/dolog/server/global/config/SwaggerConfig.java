package com.dolog.server.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))
                .addSecurityItem(securityRequirement)
                .tags(List.of(
                        new Tag().name("account"),
                        new Tag().name("exhibition"),
                        new Tag().name("exhibition-zone"),
                        new Tag().name("exhibition-host"),
                        new Tag().name("exhibition-partner"),
                        new Tag().name("exhibition-map"),
                        new Tag().name("exhibition-guide-map"),
                        new Tag().name("exhibition-custom-theme"),
                        new Tag().name("exhibition-custom-splash"),
                        new Tag().name("exhibition-banner"),
                        new Tag().name("artwork"),
                        new Tag().name("bts"),
                        new Tag().name("artist-기본 정보"),
                        new Tag().name("artist-전시참여작가")
                ));
    }
}
