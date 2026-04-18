package io.github.alcq77.cqgent.product.core.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import io.github.alcq77.cqgent.product.spi.tool.ProductTool;
import io.github.alcq77.cqgent.product.spi.tool.ProductToolParameterSpec;

import java.util.*;

/**
 * ProductTool 到 LangChain4j ToolSpecification 的适配器。
 */
public class ProductToolRegistry {

    /**
     * 运行时工具索引（key=toolName）。
     */
    private final Map<String, ProductTool> toolMap = new LinkedHashMap<>();
    /**
     * 预构建的 LangChain4j 工具描述，避免每轮重复创建。
     */
    private final List<ToolSpecification> specifications;
    /**
     * JSON 参数解析器。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductToolRegistry(List<ProductTool> tools) {
        if (tools != null) {
            for (ProductTool tool : tools) {
                if (tool != null && tool.name() != null && !tool.name().isBlank()) {
                    toolMap.putIfAbsent(tool.name(), tool);
                }
            }
        }
        this.specifications = toolMap.values().stream()
                .map(ProductToolRegistry::toSpecification)
                .toList();
    }

    public List<ToolSpecification> specifications() {
        return specifications;
    }

    public boolean isEmpty() {
        return toolMap.isEmpty();
    }

    public String execute(ToolExecutionRequest request) {
        ProductTool tool = toolMap.get(request.name());
        if (tool == null) {
            throw new IllegalStateException("tool not found: " + request.name());
        }
        Map<String, Object> arguments = parseArguments(request.arguments());
        validateArguments(tool, arguments);
        return tool.execute(arguments);
    }

    private static ToolSpecification toSpecification(ProductTool tool) {
        JsonObjectSchema.Builder schemaBuilder = JsonObjectSchema.builder();
        List<String> required = new ArrayList<>();
        for (Map.Entry<String, ProductToolParameterSpec> entry : tool.parameterSpecs().entrySet()) {
            String name = entry.getKey();
            ProductToolParameterSpec spec = Objects.requireNonNull(entry.getValue(), "parameter spec must not be null");
            String type = spec.type() == null ? "string" : spec.type().trim().toLowerCase();
            String description = spec.description();
            switch (type) {
                case "integer" -> schemaBuilder.addIntegerProperty(name, description);
                case "number" -> schemaBuilder.addNumberProperty(name, description);
                case "boolean" -> schemaBuilder.addBooleanProperty(name, description);
                default -> schemaBuilder.addStringProperty(name, description);
            }
            if (spec.required()) {
                required.add(name);
            }
        }
        if (!required.isEmpty()) {
            schemaBuilder.required(required);
        }
        JsonObjectSchema parameters = schemaBuilder.additionalProperties(false).build();
        return ToolSpecification.builder()
                .name(tool.name())
            .description(tool.description())
                .parameters(parameters)
                .build();
    }

    private Map<String, Object> parseArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return Map.of();
        }
        String text = arguments.trim();
        if (!text.startsWith("{")) {
            return Map.of("input", stripQuotes(text));
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(text, new TypeReference<>() {
            });
            return parsed == null ? Map.of() : parsed;
        } catch (Exception ex) {
            throw new IllegalStateException("invalid tool arguments json: " + arguments, ex);
        }
    }

    private static void validateArguments(ProductTool tool, Map<String, Object> arguments) {
        for (Map.Entry<String, ProductToolParameterSpec> entry : tool.parameterSpecs().entrySet()) {
            String name = entry.getKey();
            ProductToolParameterSpec spec = entry.getValue();
            Object value = arguments.get(name);
            if (spec.required() && value == null) {
                throw new IllegalStateException("tool argument missing: " + name);
            }
            if (value == null) {
                continue;
            }
            String type = spec.type() == null ? "string" : spec.type().trim().toLowerCase();
            boolean matched = switch (type) {
                case "integer" -> value instanceof Integer || value instanceof Long;
                case "number" -> value instanceof Number;
                case "boolean" -> value instanceof Boolean;
                default -> value instanceof String;
            };
            if (!matched) {
                throw new IllegalStateException("tool argument type mismatch for " + name + ": expected " + type);
            }
        }
    }

    private static String stripQuotes(String value) {
        String text = value.trim();
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }
}
