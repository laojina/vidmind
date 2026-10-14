# VidMind 工程风险与验证清单

审查日期：2026-10-14。依据当前源码静态阅读和本地单元测试，重点覆盖上传、认证、消息任务、解析、Agent、检索、存储和事件链路。不是全量漏洞证明；没有真实多中间件故障注入、长视频压测或付费模型端到端验收。

后续增量更新：锁竞争已改为请求重投，空窗口遍历已改为只访问有效核心窗口，持久结果与任务代次终态可拦截重复执行；下表对应项保留最初审查背景。新状态机复用检查点表，尚需真实 MySQL 条件更新、Broker DLQ、进程崩溃和租约失效的集成测试。RUNNING 恢复依赖 Redisson 锁，没有 worker fencing；模型调用与状态更新仍不是原子事务。消息登记与 MQ 投递也有不确定窗口，仍需要 Outbox。旧分块不会自动重建，任务代次记录尚无专门归档策略。

## 本次已修复

| 问题 | 触发条件和影响 | 修复与验证 |
| --- | --- | --- |
| 证据来源标签匹配过宽 | `EvidenceVerificationService.supported` 曾用子串判定，未知来源标签只要包含已知频道名称就可能通过 | 仅接受 ASR、OCR、ASR+OCR；新增伪标签拒绝、组合来源和时间窗右边界测试 |
| 登录服务未检查密码上限 | HTTP DTO 已限制 128 字符，但直接服务调用绕过 DTO 可继续查询缓存、数据库和计算哈希 | 服务层在外部访问前拒绝超过 128 字符的密码；测试验证 Redis 与数据库均未被调用 |
| 注册注释和代码不一致 | `AuthRequest` 注释描述至少 8 位，实际校验至少 4 位 | 修正注释；没有把当前密码策略描述为强密码策略 |

## 尚未修复或尚需验证

| 优先级 | 源码位置 | 具体边界、影响与下一步 |
| --- | --- | --- |
| 高 | `ChunkUploadService.complete` | MySQL 保存媒体后、Redis completed 标记前若异常，重试可能再次创建媒体。合并锁只防并发，不保证跨系统原子性。应持久化 uploadId 唯一约束/合并状态，并设计补偿和故障注入测试。 |
| 高 | `ChunkUploadService.uploadChunk/complete` | 上传与合并没有共用同一把锁；同一分片在合并读取时可被重传覆盖。需要分片不可变、校验和及 finalize 状态。 |
| 高 | `YtDlpUtils.validatePublicHttpUrl` | 首次 DNS 检查不能约束子进程再次解析和重定向。公网链接可转向内网，生产应隔离下载执行器并限制网络出口，不能宣称已完全防 SSRF。 |
| 高 | `AnalysisDispatchService.submit` | Redis 占位与 MQ 投递不是事务；消息已发送但客户端得到错误时，占位被删，下一次请求可能重复投递。需 Outbox/任务表与投递状态，消费者幂等仍是最后防线。 |
| 高 | `VideoAnalysisConsumer.onMessage` | 抢不到内容/目标锁时直接返回，相当于确认这条消息。若这是不同 mediaId 的同内容任务，其结果未必落在自身检查点。需并发两个媒体 ID 的集成测试，再决定等待、重投或复用完成通知。 |
| 中 | `QdrantVectorStore.ensureCollection` | 集合存在即标记 ready，未校验向量维度/距离；切换 Embedding 模型可能持续写入失败并降级。应校验集合配置或按模型版本使用新集合。 |
| 中 | `LongVideoContextService.withinBudget` | 首个片段无条件保留，单个超长转写可超过 24000 字符限制。应按安全文本边界截断或继续细分，避免破坏证据引用关系。 |
| 中 | `VideoChunkingService.build` | 根据最后 startMs 遍历从零开始的所有 5 分钟窗口；极端稀疏、超大时间戳导致大量空循环。可按有效片段分组，保持时间范围语义。 |
| 中 | `ChunkUploadService` | 410 片 × 5 MiB = 2050 MiB，超过普通上传 2048 MiB 口径，合并过程中缺少总大小限制；本地临时盘可能被耗尽。应统一上限、限制每用户并发并检查总字节数。 |
| 中 | `ChunkUploadService.initialize/cleanup` | Redis 元数据一天过期，不会自动删除 MinIO 已上传分片。用户放弃上传时可能累积孤儿对象，应配置生命周期或清理任务。 |
| 中 | `MediaService.purgeRuntimeArtifacts` | Redis、Checkpoint、Telemetry、Qdrant 清理位于同一 try 内，前一步异常会跳过后续步骤。需要独立重试/清理队列。共享内容引用也需避免误删仍被复用的证据。 |
| 中 | `AuthService.recordLoginFailure` | increment 与 expire 分离；进程在两者之间退出可留下无 TTL 失败计数。应 Lua 原子写入。限流按账号，仍需入口限速以降低账号锁定与注册资源消耗风险。 |
| 中 | `TaskEventService.subscribe/publish` | SSE 无订阅数量上限，也没有持久化事件游标。大量连接可占内存，断线期间事件可丢。客户端需状态查询兜底，服务端增加配额、心跳和重连机制。 |
| 中 | `AgentTelemetry.modelCall`、`AgentLoopService` | token 是字符估算、费用默认关闭，重试和恢复的成本统计不等同真实总账单；执行轮次达到上限也可输出带警告结果。需供应商 usage、任务累计预算及质量指标。 |
| 中 | `VideoEvidenceRetrievalService`、`EvidenceVerificationService` | TopK 无相似度阈值；原文子串校验不能证明结论蕴含关系。需 Recall@K、引用精确率、人工标注和无答案样例；不得承诺没有幻觉。 |
| 中 | `VideoContextService.cancelBranches` | 未停止的分支会保留工作目录，保护正在运行的任务，但目录可能泄漏。需延迟清理、磁盘配额和子进程回收验证。 |
| 低 | `AuthService.register` | 当前允许 4 位密码，适合本地演示的兼容口径，公网服务应提高新注册强度并保留旧用户迁移方案。 |

