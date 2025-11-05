package com.crewmeister.cmcodingchallenge.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI exchangeRateOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Crewmeister Java Coding Challenge")
                        .description(("REST APIs for EUR- based foreign exchange rates using data from the Deutsche Bundesbank"))
                        .version("1.0")
                        .contact(new Contact()
                                .name("Iheb Bellalouna")
                                .email("bellalouna.iheb@gmail.com")
                        )
                );
    }
}
