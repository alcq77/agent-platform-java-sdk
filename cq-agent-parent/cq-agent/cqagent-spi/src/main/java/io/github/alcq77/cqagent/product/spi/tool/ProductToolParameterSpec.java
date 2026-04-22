package io.github.alcq77.cqagent.product.spi.tool;

/**
 * ProductTool 参数定义。
 */
public record ProductToolParameterSpec(
    String type,
    boolean required,
    String description
) {

    public static ProductToolParameterSpec string(boolean required, String description) {
        return new ProductToolParameterSpec("string", required, description);
    }

    public static ProductToolParameterSpec integer(boolean required, String description) {
        return new ProductToolParameterSpec("integer", required, description);
    }

    public static ProductToolParameterSpec number(boolean required, String description) {
        return new ProductToolParameterSpec("number", required, description);
    }

    public static ProductToolParameterSpec bool(boolean required, String description) {
        return new ProductToolParameterSpec("boolean", required, description);
    }
}
