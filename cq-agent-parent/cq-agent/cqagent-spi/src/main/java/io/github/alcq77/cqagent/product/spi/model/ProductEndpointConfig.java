package io.github.alcq77.cqagent.product.spi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEndpointConfig {

    private String id;

    private String provider;

    private String baseUrl;

    private String apiKey;

    private String defaultModel;

    @Builder.Default
    private Map<String, String> headers = new HashMap<>();

    @Builder.Default
    private Duration connectTimeout = Duration.ofSeconds(10);

    @Builder.Default
    private Duration readTimeout = Duration.ofSeconds(120);
}
