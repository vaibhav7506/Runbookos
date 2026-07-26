package com.vaibhav.runbookos.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  @Bean
  OpenAPI runbookOsOpenApi() {
    return new OpenAPI()
        .info(
            new io.swagger.v3.oas.models.info.Info()
                .title("RunbookOS Control Plane")
                .version("0.4.0"))
        .servers(
            List.of(new Server().url("http://localhost:8080").description("Local development")));
  }

  @Bean
  OpenApiCustomizer structuredErrorSchema() {
    var validationError =
        new ObjectSchema()
            .addProperty("field", new StringSchema())
            .addProperty("message", new StringSchema())
            .addProperty("rejectedValue", new ObjectSchema().nullable(true));
    var errorSchema =
        new ObjectSchema()
            .addProperty("code", new StringSchema())
            .addProperty("message", new StringSchema())
            .addProperty("status", new IntegerSchema().format("int32"))
            .addProperty("correlationId", new StringSchema())
            .addProperty(
                "validationErrors", new ArraySchema().items(validationError).nullable(false));
    return openApi -> {
      if (openApi.getComponents() == null) {
        openApi.setComponents(new Components());
      }
      openApi.getComponents().addSchemas("ErrorResponse", errorSchema);
    };
  }
}
