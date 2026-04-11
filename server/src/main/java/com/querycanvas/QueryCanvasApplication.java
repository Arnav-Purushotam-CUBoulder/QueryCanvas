package com.querycanvas;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
    info = @Info(
        title = "QueryCanvas API",
        version = "1.0",
        description = "Natural-language SQL analytics service built with Spring Boot."
    )
)
public class QueryCanvasApplication {
    public static void main(String[] args) {
        SpringApplication.run(QueryCanvasApplication.class, args);
    }
}
