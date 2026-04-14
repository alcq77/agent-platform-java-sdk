package io.github.alcq77.cqgent.product.spi.model;

import io.github.alcq77.cqgent.model.api.dto.ChatCompletionRequest;
import io.github.alcq77.cqgent.model.api.dto.ChatCompletionResponse;

public interface ProductModelProvider {

    String providerCode();

    ChatCompletionResponse complete(ProductEndpointConfig endpoint, ChatCompletionRequest request);
}
