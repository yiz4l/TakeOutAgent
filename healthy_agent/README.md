# Python Agent 设计与向量记忆

本目录采用“只启动 Python Worker、调用 Java 内部 API”的部署方式。Python 不连接或写入 Java 业务数据库，身份、饮食事实、任务状态和最终分析结果仍由 Java 服务负责。

## 向量数据库选型：Qdrant

本项目优先使用 Qdrant：它可用 Docker 单机启动，Python SDK 完整，支持 cosine 向量检索、payload metadata 过滤、时间条件和 on-disk 索引。相比 Milvus，第一版运维更轻；相比 FAISS/Chroma，Qdrant 自带持久化服务、并发 HTTP API 和多用户过滤，更适合 Worker 部署。生产环境可使用 Qdrant Cloud 或集群。过期记忆通过应用层 `valid_until` 过滤和定期清理实现，不能把 Qdrant 当作自动 TTL 数据库。

启动和初始化：

```bash
cd healthy_agent
cp .env .env
docker compose up -d qdrant
python -m pip install -r requirements.txt
python init_qdrant.py
```

`EMBEDDING_DIM` 必须与实际 embedding 模型输出维度一致；更换模型时创建新集合并迁移，不能直接写入不同维度的向量。

当前代码默认 `ENABLE_QDRANT=false`，因为尚未配置 embedding 服务时不能写入零向量。配置 `OPENAI_API_KEY`、创建 collection 后，同时设置 `ENABLE_LLM=true` 和 `ENABLE_QDRANT=true`；关闭这两个开关不影响 Java 任务 API 和规则分析流程。

## “建表”结果

`init_qdrant.py` 创建三个 collection（Qdrant 中等价于表）：

### `agent_short_memory` 和 `agent_long_memory`

每个 point 包含 `id`、向量和 payload：

| payload | 说明 |
|---|---|
| `user_id` | Java 传入的用户 ID，所有查询必须过滤 |
| `memory_type` | `short_term`、`long_term` 或 `habit` |
| `text` | 用于 embedding 和模型上下文的记忆摘要，不存完整隐私原文 |
| `structured` | JSON，如 `{nutrient:"protein", trend:"deficit", streak_weeks:2}` |
| `source` | `conversation`、`analysis`、`feedback` |
| `conversation_id` | 短期会话 ID，可选 |
| `memory_key` | 稳定合并键，如 `protein_deficit:weekly` |
| `analysis_date` | 事实所属日期（`yyyy-MM-dd`） |
| `valid_until` | 短期记忆过期时间；长期/习惯可为空 |
| `confidence` | 0 到 1，低置信度只作弱提示 |
| `created_at` | 写入时间 |

point ID 应使用 UUID。短期记忆保存最近若干轮对话或当前任务上下文，设置 `valid_until`（例如 30 天）；长期记忆保存跨分析稳定的结论；习惯记忆保存带时间窗口和趋势的营养模式。相同用户、营养项和窗口应使用稳定业务 key 做 upsert，避免重复累积；短期会话则使用 `conversation_id` 与 turn 标识去重。

### `nutrition_knowledge`

保存可信营养知识切片，payload 至少包括 `title`、`content`、`source`、`tags`、`applicable_population`、`knowledge_version`、`published_at`。它与用户记忆分集合，检索时不会泄露其他用户信息。

## 长期记忆如何工作

1. Java context 提供本次饮食事实；Python 规则层计算蛋白质等营养趋势。
2. Agent 将当前建议写入 `agent_short_memory`，将“用户在最近两周蛋白质不足”等稳定摘要写入 `agent_long_memory`。
3. 下一次分析先按 `user_id`、类型和有效期过滤，再用当前风险查询向量召回相关记忆。
4. 对召回结果按时间衰减、置信度和连续周数重排，放入模型上下文；模型不能自行猜测未召回的事实。
5. 新结果对旧记忆做合并：连续缺乏则递增 `streak_weeks`，已补足则记录 `trend=improved` 并降低旧记忆置信度，而不是删除历史。

向量只负责“语义相似的记忆找回”，不是事实数据库。每日摄入克数、任务版本和分析结果仍以 Java 数据库为准；向量 payload 只存脱敏摘要和引用 ID。

## Agent 完整流程（仅 Python 客户端）

