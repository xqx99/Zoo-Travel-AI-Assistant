# 动物园旅游智能规划助手（Zoo Travel AI Assistant）

一个基于 **LangChain4j RAG 架构**的动物园旅游智能规划助手。用户通过对话输入目的地与想看的动物，Agent 结合**结构化数据库数据**与**知识库向量检索**，流式输出定制化旅游方案。

## 核心特性

- **Agent 对话规划**：意图识别（目的地 / 动物 / 咨询类型）→ 双路检索 → 流式生成
- **完整 RAG 问答回路**：本地 ONNX 嵌入模型 + Redis(RediSearch) 向量检索，回答附带引用来源
- **SSE 流式输出**：逐 token 流式返回，前端打字机式渲染
- **会话记忆**：多轮对话上下文持久化到 Redis
- **LLM 可插拔**：配置 DeepSeek API Key 走真实 LLM；未配置时自动降级为数据库直拼模板回答
- **数据管理**：动物园 / 动物 / 知识文档的完整 CRUD 后台管理界面

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.5（Java 21）、Spring JDBC |
| Agent 框架 | LangChain4j 1.22（OpenAI 兼容、RAG、流式、ChatMemory） |
| LLM | DeepSeek（OpenAI 兼容 API，`deepseek-chat`） |
| 嵌入模型 | 本地 ONNX `all-MiniLM-L6-v2`（384 维，无需外部 API） |
| 结构化数据 | MySQL 8.0 |
| 向量存储 | Redis Stack（RediSearch 向量索引） |
| 会话记忆 | Redis |
| 前端 | Vue 3 + Vite + Element Plus + Pinia |

## 系统架构

```
用户输入
   │
   ▼
┌──────────┐   意图识别    ┌──────────────┐
│  前端 Vue │ ──────────▶ │  IntentService │
│ (SSE 渲染) │ ◀────────── │  (规则匹配)    │
└──────────┘   流式输出    └──────┬───────┘
                                  │
                     ┌────────────┴────────────┐
                     ▼                          ▼
        ┌──────────────────┐        ┌───────────────────┐
        │ 结构化检索 (MySQL) │        │ RAG 向量检索(Redis) │
        │ zoo / animal 表   │        │ FT.SEARCH 相似度   │
        └──────────────────┘        └───────────────────┘
                     │                          │
                     └──────────┬───────────────┘
                                ▼
                   ┌──────────────────────┐
                   │ 组装：系统提示词 + 参考 │
                   │ 信息 + 会话记忆       │
                   └──────────┬───────────┘
                              ▼
              ┌───────────────────────────┐
              │ DeepSeek 流式生成（或本地  │
              │ 降级模板）                │
              └───────────────────────────┘
```

## 目录结构

```
zootravel/
├── backend/                  # Spring Boot 后端
│   └── src/main/
│       ├── java/com/zootravel/
│       │   ├── config/       # RagConfig(向量链) / JedisConfig / WebConfig(CORS)
│       │   ├── controller/   # Chat / Zoo / Knowledge 控制器
│       │   ├── dto/          # 请求响应对象
│       │   ├── entity/       # zoo / animal / zoo_animal / knowledge_doc
│       │   ├── memory/       # RedisChatMemoryStore（会话记忆）
│       │   ├── repository/   # JdbcTemplate 数据访问
│       │   └── service/      # ChatService(编排) / IntentService / ZooService 等
│       └── resources/
│           ├── application.yml
│           └── prompt/system.md   # Agent 系统提示词
├── frontend/                 # Vue3 前端
│   └── src/
│       ├── api/              # axios / SSE fetch 封装
│       ├── stores/           # Pinia 会话状态
│       ├── views/            # ChatView / ZooList / ZooDetail / Admin
│       └── router/
└── db/
    ├── schema.sql            # 建库建表（4 张表）
    └── seed.sql              # 10 动物园 + 20 动物 + 8 篇知识文档
```

## 快速开始

### 前置要求

- JDK 21、Maven 3.9+、Node.js 18+
- Docker（用于 MySQL 与 Redis Stack）

### 1. 启动基础设施

```bash
# MySQL 8.0（root 密码可自定义，需与下方一致）
docker run -d --name zootravel-mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 -e MYSQL_DATABASE=zootravel mysql:8.0

# Redis Stack（内置 RediSearch 向量检索模块）
docker run -d --name zootravel-redis -p 6379:6379 redis/redis-stack:latest
```

### 2. 初始化数据库

```bash
mysql -h127.0.0.1 -uroot -p123456 < db/schema.sql
mysql -h127.0.0.1 -uroot -p123456 < db/seed.sql
```

### 3. 配置 DeepSeek API Key（可选）

不配置时系统自动降级为本地模板回答，功能仍可演示。

```bash
# Windows
setx DEEPSEEK_API_KEY "sk-你的key"
# Linux / macOS
export DEEPSEEK_API_KEY="sk-你的key"
```

模型可通过环境变量 `DEEPSEEK_MODEL` 切换（默认 `deepseek-chat`，如遇限流可试 `deepseek-flash`）。

### 4. 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

启动时自动构建 RAG 向量索引（幂等：索引已有数据则跳过，避免重复嵌入）。

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173

## 主要接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/chat/stream` | SSE 流式对话（事件：`meta` 引用 / `delta` 增量 / `done` / `error`） |
| GET  | `/api/chat/history?sessionId=` | 查询会话历史 |
| DELETE | `/api/chat/history?sessionId=` | 清空会话历史 |
| GET  | `/api/zoo/list` | 动物园分页列表（支持城市/动物筛选） |
| GET  | `/api/zoo/cities` | 城市列表 |
| GET  | `/api/zoo/{id}` | 动物园详情（含动物列表） |
| POST/PUT/DELETE | `/api/zoo...` | 动物园 CRUD |
| GET/POST/PUT/DELETE | `/api/knowledge...` | 知识文档 CRUD |
| POST | `/api/knowledge/reindex` | 重建 RAG 向量索引 |

## 配置说明（backend/src/main/resources/application.yml）

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `spring.datasource.password` | `${MYSQL_PASSWORD:123456}` | MySQL 密码 |
| `langchain4j.open-ai.chat-model.api-key` | `${DEEPSEEK_API_KEY:}` | DeepSeek Key（留空走降级） |
| `langchain4j.open-ai.chat-model.base-url` | `https://api.deepseek.com/v1` | OpenAI 兼容端点 |
| `zootravel.chat.local-fallback` | `true` | 无 Key 时启用本地降级 |
| `zootravel.rag.chunk-size` | `500` | 知识文档切分长度 |
| `zootravel.rag.max-results` | `4` | 向量检索返回条数 |
| `zootravel.rag.min-score` | `0.6` | 向量检索相似度阈值 |

## 安全说明

- **API Key 仅通过环境变量注入，请勿写入任何代码或配置文件**
- 生产部署时请更换默认的 MySQL 密码，并限制 `DEEPSEEK_API_KEY` 的访问权限
