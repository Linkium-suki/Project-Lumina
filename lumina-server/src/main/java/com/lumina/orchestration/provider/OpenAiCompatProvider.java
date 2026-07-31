package com.lumina.orchestration.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumina.orchestration.model.ChatMessage;
import com.lumina.orchestration.model.ChatResult;
import com.lumina.orchestration.model.Usage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议适配器。
 * Deepseek / Qwen / Zhipu / Gemini(OpenAI 端点) 均走同一实现，仅 baseUrl 与模型不同。
 */
@Slf4j
public class OpenAiCompatProvider implements ChatProvider {

    private final String id;
    private final List<String> models;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OpenAiCompatProvider(String id, String baseUrl, List<String> models) {
        this.id = id;
        this.models = models.isEmpty() ? List.of("default") : models;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/")
                .build();
    }

    @Override
    public String providerId() {
        return id;
    }

    @Override
    public List<String> supportedModels() {
        return models;
    }

    @Override
    public ChatResult chat(List<ChatMessage> messages, String model, String apiKey) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", messages,
                    "stream", false);

            JsonNode body = restClient.post()
                    .uri("chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), (req, res) -> {
                        String detail = new String(res.getBody().readAllBytes());
                        throw new ProviderException(
                                "provider [" + id + "] returned HTTP " + res.getStatusCode() + ": " + detail);
                    })
                    .body(JsonNode.class);

            String text = firstChoiceText(body);
            Usage usage = parseUsage(body);
            String usedModel = body.path("model").asText(model);
            return new ChatResult(text, id, usedModel, usage);
        } catch (ProviderException e) {
            throw e;
        } catch (Exception e) {
            throw ProviderException.unavailable("provider [" + id + "] call failed", e);
        }
    }

    private String firstChoiceText(JsonNode body) {
        JsonNode choices = body.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode content = choices.get(0).path("message").path("content");
            if (!content.isMissingNode()) {
                return content.asText();
            }
            // 部分推理模型返回 content 分段（reasoning_content + content）
            JsonNode text = choices.get(0).path("text");
            return text.isMissingNode() ? "" : text.asText();
        }
        throw new ProviderException("provider [" + id + "] returned empty choices");
    }

    private Usage parseUsage(JsonNode body) {
        JsonNode usage = body.path("usage");
        if (usage.isMissingNode()) {
            return Usage.EMPTY;
        }
        return new Usage(
                usage.path("prompt_tokens").asInt(0),
                usage.path("completion_tokens").asInt(0));
    }
}