1. Worker 启动，生成唯一 `workerId`，读取 `JAVA_AGENT_BASE_URL` 和独立服务凭证。
2. `POST /internal/agent/health-analysis-tasks/claim` 领取任务（租约 30~600 秒）。无任务则退避轮询。
3. `GET /internal/agent/health-analysis-tasks/{taskId}/context` 获取 Java 组装的、已权限过滤的饮食事实；Python 不带用户 JWT，也不自行查用户接口。
4. 查询 `agent_short_memory` 和 `agent_long_memory` 中该用户的记忆，同时检索 `nutrition_knowledge`。所有记忆查询强制带 `user_id` 过滤。
5. 规则层计算确定性营养汇总、缺口、连续周数和评分；规则版本与 `modelVersion` 一起记录。
6. （配置 embedding/大模型后）调用模型并要求严格 JSON；用 Pydantic 校验分数、推荐分类白名单、文本长度和安全约束。当前无模型密钥时使用确定性规则结果。
7. 模型耗时较长时定期 `heartbeat`；续租失败立即停止提交。
8. 将合并后的记忆 upsert 到 Qdrant，再调用 `POST .../{taskId}/complete`，携带任务 `requestId` 作为幂等键、原始 `inputRevision` 和 `modelVersion`。Java 在一个事务中写入 `health_analysis`、`diet_recommendation` 并置 `SUCCEEDED`。
9. 网络/模型可重试错误调用 `.../{taskId}/fail` 并标记 `retryable=true`；参数、校验或安全错误标记不可重试。租约过期或输入版本变化时不提交旧结果。
10. 记录不含 JWT、密钥、完整提示词和用户隐私的指标与日志。

详尽的请求字段和状态机以 `doc/API.md` 第 10 节为准。

## 本次端到端扩展

Java 的 `doc/updateDatebase.sql` 追加了三类数据：`user_health_profile` 保存用户主动填写的身高、体重、目标、偏好和过敏；`nutrition_daily_state` 保存每日营养聚合与 revision；`health_period_summary` 保存两周/月度总结及来源版本。画像接口为 `GET/PUT /api/user/health-profile`。Agent context 接口现在组装画像、当天饮食明细、最近 14 天每日营养、最近周期总结、营养标签和推荐分类。

周期总结由 `POST /internal/agent/period-summaries/complete` 写回 MySQL；Python 的 `PeriodSummaryChain` 用 LangChain 生成结构化总结，随后应使用同一摘要 embedding upsert 到 `agent_long_memory`。MySQL 是可审计的结构化来源，Qdrant 是语义召回副本。

## LangChain 与 LangGraph 取舍

当前实现使用 LangChain 的 `ChatPromptTemplate`、`ChatOpenAI`、`with_structured_output` 和 Runnable 管道，已经覆盖模型调用、提示词管理和结构化输出。暂不引入 LangGraph，是因为当前流程是固定线性的；当后续加入并行检索、人工审核、模型重试分支、断点恢复或多 Agent 协作时，再把这些方法拆成 LangGraph 节点，可以避免为了使用框架而增加不必要的状态复杂度。

## 冲突处理原则

模型输出永远是候选结果。Java context 中的饮食数量、营养数值、版本号和可推荐分类是权威事实；规则层重新计算健康分和风险摘要。`conflict.py` 会过滤模型虚构的分类，并用规则结果覆盖模型返回的分数和风险。这样即使模型说“蛋白质已经充足”，但 Java 数据计算仍不足，最终反馈仍以结构化业务事实为准。

## 代码导览

- `main.py`：进程入口，只启动 Worker。
- `agent/config.py`：环境变量和 Worker 身份配置。
- `agent/schemas.py`：Java context、租约和完成结果的 Pydantic 合同。
- `agent/java_client.py`：封装 API 文档第 10 节的五个内部接口。
- `agent/rules.py`：确定性营养汇总和评分，保证核心结果可解释。
- `agent/memory.py`：Qdrant 访问边界，强制按用户过滤；embedding 由后续 provider 注入。
- `agent/worker.py`：领取、读取、分析、提交和失败上报的编排层。

启动 Worker（默认不连接 Qdrant）：

```bash
cd healthy_agent
python main.py
```

生产部署前只需配置 `OPENAI_API_KEY` 并打开两个开关；`MemoryStore` 会使用 `OpenAIEmbeddings` 生成向量，不使用固定零向量。
