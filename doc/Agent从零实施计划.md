# 健康饮食 Agent 从零实施计划

## 1. 目标架构

系统分为三个边界：

1. 前端负责用户手动触发分析、展示任务状态和最终结果。
2. Java 服务负责用户鉴权、饮食事实、营养聚合、任务状态机和结果落库，是业务数据的唯一写入方。
3. Python Agent 负责领取任务、RAG 检索、规则计算、大模型推理和生成结构化候选结果，不直接写业务数据库。

第一版使用数据库任务队列即可，不必立即引入 RabbitMQ。前端调用 Java 创建任务后可以离开页面；Python Worker 在后台处理，前端通过轮询恢复任务状态。

## 2. 阶段一：补齐数据版本基础

### 2.1 建立每日输入版本表

`health_analysis_task.input_revision` 和 `health_analysis.input_revision` 保存的是分析所使用的版本，还需要一张表保存“当前版本”：

```sql
CREATE TABLE nutrition_daily_state (
  user_id bigint NOT NULL,
  record_date date NOT NULL,
  revision bigint NOT NULL DEFAULT 0,
  create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id, record_date),
  CONSTRAINT ck_nutrition_daily_revision CHECK (revision >= 0)
);
```

新增或删除 `diet_record` 时，在同一个 Java 数据库事务中：

1. 锁定或创建 `nutrition_daily_state(user_id, record_date)`。
2. 修改饮食记录。
3. 重新计算该日 `nutrition_record`。
4. 将 `revision = revision + 1`。
5. 一起提交；任一步失败则全部回滚。

验收标准：同一用户同一天在多个设备并发增删记录后，聚合结果完整，版本严格递增且不丢失更新。

### 2.2 保存分析使用的版本

创建任务时，把当前 `nutrition_daily_state.revision` 写入 `health_analysis_task.input_revision`。Agent 成功完成后，再把任务版本复制到 `health_analysis.input_revision`。

旧分析无法确定版本时使用 `0`，只表示历史未知版本，不能认为它是当前结果。

## 3. 阶段二：先完成不依赖大模型的饮食模块

实现 API 第 6.1 至 6.3：

1. 新增饮食记录。
2. 按日期查询饮食记录。
3. 删除饮食记录。
4. 根据平台菜品分类标签、菜品营养 JSON 或手工标签重建每日聚合。

这一阶段先确保输入数据正确。Agent 再复杂，也不能弥补业务事实不准确。

建议把营养计算拆成纯函数或独立服务，例如：

```text
DietRecord[] -> NutritionSummary
```

这样 Java 聚合逻辑和 Python Agent 输入可以用固定样例交叉验证。

## 4. 阶段三：实现 Java 异步任务服务

按照 API 文档实现用户侧接口：

```text
POST /health-analysis-tasks
GET  /health-analysis-tasks/{taskId}
GET  /health-analysis-tasks
GET  /health-analyses
GET  /health-analyses/{analysisId}
```

创建任务的关键规则：

1. `Idempotency-Key` 写入 `request_id`。
2. 从每日状态表读取当前 `input_revision`。
3. `model_version` 由服务端配置决定。
4. 相同用户、日期、版本和模型命中唯一索引时返回已有任务。
5. 接口立即返回 `PENDING`，不在 HTTP 请求内调用 Python 或大模型。

实现任务状态机：

```text
PENDING -> RUNNING -> SUCCEEDED
                   -> FAILED
                   -> STALE
RUNNING -> PENDING
FAILED  -> PENDING（用户手动重试）
```

所有更新必须使用带旧状态的条件 SQL，并检查影响行数。

## 5. 阶段四：实现 Java 内部 Agent API

依次实现：

```text
POST /internal/agent/health-analysis-tasks/claim
GET  /internal/agent/health-analysis-tasks/{taskId}/context
POST /internal/agent/health-analysis-tasks/{taskId}/heartbeat
POST /internal/agent/health-analysis-tasks/{taskId}/complete
POST /internal/agent/health-analysis-tasks/{taskId}/fail
```

### 5.1 领取任务

在短事务中使用：

```sql
SELECT id
FROM health_analysis_task
WHERE status = 'PENDING'
ORDER BY create_time, id
LIMIT 1
FOR UPDATE SKIP LOCKED;
```

随后更新为 `RUNNING`，写入 `lease_owner`、`lease_until` 并递增 `attempt_count`。事务提交后才能返回任务。

### 5.2 上下文组装

Java 根据任务中的 `user_id` 和 `analysis_date` 返回：

1. 当日饮食记录。
2. 营养标签聚合。
3. 菜品营养详情。
4. 可推荐分类及分类标签。
5. `input_revision` 和 `model_version`。

不要让 Python 传任意用户 ID 查询普通用户接口。

### 5.3 完成任务

Java 在一个短事务中锁定任务行，校验 Worker、租约、状态和当前输入版本，然后写入：

1. `health_analysis`。
2. `diet_recommendation`。
3. 任务的 `SUCCEEDED` 状态和 `health_analysis_id`。

输入版本已变化时更新为 `STALE`，不发布旧分析。重复完成请求直接返回原 `analysisId`。

## 6. 阶段五：建立最小 Python Worker

推荐技术栈：

