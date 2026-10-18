package io.hexlet.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/** General information shown at the top of Swagger UI (LR5). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bookCatalogOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Book catalog API")
                .version("0.0.1")
                .description("REST API книжного каталога (/api/books) и пользователей (/users). "
                        + "Лабораторная работа №5: Swagger UI + springdoc-openapi. "
                        + "Лабораторная работа №6: заказы пользователей через @FeignClient (Order-Service)."));
    }
}
