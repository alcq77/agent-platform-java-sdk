package io.github.alcq77.cqgent.model.api.spi;

/**
 * 内置 {@code provider} 编码常量，与 {@code agent.model.endpoints[].provider} 对应。
 * <p>新增第三方协议时不必改此类：可在自有适配器内定义任意新编码字符串；此处仅收录平台内置实现。</p>
 */
public final class ModelProviderCodes {

    private ModelProviderCodes() {
    }

    /** OpenAI Chat Completions 及兼容形态（多数国产厂商）。 */
    public static final String OPENAI_COMPAT = "openai_compat";

    /** Anthropic Messages（Claude）。 */
    public static final String ANTHROPIC = "anthropic";
}