```text
Python 3.12
httpx
pydantic
tenacity
structlog 或标准 logging
pytest
```

第一版不要立刻加入复杂 Agent 框架。先写一个明确的 Worker 循环：

```text
领取任务
  -> 获取上下文
  -> 规则计算
  -> 生成结构化结果
  -> 提交结果
  -> 失败时分类并上报
```

使用 Pydantic 定义 Java 上下文和 Agent 输出，拒绝模型返回的任意文本直接入库。Worker ID 使用“主机名 + 进程 ID + 随机启动 ID”，同一进程生命周期保持不变。

先让 Python 返回固定或纯规则结果，跑通整个异步闭环，再接 RAG 和大模型。这能快速区分“任务系统问题”和“模型问题”。

## 7. 阶段六：加入确定性规则层

在调用模型之前，由 Python 计算可验证的结构化事实，例如：

1. 热量、蛋白质、脂肪、碳水总量。
2. 各营养标签出现次数。
3. 缺少或过量的项目。
4. 候选推荐分类和基础分数。

模型只负责结合规则结果和检索知识生成解释、风险摘要及推荐理由。健康评分的核心计算尽量保持确定性，并记录规则版本。

建议将版本组合成：

```text
diet-agent-v1 + rules-v1 + prompt-v1 + knowledge-v1
```

任何会显著改变结果的规则、提示词或知识库发布都应更新 `model_version`。

## 8. 阶段七：建立 RAG

先准备小而可信的营养知识库，每个知识片段至少包含：

```text
chunk_id
title
content
source
applicable_population
tags
published_at
knowledge_version
```

处理流程：

1. 清洗原始文档。
2. 按语义切片，保留标题和来源。
3. 生成 embedding。
4. 写入向量库。
5. 根据规则层产生的风险和目标构造检索查询。
6. 召回后按相关度、适用人群和来源可信度重排。
7. 只把少量高质量片段放入模型上下文。

本地原型可使用 Chroma 或 pgvector；如果业务数据库仍是 MySQL，Chroma 更容易快速隔离实验。不要把用户每日饮食事实写入向量库。

## 9. 阶段八：接入大模型和结构化输出

Prompt 至少分为：

1. 固定系统约束：角色、禁止诊断疾病、不能虚构事实。
2. Java 提供的结构化饮食事实。
3. Python 规则层的计算结果。
4. RAG 检索片段及来源。
5. 严格的 JSON 输出 Schema。

模型输出先经过 Pydantic 校验，再进行业务校验：

1. 分数范围为 0 至 100。
2. 推荐分类必须来自 Java 返回的允许集合。
3. 推荐分类不得重复。
4. 文本长度受限。
5. 输出不得包含医疗诊断或无法从上下文支持的事实。

解析失败可以进行一次带错误信息的修复调用；仍失败则按可重试错误上报。

## 10. 阶段九：前端任务体验

用户点击“开始健康分析”后立即获得任务 ID。前端：

1. 前 15 秒每 2 秒查询一次任务。
2. 之后每 5 秒查询一次。
3. 页面隐藏时暂停轮询，重新可见时立即查询。
4. 用户离开页面不取消任务。
5. `SUCCEEDED` 后跳转或刷新分析详情。
6. `STALE` 时提示“饮食记录已变化，请按最新记录重新分析”。
7. `FAILED` 时提供手动重试入口。

饮食记录变化只递增版本，不自动触发昂贵 Agent。后续可以增加用户主动开启的每日定时分析或修改停止十分钟后的防抖分析。

## 11. 阶段十：可靠性、安全与观测

至少记录以下指标：

```text
PENDING 任务数量
任务排队时间
Agent 执行时间
成功、失败、过期和重试数量
模型请求耗时与 token 用量
RAG 召回数量和引用来源
各 model_version 的结果分布
```

安全要求：

1. 内部 API 使用独立服务凭证，生产环境再增加 mTLS。
2. 日志不记录 JWT、模型密钥、完整提示词和用户敏感信息。
3. Python 不直接连接或修改业务数据库。
4. 模型输出永远视为不可信输入，由 Java 进行最终业务校验。
5. 健康建议明确是饮食参考，不代替专业医疗诊断。

任务租约过期扫描可以先由 Java 定时任务实现，但任务回收必须使用数据库条件更新；部署多实例时不能依赖 Java 本地锁保证唯一执行。

## 12. 推荐实施顺序和里程碑

### 里程碑一：数据正确

- 完成饮食记录、营养聚合和每日 revision。
- 验证并发增删不会破坏聚合。

### 里程碑二：异步闭环

- Java 可以创建、领取、查询、完成和失败任务。
- Python 固定结果 Worker 能够端到端运行。
- 前端可以离开页面后重新恢复任务状态。

### 里程碑三：结果可信

- Python 确定性规则层完成。
- 结构化输出和 Java 二次校验完成。
- 输入变化时任务正确进入 `STALE`。

### 里程碑四：智能增强

- RAG 知识库、引用来源和重排完成。
- 接入大模型并建立 prompt/model/knowledge 版本管理。

### 里程碑五：可运维

- 租约恢复、失败重试、限流、指标和告警完成。
- 再根据负载决定是否从数据库轮询升级为消息队列加事务 Outbox。
