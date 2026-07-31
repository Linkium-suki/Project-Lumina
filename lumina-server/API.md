# Lumina Server API 调用说明

- **Base URL**: `http://localhost:8080`
- **接口前缀**: `/api/v1`
- **鉴权**: 注册返回的 token 放入请求头 `Authorization: Bearer <token>`
- **响应包装**: 所有接口返回统一结构

```json
{
  "success": true,
  "code": "OK",
  "message": null,
  "data": { }
}
```

`success=false` 时，`code` 为错误码（见文末），`message` 为人类可读的错误信息。

---

## 1. 设备注册

注册返回 **userId** 与 **token**。同一 `deviceId` 重复注册是幂等的：返回既有 `userId`，`token` 为 `null`（客户端应保留首次拿到的 token）。

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"deviceId": "my-android-phone-001", "nickname": "夜行者"}'
```

```json
{
  "success": true,
  "code": "OK",
  "message": null,
  "data": { "userId": 1, "token": "3fKd9s..." }
}
```

> 建议用 shell 变量保存 token 供后续调用：`TOKEN="3fKd9s..."`

---

## 2. 添加自有 API Key（"装子弹"）

支持的 `provider`：`deepseek` / `qwen` / `zhipu` / `gemini`。

```bash
curl -X POST http://localhost:8080/api/v1/keys \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"provider": "deepseek", "model": "deepseek-chat", "apiKey": "sk-xxxxxxxx"}'
```

```json
{
  "success": true,
  "code": "OK",
  "data": {
    "id": 10,
    "provider": "deepseek",
    "model": "deepseek-chat",
    "status": "PRIVATE",
    "usedQuota": 0,
    "expiresAt": null
  }
}
```

> 明文 `apiKey` 加密后入库，**任何接口都不会回传明文**。

### 列出我的 Key

```bash
curl http://localhost:8080/api/v1/keys -H "Authorization: Bearer $TOKEN"
```

### 删除 Key

```bash
curl -X DELETE http://localhost:8080/api/v1/keys/10 -H "Authorization: Bearer $TOKEN"
```

> 已捐赠进互助池的 Key 不能直接删除，需先 `POST /pool/withdraw/{keyId}`。

---

## 3. 对话

`message` 必填（≤4000 字符）。`history` 为可选多轮上下文（不含本条消息），
`voice=true` 时返回 TTS 音频 base64。`provider` 可选，指定后只尝试该厂商。

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "今天加班到很晚，有点累",
    "history": [
      {"role": "user", "content": "你好，能陪我聊聊天吗？"},
      {"role": "assistant", "content": "当然可以，我一直在。你今天过得怎么样？"}
    ],
    "voice": false
  }'
```

```json
{
  "success": true,
  "code": "OK",
  "data": {
    "text": "辛苦了。你已经很努力了，接下来好好休息一下，把今晚留给自己。",
    "provider": "deepseek",
    "model": "deepseek-chat",
    "source": "OWN",
    "usage": { "promptTokens": 45, "completionTokens": 32 },
    "audioBase64": null
  }
}
```

- `source`: `OWN`（自有 Key）/ `POOL`（互助池）/ `WATCHMAN`（守夜人兜底）
- `voice=true` 时 `audioBase64` 为 MP3 的 base64 编码，客户端直接解码播放。

### 带语音的对话

```bash
curl -X POST http://localhost:8080/api/v1/chat \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"message": "晚安", "voice": true}'
```

---

## 4. 文本转语音（独立接口）

```bash
curl "http://localhost:8080/api/v1/tts?text=%E6%99%9A%E5%AE%89&voice=zh-CN-XiaoxiaoNeural" \
  -H "Authorization: Bearer $TOKEN"
```

```json
{
  "success": true,
  "code": "OK",
  "data": "//NExAARSunICDf8tCRB///tGVK4iLc6j…（MP3 base64）"
}
```

`voice` 可省略，默认 `zh-CN-XiaoxiaoNeural`。

---

## 5. 互助池协议

### 5.1 捐赠 Key（点亮一盏灯）

把自有 Key 托管进加密池，供受助者使用：

```bash
curl -X POST http://localhost:8080/api/v1/pool/donate/10 \
  -H "Authorization: Bearer $TOKEN"
```

