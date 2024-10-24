package cn.yyk.middleware.sdk.infrastructure.openai;


import cn.yyk.middleware.sdk.infrastructure.openai.dto.ChatCompletionRequestDTO;
import cn.yyk.middleware.sdk.infrastructure.openai.dto.ChatCompletionSyncResponseDTO;

public interface IOpenAI {

    ChatCompletionSyncResponseDTO completions(ChatCompletionRequestDTO requestDTO) throws Exception;

}
