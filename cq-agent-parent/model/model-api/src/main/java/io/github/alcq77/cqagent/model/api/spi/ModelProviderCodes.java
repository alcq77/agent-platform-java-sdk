package io.github.alcq77.cqagent.model.api.spi;

/**
 * Built-in provider codes mapped from {@code agent.model.endpoints[].provider}.
 * Custom providers may define their own codes outside this class.
 */
public final class ModelProviderCodes {

    private ModelProviderCodes() {
    }

    /** OpenAI-compatible Chat Completions providers. */
    public static final String OPENAI_COMPAT = "openai_compat";

    /** Native OpenAI provider. */
    public static final String OPENAI = "openai";

    /** Native Anthropic Messages provider. */
    public static final String ANTHROPIC = "anthropic";

    /** Local/self-hosted Ollama provider. */
    public static final String OLLAMA = "ollama";

    /** Alibaba Cloud DashScope/Qwen provider. */
    public static final String DASHSCOPE = "dashscope";
}
