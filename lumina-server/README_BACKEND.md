# Lumina Server

Lumina 后端的可维护实现：**AI 编排（多提供商故障转移）+ 互助池协议 + 密钥加密存储**。

> 万物皆有裂痕，那是光照进来的地方。—— Leonard Cohen

## 技术栈

| 层 | 选型 |
|---|---|
| 语言 / 框架 | Java 21 · Spring Boot 3.4 · Maven |
| 数据库 | PostgreSQL 16 · Flyway |
| AI 编排 | OpenAI 兼容协议适配器（Deepseek / Qwen / Zhipu / Gemini）+ Resilience4j 熔断重试 |
| 语音 | Azure TTS（SSML，REST） |
| 安全 | AES-256-GCM 密钥加密 + SHA-256 token 鉴权 |
| API 文档 | springdoc-openapi（Swagger UI: `/swagger-ui.html`） |
| 测试 | JUnit 5 · Testcontainers |
| 部署 | Docker Compose |

## 快速开始

```bash
# 1. 复制环境变量样例并按需修改（尤其 LUMINA_MASTER_KEY）
cp .env.example .env

# 2. 构建
mvn clean package -DskipTests

# 3. 起数据库 + API（api 镜像需要先构建好 jar）
docker compose up -d --build

# 4. 打开 API 文档
# http://localhost:8080/swagger-ui.html
```

本地开发可只起数据库，再用 IDE 或 `mvn spring-boot:run` 跑：

```bash
docker compose up -d db
mvn spring-boot:run
```

## 环境变量

见 [.env.example](.env.example)。关键项：

| 变量 | 说明 |
|---|---|
| `LUMINA_MASTER_KEY` | AES-256-GCM 主密钥，**必须替换**，仅存环境变量 |
| `LUMINA_WATCHMAN_ENABLED` | 守夜人兜底开关（互助池枯竭时使用作者 Key） |
| `LUMINA_WATCHMAN_DEEPSEEK_KEY` | 守夜人兜底 Key |
| `AZURE_TTS_KEY` | Azure 语音密钥 |

## 核心设计

### 1. 故障转移链（`orchestration/failover/ProviderRouter`）

路由优先级：**自有 Key → 互助池 Key → 守夜人兜底**；同级内按 `application.yml` 中
`lumina-ai.providers.chat` 列表顺序依次尝试。每个提供商经 CircuitBreaker + Retry 保护，
失败即登记健康状态并降级。所有提供商（Deepseek/Qwen/Zhipu/Gemini）都走 OpenAI 兼容协议，
因此只有**一个** `OpenAiCompatProvider` 实现，新增提供商只需加一条配置。

### 2. 密钥加密（`key/KeyEncryptor`）

用户自填的 API Key 用 AES-256-GCM 加密后入库，密文格式 `base64(nonce + ciphertext)`，
主密钥来自环境变量，绝不落库、绝不提交。API 永不回传明文。

### 3. 互助池（`pool/PoolService`）

- **捐赠**：`POST /api/v1/pool/donate/{keyId}` 把自有 Key 托管进加密池。
- **受助**：开启互助模式后，聊天自动按需领取池内 Key，用后即释放。
- **防滥用**：每日请求上限（`lumina.pool.daily-request-limit`，默认 100）。
- **并发安全**：一个 Key 同时只允许一人领取（数据库部分唯一索引 + REQUIRES_NEW 兜底）。
- **守夜人**：池枯竭时自动降级到配置中的作者 Key。

## API 调用

完整的接口清单、请求/响应示例与一条龙 curl 流程见 **[API.md](./API.md)**。

Swagger UI：启动后访问 `http://localhost:8080/swagger-ui.html`。

## 测试

```bash
mvn test
```

- `KeyEncryptorTest` / `TokenManagerTest`：纯单元测试，无需外部依赖。
- `LuminaIntegrationTest`：Testcontainers + 桩 Provider 的全流程测试
  （注册 → 填 Key → 对话 → 捐赠 → 互助对话），**需要本机有 Docker**，否则自动跳过。

## 目录结构

```
src/main/java/com/lumina/
├── common/        # 统一响应、异常处理、限流、拦截器注册
├── auth/          # 设备注册 + SHA-256 token 鉴权
├── user/          # 用户实体
├── key/           # API Key 管理 + AES-256-GCM 加解密
├── orchestration/ # 提供商适配器、故障转移、路由、TTS
├── chat/          # 对话编排与用量审计
└── pool/          # 互助池协议
```