## 旧名称迁移影响

源码、模板、日志文本、注释和运行时键已统一为 VidMind（机器标识按惯例使用小写）。旧 Git 对象与恢复备份不是当前源码，不做物理清除；备份是为了可恢复，不能承诺历史字符串已不可恢复。

- Redis PubSub 频道变更：所有后端实例应一起重启，避免新旧频道隔离。
- 浏览器目标/模式存储键变更：已有本地偏好不会自动迁移，需要重新选择。
- 新安装默认数据库和 MinIO 用户名变更：现有持久卷不会自动重建用户。保留实际运行账号，或先创建新账号并授权，再修改本地配置；不要仅改用户名导致无法连接。
- 本地 `.env`、私人脚本、日志和运行数据不属于已发布源码，不自动改凭据、不提交秘密。生产部署应单独审核迁移。

## 本地验证结果

- Java 23 环境以 `release 21` 编译，86 个主源码文件编译通过；7 个后端测试通过。
- 两项新增测试先在原实现失败，再在修复后通过。JDK 23 上启用 Lombok annotation processing；Mockito 用显式 Java agent 避免本机动态 attach 管道限制。
- 前端 `npm ci`、2 个测试与 `npm run build` 成功。
- 当前测试非常有限：没有认证端到端、MQ 重复投递、上传并发、Qdrant 故障、付费模型、Docker 启动集成验证；不把构建成功等同全链路成功。

## GitHub 审阅者可能检查

1. commit diff 是否支撑提交说明，早期是否完整基线导入，作者和日期是否可信。
2. LangChain4j 是否出现在真实 Java 调用中，RAG/Agent 到底由框架还是自研实现。
3. CI 是否通过，测试是否覆盖幂等、权限、消息重试、边界和故障恢复，而非仅正常路径。
4. README 默认配置是否与 Compose、端口、脚本、数据库迁移一致。
5. Token/密码是否进入源码或旧历史；本地密钥不可入库，已泄露令牌需撤销。
6. 所谓无幻觉、exactly-once、降低成本百分比等描述是否有实现和实验数据支撑。
7. `v1.0.0` 是否对应明确提交，功能限制是否披露，MIT 许可与真实来源归属是否保留。

## 建议的后续验收顺序

先用真实 Docker 环境验证注册、上传、异步分析、SSE 与删除；再针对 Redis/MQ/MySQL 切断与恢复、并发同内容任务做故障注入；最后用固定长视频样本和人工标准答案评估召回、引用质量、耗时和供应商实际费用。
