package com.codify.universaltracker.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String HEADER_SCHEME = "X-User-Id";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Universal Tracker API")
                        .description("""
                                Metadata-driven personal tracking platform.

                                **Authentication:** Pass your user UUID in the `X-User-Id` header.
                                Click **Authorize** above, enter a valid UUID (e.g. `a1000000-0000-0000-0000-000000000001`),
                                and it will be sent with every request automatically.
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("Codify")))
                // Declare the header as an API-key-style security scheme
                .components(new Components()
                        .addSecuritySchemes(HEADER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(HEADER_SCHEME)
                                .description("Your user UUID — e.g. a1000000-0000-0000-0000-000000000001")))
                // Apply it globally so every endpoint requires it
                .addSecurityItem(new SecurityRequirement().addList(HEADER_SCHEME));
    }
}