```json
{
  "success": true,
  "code": "OK",
  "data": { "id": 10, "provider": "deepseek", "status": "POOL", "usedQuota": 0, "expiresAt": null }
}
```

### 5.2 撤回捐赠

```bash
curl -X POST http://localhost:8080/api/v1/pool/withdraw/10 \
  -H "Authorization: Bearer $TOKEN"
```

> 若该 Key 正被某位受助者使用，会返回 `CONFLICT`，稍后再试。

### 5.3 开启互助模式（受助者）

```bash
curl -X POST http://localhost:8080/api/v1/pool/join -H "Authorization: Bearer $TOKEN"
```

开启后，聊天会自动按"**自有 → 互助池 → 守夜人**"顺序调度；没有自有 Key 时自动领取池内算力，用后即释放。

### 5.4 关闭互助模式

```bash
curl -X POST http://localhost:8080/api/v1/pool/leave -H "Authorization: Bearer $TOKEN"
```

### 5.5 查看互助状态

```bash
curl http://localhost:8080/api/v1/pool/status -H "Authorization: Bearer $TOKEN"
```

```json
{
  "success": true,
  "code": "OK",
  "data": {
    "aidMode": true,
    "dailyLimit": 100,
    "todayUsage": 3,
    "availablePoolKeys": 2
  }
}
```

- `dailyLimit`：互助模式每日请求上限（`lumina.pool.daily-request-limit`）。
- `todayUsage`：今日已消耗的互助次数，达到 `dailyLimit` 后返回 `POOL_LIMIT_EXCEEDED`。

---

## 6. 完整流程示例（一条龙）

```bash
BASE=http://localhost:8080/api/v1

# 1) 注册
TOKEN=$(curl -s -X POST $BASE/auth/register \
  -H "Content-Type: application/json" \
  -d '{"deviceId":"demo-001","nickname":"demo"}' | \
  sed -E 's/.*"token":"([^"]+)".*/\1/')

echo "token=$TOKEN"

# 2) 装子弹（自有 Key）
curl -s -X POST $BASE/keys \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"provider":"deepseek","model":"deepseek-chat","apiKey":"sk-xxx"}'

# 3) 对话
curl -s -X POST $BASE/chat \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"message":"你好，Lumina"}' | jq '.data | {text, source}'

# 4) 捐赠（Key id 取上一步返回的 id，假设为 1）
curl -s -X POST $BASE/pool/donate/1 -H "Authorization: Bearer $TOKEN"

# 5) 开启互助模式
curl -s -X POST $BASE/pool/join -H "Authorization: Bearer $TOKEN"

# 6) 互助对话（此时 source=POOL）
curl -s -X POST $BASE/chat \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"message":"有点难过"}' | jq '.data | {text, source}'
```

---

## 7. 错误码

| code | HTTP | 含义 |
|---|---|---|
| `UNAUTHORIZED` | 401 | 未带 / 无效 token |
| `RATE_LIMITED` | 429 | 请求过于频繁（默认 30 次/分钟） |
| `BAD_REQUEST` | 400 | 参数校验失败 |
| `NOT_FOUND` | 404 | 资源不存在 |
| `KEY_NOT_FOUND` | 404 | Key 不存在或不属于当前用户 |
| `CONFLICT` | 200 | 状态冲突（如 Key 已在池中 / 正被使用） |
| `AID_MODE_REQUIRED` | 200 | 未开启互助模式却尝试使用互助池 |
| `POOL_LIMIT_EXCEEDED` | 200 | 今日互助额度用尽 |
| `POOL_UNAVAILABLE` | 200 | 互助池当前无可用算力 |
| `PROVIDER_UNAVAILABLE` | 200 | 所有 AI 提供商均不可用（含守夜人兜底失效） |
| `PROVIDER_RESPONSE` | 200 | AI 服务返回异常 |
| `TTS_UNAVAILABLE` | 200 | 语音服务不可用 |
| `INTERNAL_ERROR` | 500 | 服务器内部错误 |

> 除 `UNAUTHORIZED` / `RATE_LIMITED` / `BAD_REQUEST` / `NOT_FOUND` 外，业务错误统一返回 HTTP 200，以 `success=false` + `code` 区分。
