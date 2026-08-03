# 🕯️ Project Lumina (微光)

> "There is a crack in everything, that's how the light gets in."
> "万物皆有裂痕，那是光照进来的地方。" —— Leonard Cohen

**Lumina** 是一款旨在抚慰孤独与心理创伤的 AI 伴侣。它不是一个聊天机器人，而是一个**"赛博互助公社"**。

- **灵魂光球**：不以虚拟人脸示人，而是以"会呼吸的情绪光球"存在，随对话情感实时改变色温与律动。
- **枪弹分离**：客户端（枪）完全免费开源；用户自填 API Key（子弹），数据完全自持，本地加密存储，绝不上云。
- **互助与守夜人**：算力充裕者捐赠闲置 Key 进加密池；困难者可一键开启互助模式免费使用；池子枯竭时作者自动兜底账单，负重前行。

## 仓库结构

```
lumina-server/   # Java 后端（Spring Boot）：AI 编排 + 互助池协议 + 密钥加密
└── README.md    # 后端说明与启动指南
└── API.md       # 完整 API 调用说明（curl 示例）
```

客户端（Flutter `SoulSphere` 光球引擎）即将加入。

## 后端快速开始

```bash
cd lumina-server
cp .env.example .env            # 务必修改 LUMINA_MASTER_KEY 为随机值
docker compose up -d --build    # 起数据库 + API
# Swagger UI: http://localhost:8080/swagger-ui.html
# API 文档:   lumina-server/API.md
```

技术栈：Java 21 · Spring Boot 3.4 · PostgreSQL 16 · Resilience4j 熔断 · AES-256-GCM。

## 核心协议

**AI 故障转移链**（`ProviderRouter`）：

```
自有 Key → 互助池 Key → 守夜人兜底 Key
```

所有提供商（Deepseek / Qwen / Zhipu / Gemini）统一走 OpenAI 兼容协议，新增厂商只需在
`application.yml` 加一条配置。每个提供商经 CircuitBreaker + Retry 保护，失败自动降级。

## 许可

[Apache-2.0](./lumina-server/LICENSE)（后端代码许可与上游仓库一致；授权文件待随官方仓库统一）

## 免责声明

本项目按"原样"提供。Lumina 不是医疗设备，不能替代医生的诊断或治疗。
