package io.github.alcq77.cqgent.product.core.tool;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import io.github.alcq77.cqgent.product.spi.tool.ProductTool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        String input = extractArgument(request.arguments());
        return tool.execute(input);
    }

    private static ToolSpecification toSpecification(ProductTool tool) {
        // 当前统一入参协议：{"input":"..."}，兼容已有 ProductTool#execute(String) 形态。
        JsonObjectSchema parameters = JsonObjectSchema.builder()
                .addStringProperty("input", "The plain text input for this tool.")
                .required("input")
                .build();
        return ToolSpecification.builder()
                .name(tool.name())
                .description("cqgent product tool: " + tool.name())
                .parameters(parameters)
                .build();
    }

    private static String extractArgument(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return "";
        }
        String text = arguments.trim();
        int keyIndex = text.indexOf("\"input\"");
        if (keyIndex < 0) {
            // 兼容极简场景：模型直接返回纯字符串参数。
            return stripQuotes(text);
        }
        int colon = text.indexOf(':', keyIndex);
        if (colon < 0) {
            return "";
        }
        int startQuote = text.indexOf('"', colon + 1);
        if (startQuote < 0) {
            return text.substring(colon + 1).replace("}", "").trim();
        }
        int endQuote = text.indexOf('"', startQuote + 1);
        if (endQuote < 0) {
            return text.substring(startQuote + 1).trim();
        }
        return text.substring(startQuote + 1, endQuote);
    }

    private static String stripQuotes(String value) {
        String text = value.trim();
        if (text.startsWith("\"") && text.endsWith("\"") && text.length() >= 2) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }
}
