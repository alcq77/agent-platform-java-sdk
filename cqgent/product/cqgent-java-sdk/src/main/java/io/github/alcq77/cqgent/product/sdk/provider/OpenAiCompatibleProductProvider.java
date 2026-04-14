package io.github.alcq77.cqgent.product.sdk.provider;

import io.github.alcq77.cqgent.common.api.ApiResponse;
import io.github.alcq77.cqgent.model.api.dto.ChatCompletionRequest;
import io.github.alcq77.cqgent.model.api.dto.ChatCompletionResponse;
import io.github.alcq77.cqgent.product.spi.model.ProductEndpointConfig;
import io.github.alcq77.cqgent.product.spi.model.ProductModelProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

public class OpenAiCompatibleProductProvider implements ProductModelProvider {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String providerCode() {
        return "openai_compat";
    }

    @Override
    public ChatCompletionResponse complete(ProductEndpointConfig endpoint, ChatCompletionRequest request) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(endpoint.getConnectTimeout())
                    .build();
            String root = endpoint.getBaseUrl().endsWith("/")
                    ? endpoint.getBaseUrl().substring(0, endpoint.getBaseUrl().length() - 1)
                    : endpoint.getBaseUrl();
            Map<String, Object> body = objectMapper.convertValue(request, new TypeReference<>() {
            });
            if (endpoint.getDefaultModel() != null && !endpoint.getDefaultModel().isBlank()) {
                body.put("model", endpoint.getDefaultModel());
            }
            String reqBody = objectMapper.writeValueAsString(body);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(root + "/v1/chat/completions"))
                    .timeout(endpoint.getReadTimeout())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(reqBody));
            if (endpoint.getApiKey() != null && !endpoint.getApiKey().isBlank()) {
                builder.header("Authorization", "Bearer " + endpoint.getApiKey());
            }
            for (Map.Entry<String, String> h : endpoint.getHeaders().entrySet()) {
                builder.header(h.getKey(), h.getValue());
            }
            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException("upstream http " + response.statusCode() + ": " + response.body());
            }
            String bodyText = response.body();
            if (bodyText.contains("\"code\"") && bodyText.contains("\"data\"")) {
                ApiResponse<ChatCompletionResponse> wrapped = objectMapper.readValue(bodyText, new TypeReference<>() {
                });
                if (wrapped.getCode() != 0 || wrapped.getData() == null) {
                    throw new IllegalStateException("model service response error: " + wrapped.getMessage());
                }
                return wrapped.getData();
            }
            return objectMapper.readValue(bodyText, ChatCompletionResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("openai compatible invoke failed: " + e.getMessage(), e);
        }
    }
}
