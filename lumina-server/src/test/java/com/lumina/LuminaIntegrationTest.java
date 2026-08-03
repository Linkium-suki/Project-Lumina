package com.lumina;

import com.fasterxml.jackson.databind.JsonNode;
import com.lumina.orchestration.model.ChatMessage;
import com.lumina.orchestration.model.ChatResult;
import com.lumina.orchestration.model.Usage;
import com.lumina.orchestration.provider.ChatProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端集成测试：注册 → 填 Key → 对话 → 捐赠 → 互助模式对话 → 状态。
 * 使用 Testcontainers Postgres + 桩 ChatProvider，不发起真实 LLM 调用。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
class LuminaIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    TestRestTemplate rest;

    @Test
    void fullFlow_registerChatDonateAid() {
        // 1. 注册
        ResponseEntity<JsonNode> reg = post("/api/v1/auth/register",
                Map.of("deviceId", "device-001", "nickname", "test"), null);
        assertThat(reg.getStatusCode()).isEqualTo(HttpStatus.OK);
        String token = reg.getBody().path("data").path("token").asText();
        assertThat(token).isNotBlank();

        // 2. 未带 token 应 401
        ResponseEntity<JsonNode> unauth = rest.getForEntity("/api/v1/keys", JsonNode.class);
        assertThat(unauth.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // 3. 填 Key
        ResponseEntity<JsonNode> addKey = post("/api/v1/keys",
                Map.of("provider", "deepseek", "model", "deepseek-chat", "apiKey", "sk-test-001"), token);
        assertThat(addKey.getStatusCode()).isEqualTo(HttpStatus.OK);
        long keyId = addKey.getBody().path("data").path("id").asLong();
        assertThat(addKey.getBody().path("data").path("encryptedKey").asText())
                .isEmpty(); // 明文永不回传

        // 4. 对话（自有 Key）
        ResponseEntity<JsonNode> chat1 = post("/api/v1/chat", Map.of("message", "你好"), token);
        assertThat(chat1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(chat1.getBody().path("data").path("text").asText()).isEqualTo("Hello from stub");
        assertThat(chat1.getBody().path("data").path("source").asText()).isEqualTo("OWN");

        // 5. 捐赠进互助池
        ResponseEntity<JsonNode> donate = post("/api/v1/pool/donate/" + keyId, Map.of(), token);
        assertThat(donate.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(donate.getBody().path("data").path("status").asText()).isEqualTo("POOL");

        // 6. 开启互助模式
        ResponseEntity<JsonNode> join = post("/api/v1/pool/join", Map.of(), token);
        assertThat(join.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 7. 互助模式对话（走 POOL 来源）
        ResponseEntity<JsonNode> chat2 = post("/api/v1/chat", Map.of("message", "今天有点累"), token);
        assertThat(chat2.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(chat2.getBody().path("data").path("source").asText()).isEqualTo("POOL");

        // 8. 互助状态
        ResponseEntity<JsonNode> status = rest.exchange(
                "/api/v1/pool/status", HttpMethod.GET,
                new HttpEntity<>(headers(token)), JsonNode.class);
        assertThat(status.getBody().path("data").path("aidMode").asBoolean()).isTrue();
        assertThat(status.getBody().path("data").path("todayUsage").asLong()).isGreaterThanOrEqualTo(1);

        // 9. 关闭互助模式
        ResponseEntity<JsonNode> leave = post("/api/v1/pool/leave", Map.of(), token);
        assertThat(leave.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<JsonNode> post(String path, Object body, String token) {
        HttpHeaders headers = headers(token);
        return rest.exchange(path, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
    }

    private static HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    @TestConfiguration
    static class StubConfig {

        @Bean
        ChatProvider stubChatProvider() {
            return new ChatProvider() {
                @Override
                public String providerId() {
                    return "deepseek";
                }

                @Override
                public List<String> supportedModels() {
                    return List.of("deepseek-chat");
                }

                @Override
                public ChatResult chat(List<ChatMessage> messages, String model, String apiKey) {
                    return new ChatResult("Hello from stub", "deepseek", "deepseek-chat",
                            new Usage(10, 5));
                }
            };
        }
    }
}
