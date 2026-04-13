package com.agent.platform.product.spi.model;

import com.agent.platform.model.api.dto.ChatCompletionRequest;
import com.agent.platform.model.api.dto.ChatCompletionResponse;

public interface ProductModelProvider {

    String providerCode();

    ChatCompletionResponse complete(ProductEndpointConfig endpoint, ChatCompletionRequest request);
}
