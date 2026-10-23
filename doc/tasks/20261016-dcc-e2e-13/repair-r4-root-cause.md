# R4 第一阶段：响应超时根因诊断

## Current Status

in_progress — 第一阶段诊断已交付，dispatch HOLD，等待 Root 审核修复范围；未进入修复、RED/GREEN 或整任务收尾。

## 目标、约束与结论

目标：区分 R3-04（E13/E18 培训确认）与 R3-01（E03 主管签名及原任务读取）的响应超时，确定可证实的等待链和最小后续验证边界。遵循 ponytail，仅修改本报告；实际分支已核对为 `int_qms`。本阶段无浏览器/E2E、生产代码修改、构建、服务重启、数据库操作、Git 写入、jcmd 或子 Agent。

**已确认的是产品请求超时现象，以及培训确认存在同步提交后处理链；尚未锁定某个实际耗时调用，也不能证明 E03 与培训确认同根。** 不能将 30 秒客户端终止解释为数据库回滚、死锁或签名失败。E03 签名结果继续 UNKNOWN，禁止补签。

里程碑：既有证据核对完成；直接事务、事件、消息链静态追踪完成；运行时阻塞点未证实；修复范围待 Root 审核。这里的源码行号对应本次本地只读快照，不替代运行 jar 与源码映射证明。

## 证据与分类

下表时间统一为 2026-10-20 UTC（原应用日志为上海时间 UTC+8）；不把日志时间误写为本机 America/New_York 时间。

| 用例/对象 | 请求及终态 | 结论 |
|---|---|---|
| E13 Retain 4421，BPM `957e05b4-cc9a-11f1-ada5-b082e25ec548` | 15:34:31.743 → 15:34:45.989，HTTP 200，14,246ms | 同一路径存在成功但较慢的对照 |
| E13 Adjust 4424，BPM `056119c7-cc9c-11f1-ada5-b082e25ec548` | requestId 1012，15:42:07.423 → 15:42:37.435，约30,012ms，ERR_ABORTED，无 response 事件 | 页面 Axios 超时；之后对原对象只读刷新可见 ACTIVE/A1、唯一培训附件、上传者及 BPM 事实。业务落地和响应失败并存 |
| E18 A1 4419，BPM `86ae0f3b-cc9a-11f1-ada5-b082e25ec548` | 15:27:13.727 → 15:27:43.842，30,115ms，ERR_ABORTED | 原对象只读刷新可见 ACTIVE/A1 和培训事实 |
| E18 A2 4423，BPM `40121560-cc9b-11f1-ada5-b082e25ec548` | 15:32:41.214 → 15:33:11.228，30,014ms，ERR_ABORTED | 原对象只读刷新可见 ACTIVE/A2 和培训事实 |
| E03 A2 4392，BPM `9a6190f9-cc93-11f1-ada5-b082e25ec548` | 主管首次签名 14:36:45.134 → 14:37:15.140，30,006ms，ERR_ABORTED，无 response | 签名 UNKNOWN；没有后续持久化成功证据，不可套用培训确认结论 |
| E03 原任务读取 | 14:39:36.243/.244，字典 GET `/admin-api/system/dict-data/simple-list`、权限 GET `/admin-api/system/auth/get-permission-info` 均终止 | 超时已扩展到任务初始化依赖；不能仅用该文件行锁解释，更不能据此证明整个后端死锁 |

证据入口（均为既有文件）：

- 本例 `rerun-20261020-r3-round3/{handoff.json,network-events.jsonl,verification-report.md,Adjust-timeout-readonly-dom.txt,Adjust-final-dom.txt}`。附件名 `DCC241020C13R3R3Adjust-training.pdf`；Owner O、Uploader U 契约通过，未重复提交。
- `doc/tasks/20261016-dcc-e2e-18/rerun-20261020-r3-round3/{handoff.json,verification-report.md}`。两次培训确认均单次 POST，无重试补偿动作。
- `doc/tasks/20261016-dcc-e2e-03/rerun-20261020-r3-round1/{verification-report.md,signature-request-events.json,network.jsonl}`。此前 A1 文控签名 14:34:42.708 → 14:34:50.118 正常 HTTP 200。
- Root `doc/tasks/20261016-dcc-24-e2e-coordination/rerun-r3-issues.md`：R3-01 / R3-04；此前 jcmd 空输出不构成线程栈证据。

分类：E13/E18 为已确认的产品响应体验/请求可靠性问题，实际后端耗时根因 OPEN；E03 为产品交互路径超时及结果不明，故障层级仍 OPEN。三例没有足以认定账号、权限或夹具缺失导致本次超时的证据；正常签名/培训对照与后续业务事实反对直接归咎于这些因素，但不能排除未采集的环境问题。E13 脚本中 `require` 不可用、已登录账号重复登录定位失败，是独立脚本问题；培训对话框隐藏断言的后续等待不是 Axios 超时根因。旧 4366 事件继续 OPEN，不用新对象成功覆盖旧事件。

## 直接代码链与等待边界

以下路径以 `IntRuoyiBackend/` 为根；DCC 简称路径为 `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/`。

1. `DccControlledFileWorkflowServiceImpl.java:2032` 的 `@Transactional uploadTrainingRecord` 在 `2035` 获取行锁；原生流程由 `2016` 的 `lockNativeApplicantFile` 先锁 master、后锁 tenant/file。校验原始上传者及 BPM 上下文、绑定培训附件后，`2080` 直接调用 `bpmTaskService.triggerTask(...,"TRAINING")`。这是同步调用，不是提交后台任务后直接返回。
2. `yudao-module-bpm/.../service/task/BpmTaskServiceImpl.java:1671` 的 `triggerTask` 查询执行实例并直接 `runtimeService.trigger`。流程完成事件进入 `DccControlledFileFinalizationServiceImpl.java:199/404` 的普通审批完成路径；按具体流程进入版本正式化、生效处理。该类 `967` 为版本正式化实现，`1074` 为其中源文件内容读取位置。文件读取、正式化和锁等待都在候选耗时范围；没有实际调用耗时证据，不能断言 PDF 处理就是本次根因。
3. `DccControlledFileLifecycleService.java:483` 在 ACTIVATED 时于事务内受理持久化分发批次，`491–494` 注册 `afterCommit` 并直接执行 `distributionBatchService.processAutomaticBatch(batchId)`。没有在该回调入口切换到后台执行。业务提交完成后，请求线程仍可能等待分发。
4. `DccDistributionBatchService.java:33/46/49` 处理正式源和冻结接收人，事务内先锁 file、再锁 batch；`238` 的事务模板明确为 `REQUIRES_NEW`。`DccControlledFileMessageDeliveryService.java:65–72` 注册的 `afterCommit` 同样直接执行 `sendMessageJob`；`91–99` 的新事务先固化投递尝试，`133` 同步调用平台幂等消息发送 API，再记录完成结果。多条任务回调可累计耗时。ACTIVE、培训时间和分发业务时间均不是整个 HTTP 请求结束标记。
5. E03 不同入口：`DccControlledFileWorkflowServiceImpl.java:2124` 的签名事务，经校验、密码签名及证据绑定，`2197` 调用 BPM 审批；`BpmTaskServiceImpl.java:571/645` 在审批事务中 `taskService.complete`。主管 S 推进下一审批任务，不能直接等同于培训后流程完成/正式生效路径。
6. E03 可能的另一条提交后链：`BpmTaskServiceImpl.java:1464–1476` 的 `processTaskAssigned` 注册 `afterCompletion`，`1591` 在该回调直接 `sendMessageWhenTaskAssigned`；`.../service/message/BpmMessageServiceImpl.java:94/129` 调用普通平台站内消息发送。它与培训分发同属同步事务回调模式，但普通消息与分发幂等消息不是同一个方法。静态交集只能说明值得排查消息/连接资源，不能证明同一次资源阻塞。

锁/等待推断边界：外层提交回调仍在执行、内层新事务需要连接，可能放大连接池压力；文件/master/batch/job 的行锁也可能等待。**这些均为候选，不是已捕获的连接池耗尽、锁环或死锁。** 没有运行时线程栈、连接占用、数据库等待关系或关联请求的分段耗时，无法选定候选。E03 字典/权限 GET 同时超时还要求检查共享线程、连接、缓存或网络等待，不能只修培训文件锁。

前端 `IntRuoyiFronted/src/config/axios/config.ts:19` 为 30,000ms；与网络终止时长相符。它是观察窗口，不是根因证明，也不是建议放大超时的依据。E03 已有“签名结果未确认，勿重复签名”提示，应保留；培训成功事实也不能变成前端猜测成功或自动重提。

## 既有日志能证明什么

检查 `output/runtime/int_qms/logs/dcc24-rerunr2/` 的 `application.log.2026-10-20.0.log`（53,512 bytes）及 stdout；当前 `application.log` 为零字节是轮转事实，不代表没有日志。上海时间 22:37:15.850、23:28:00.973、23:33:48.529、23:42:20.293 等有 `DccControlledFileDetailAuthorizationGuard.bodyDenied(...:196)` 服务异常，但缺少关联对象、请求路径、请求 ID 和处理耗时。不能把邻近异常绑定到某次签名/培训请求，也不能据此改判账号错误。

未发现可关联上述对象/BPM 的请求开始—结束日志、慢 SQL、连接池等待或锁等待证据；启动显示 Druid，不能称为 Hikari 耗尽。R3 使用 jar `backend-rerun-r2-20261020-1301.jar`，既有 SHA-256 为 `709e78bd665da04cc0fde3ddffffb401a028a8835139646fa4754ec753aaa147`；本次未重打包或重新冻结完整源码。

## 最小修复边界（待审核，尚未实施）

优先在既有事务/分发/消息边界补足可关联的分段耗时证据，区分：锁前后、流程推进、正式化、提交、提交后分发、平台消息、HTTP 完成；不记录密码、签名材料或附件正文。E03 应独立标记审批及下一任务通知，并关联字典/权限请求。Root 另行批准采集方式及复现范围后，才能据实际等待点修复。

若证实提交后投递占满响应时间，最小候选是复用已有持久化批次/消息任务及其重试机制，将已受理投递从请求返回关键路径移出；先审计已有消费者是否足够，再决定是否需要改动。事务内必须继续校验、固化证据和可靠受理；保留租户、幂等、失败可见及重试。不能仅把所有回调改异步、取消行锁、吞异常或依赖一次内存排队。若实际耗时在提交前，此候选不能解决根因。

E03 仅在独立证据证实同一共享等待点后复用修复；否则保持单独问题。无需修改 Owner/Uploader 契约、额外插入 Owner 节点、扩大前端超时或补签。当前没有足够依据直接给出生产补丁。

## Given / When / Then 与 RED/GREEN 计划

本阶段仅设计，**未执行测试，不能宣称 RED/GREEN 已完成**。修复阶段先在现有测试中建立能稳定失败的根因用例，再改生产代码：

| 场景 | Given / When / Then | RED 及 GREEN 判断 |
|---|---|---|
| 培训提交与响应边界 | Given 合法 U、准确 BPM、唯一培训附件，投递端用受控栅栏阻塞；When 单次确认；Then 按审核后的契约，事务可靠提交、响应无需等待非必需投递，投递稍后恰好处理一次 | 先证明现有同步回调确实阻塞返回；若 Root 确认该边界应解耦，再以同一用例失败为 RED，修复后为 GREEN。用栅栏与有界等待，不用睡眠猜耗时 |
| 持久化与失败恢复 | Given 投递失败/进程恢复、重复消费及跨租户数据；When 消费原已受理任务；Then 失败可查、可重试、不重复、不越租户；核心校验失败仍整体拒绝 | 扩展 `DccLifecycleDistributionOutboxTransactionTest`、`DccMessageDeliveryTransactionIntegrationTest`、`DccMessageDeliveryIdempotencyTest`；证明可靠受理与真实事务边界，不能只验 mock 调用次数 |
| 主管 S 签名 | Given 原生主管任务及下一审批人的消息发送受阻；When 单次签名推进；Then 核验签名、任务、通知的事务边界及返回等待，异常结果保持未确认且无自动补签 | 优先复用 `DccNativeUploadSupervisorWorkflowTest` 和 BPM `DccWorkflowSignedTaskActionGuardTest`；只有复现出实际等待点才作为该缺陷 RED，不能拿培训测试代替 |
| 资源等待假设 | Given 与已采集事实一致的连接/锁条件；When 培训或主管签名与只读初始化请求并行；Then 观测明确等待资源及释放，不发生环路或跨事务自等待 | 若日志证明资源等待，再补最小事务集成回归；无事实前不先改连接池、不制造任意负载并宣称复现 |

GREEN 后按授权执行针对性构建/测试；真实页面 E2E、运行时采集、服务操作均需另行明确授权。本报告文档验证：核对证据文件和源码锚点、表格/状态/约束结构；没有将业务字段当成提交/HTTP 结束时间。尚缺运行时卡点证据，Root 审核前继续 HOLD。

## R4 只读补查：并发 GET 与既有运行日志

Root 认可首阶段未定位实际等待点，未批准 async 或延长 timeout。本次继续仅只读本地文件，补查自然请求元数据，不读取业务响应 JSON、不发新请求、不操作 JVM。仍为 `in_progress` / dispatch HOLD，完成本次分析后停等 Root。

### 关联方法与数据限制

E13 使用 `network-events.jsonl` 的 request / response / requestfinished / requestfailed；其请求起始 ID 无重复。E18 只提取 `result.json.network`：经 `session.cjs` 确认该字段由页面事件记录 actor、method、path、startedAt、status、durationMs、finishedAt/failedAt，没有响应正文。没有读取 trace 的响应资源。

E03 的 `network.jsonl` 存在同一 actor 下 ID 复用，以及部分没有起始记录的终态事件。**不能单按 requestId 连接，更不能据此报告数分钟 GET。** 本补查对 E03 同时核对 method/path，仅采用唯一完整起止配对；缺起点、重复或无法唯一配对的记录不计耗时。先前试算中出现的 188/215 秒跨请求配对已排除，不构成结论。时间仍统一 UTC；“同时”指自然请求区间相交，不表示同一服务线程或同一阻塞资源。

### 可证明的窗口对照

| 业务请求窗口（UTC） | 同窗口自然 GET 元数据 | 可证明结论 |
|---|---|---|
| E03 签名 14:36:45.134–14:37:15.140 | 在完整可唯一关联的 E03 元数据中，未找到窗口内发起的无关 GET 可作并发耗时对照 | 缺采样，不能说当时其它 GET 正常，也不能说全部卡住 |
| E03 随后读取 | 14:38:41.430–.554 未读数 GET，200，124ms；14:39:06.165 同时发起的字典与权限 GET，分别于14:39:36.243/.244 ERR_ABORTED，30,078/30,079ms，无 response | 两个无关 GET 确实在同一个后续窗口超时；它们在签名终止111秒后才发起，不能写成与签名请求同时变慢。此前未读数还能快速返回 |
| E18 A1 15:27:13.727–15:27:43.842 | E13 会话中7条 GET 区间相交；审批任务页164ms、模块40ms；未读数15:27:39.226–15:27:41.079，200，1,853ms | 有轻度变慢对照，但不是所有请求都等待30秒 |
| E18 A2 15:32:41.214–15:33:11.228 | E18 S 未读数15:33:03.663–.680，200，17ms；E13 S 未读数15:33:07.004–.016，200，12ms | 同窗口不同会话仍快速响应，不支持全后端持续停顿30秒 |
| E13 Retain 15:34:31.743–15:34:45.989 | E13 C 未读数15:34:34.080–.095，200，15ms | 业务请求14.246秒时，无关 GET 可很快返回 |
| E13 Adjust 15:42:07.423–15:42:37.435 | E13 U 未读数15:42:23.961–15:42:24.771，200，约810ms；E18 另一个文件4426的72条 GET 区间相交，其中任务页1,341ms、分类2,065ms、用户简表3,900ms、文件详情2,423ms、预览3,402ms，均200 | 存在其它业务与通用 GET 的秒级耗时，未观察到它们跟随培训确认一起30秒终止；缺同端点稳定基线，不能宣称这些 GET 相比平时退化多少 |

E13 Adjust 窗口合计73条相交 GET，7条耗时至少1秒，其余均不足1秒；这只是所记录三个会话集合的样本，不代表服务全量流量。其它业务并行存在是事实，不等于已证明它制造资源竞争。30秒浏览器终止也不能说明对应服务端线程在同一时刻停止工作。

### 运行日志与资源证据

本次重新读取同一 runtime 的四份文件：`application.log.2026-10-20.0.log` 53,512 bytes，`application.log` 12,982 bytes，`stdout.log` 75,448 bytes，`stderr.log` 81 bytes。当前 application 已写入上海时间10月21日00:00后的其它日志，纠正首阶段“当前零字节”的时点描述；历史窗口主要仍在 rotated 文件，不能用当前文件的大小推断历史请求状态。stdout 与 application 中相同日志只计一份事实。

- **连接池**：Druid 启动成功有记录。rotated application 第266行 / stdout第300行，上海时间10月20日15:09:07.810（UTC07:09:07.810）出现 `WebAppStat uriSessionMapFullCount is full`。这是 Web 统计映射容量警告，早于目标窗口数小时；不是数据库连接池已满证据。未找到目标窗口关联的获取连接超时或连接占用/等待计数。
- **SQL/锁**：未找到 `slow sql`、`Lock wait timeout`、`Deadlock found` 等证据，也没有目标请求关联的 SQL 开始/结束。源码 `application-local.yaml:26–50` 配置 Druid stat、慢SQL100ms、max-active20、max-wait60000；这是源码配置，未核实运行时生效值或 logger 过滤。即使生效，30秒客户端中断也可能早于60秒连接获取失败日志；没有慢SQL日志不能排除尚未获得连接、尚未结束的SQL或其它等待。
- **GC/JVM**：这些日志中未发现 GC pause / Full GC / OOM 记录，指定 runtime 文件检索未发现现成 `.jfr` 或 GC 日志；stderr 仅有 `JAVA_TOOL_OPTIONS` 的 UTF-8 和上海时区提示。已检索的启动脚本与 YAML/XML 未见 GC/JFR/JMX remote/accesslog 配置匹配。这不证明 GC 没发生，也不证明 JVM 未从其它入口开启诊断；没有采集就无法回溯暂停时长。
- **消息调用**：没有按目标请求关联的消息调用开始/结束耗时，也没有该窗口的 SocketTimeout/Read timed out 异常。现有直接代码证明同步发送调用存在，不能证明实际慢在消息发送，更不能把“平台消息 API”静态名称当作已经证明的外部 HTTP 调用。

因此，当前可排除的是“所有自然 GET 都统一等满同一个30秒窗口”的描述；不能排除间歇资源竞争、单请求等待、短暂停顿或不同路径分别阻塞。没有证据将 E03 和 E13/E18 合并为单一根因。

### 下一次原流程运行的最小观测方案（仅设计，未执行）

不增加遥测框架、不修改代码、不放大超时。由 Root 批准下一次真实页面运行及所需诊断权限后实施；不要重试 E03 UNKNOWN 签名或任何结果不明的培训提交。

1. **运行前确认关联与可采能力**：核对实际分支端口、PID、jar及 JVM 启动时间，统一 UTC 与应用上海时间；保存现有日志文件名、字节偏移和轮转边界。只读核实是否已提供本地 JMX / Druid 指标及访问权限，不猜地址、不开放端口。当前尚未证明这些接口可用。页面仍只记录方法、去查询参数的路径、单请求唯一ID、时间、状态、结束/失败、浏览器网络 timing；不保存 token、请求签名/密码或响应 JSON。
2. **单次页面动作，保留并发对照**：只执行该轮已授权原流程中的一次业务动作；记录点击与 request 起点。同账号自然轮询及其它页面自然 GET 作为对照，不另行高频发探测请求。一个长请求未终态前不重复点击；浏览器终止后保留原对象核验，签名未知继续 UNKNOWN。
3. **最关键缺口是正在等待的 Java 栈**：建议在该请求自然等待约10/20/28秒时，各取得一份有时间戳的 JVM 全线程栈并保留锁信息。下一轮若 Root 单独允许，可用同一 JDK 的 `jstack -l <已核实PID>`，不用 jcmd；本轮没有执行。jstack同样依赖attach权限，不能假设它能绕过此前jcmd失败。若已有受权本地 JMX，则可用 `ThreadMXBean.dumpAllThreads(true,true)` 替代，避免另开诊断服务。采集失败即准确记录，不循环attach、不强制dump、不自动重启。三份栈可区分请求仍在 Flowable/正式化、Druid取连接、JDBC/socket读取、对象锁、消息同步调用或提交回调；不能仅凭一个 WAITING 状态下结论。
4. **有现成能力才补最少计数**：与栈同一时刻保存 Druid Active/Pooling/等待线程及累计等待、JVM GC CollectionCount/CollectionTime（单调差值）、堆/线程数；不清零 Druid 指标，不抓SQL正文。GC计数差只说明发生过GC，精确停顿仍需要已有GC日志/JFR事件。若栈指向 JDBC，锁拥有者/数据库等待关系仍可能需要另行授权的数据库只读诊断；本轮没有执行或授权该步骤。
5. **以栈和请求关联后再决定修复**：保留日志增量到客户端结束后至少一个已批准的有限观察窗口，防止漏掉“客户端已断开、服务端仍处理”的终态。用单次请求时间、唯一对象、栈中请求线程及自然GET对照交叉关联；若多个同路由请求同时存在且现有日志无请求线程映射，明确标记关联不唯一，不凭线程名猜对象。必要时下一轮选择该动作单次提交来降低歧义，不限制其它正常业务。

该方案的优先级是“现有请求元数据＋日志增量＋3份线程栈”，不是先开完整JFR。若 Root 继续禁止所有 JVM attach、且没有预先开放的 JMX/现成录制，**当前证据通道无法保证直接定位一次等待**；只能记录现象与缺口。需要 Root 在下一轮批准一种可用的栈采集通道，或另行批准提前配置JFR/GC日志；后者可能涉及启动配置/重启，均不在本轮范围，不能默认为已授权。

补查验证：已排除 E03 ID复用导致的假长请求；逐项核对上表起止、方法、路径、HTTP状态/失败事件；日志按时区和轮转区分。本次只更新本报告，无测试、浏览器、jcmd/jstack/JMX请求、DB、构建、重启或Git写入。结论交回 Root，等待后续范围审核。

## R4 栈采集能力预检（Root 单独授权的一次尝试）

授权仅限：核实重启后的 QMS PID38176 / 48061、同 jar 后，以匹配 JDK 的 `jstack -l 38176` 尝试一次、最长10秒；禁止业务动作、E2E、jcmd、强制dump、开放JMX、修改启动或再次重启。此前章节的“未执行jstack”描述截至此前只读分析阶段，本节记录本次新增授权及结果。

- 身份核实通过：分支 `int_qms`；48061监听归属38176；进程启动时间 `2026-10-20T16:59:03.6041490Z`；Java和jstack同属 `.runtime/tools/jdk-17/bin/`，Java版本17.0.20.1；jar SHA-256为 `709e78bd665da04cc0fde3ddffffb401a028a8835139646fa4754ec753aaa147`。启动采集前再次核对PID启动时间与端口归属，没有输出完整命令行或环境变量。
- 唯一采样：UTC `2026-10-20T17:08:53.0026318Z` 开始，`17:08:54.4115994Z` 结束；纽约本地时间13:08:53–13:08:54 EDT。采集耗时1,343ms，退出码0，未触发10秒终止；包含完整dump标记，采集可用。
- 统计：209个含tid/nid的线程标题；其中177条Java线程状态记录，RUNNABLE79、WAITING69、TIMED_WAITING29、BLOCKED0。其余32个标题无该Java状态行，不能全部归入RUNNABLE或将209当成Java业务线程数。
- 原始stdout 218,994 bytes、stderr 0 bytes，仅保留在本任务 `diagnostics-private/r4-jstack-preflight-38176/`；`collection-metadata.json`记录时间、身份与统计。原始栈未打印、未复制进公共报告，不视为已脱敏材料。

结论仅为**一次栈采集能力预检成功**；本次没有业务提交，不证明培训耗时根因，也不证明以后每次attach必然成功。没有循环attach、jcmd、JMX请求、强制dump、重启、生产修改、DB或Git写入。10/20/28秒采样及单次全新真实页面培训动作仍等待Root另发业务gate；旧UNKNOWN任务绝不重放。状态保持in_progress / HOLD。

## R4 全新任务观测器准备（待 Root 业务 gate）

检查现存 E13 任务资产及正式页面入口后，没有证据表明已有一条可确认为本轮新建、尚未培训提交的任务；不得把 E13/E18 既有记录或任何 UNKNOWN 提交当作新任务复用。新对象须在 gate 后沿真实上传、审批、培训工作台入口推进，未执行任何业务动作。

已在 [R4 观察器准备目录](r4-observation/task.md) 建立单次观察器和采集脚本。观察器只监听浏览器自然产生的目标培训 POST，保存方法、路径（去查询参数）、HTTP状态与时间；不读请求/响应正文。仅当该请求仍未 `requestfinished`/`requestfailed` 时，在10/20/28秒触发一次匹配运行时的 `jstack -l` 采集尝试；响应头到达单独记状态，不会提前终止仍在传输的请求。任一采集失败后停止后续尝试；前一采集尚未结束则跳过该秒并记明，不并行或补采。日志增量按基线字节偏移保存至私有目录，轮转或身份缺失导致不完整时明确失败，不截断冒充完整。

离线自检覆盖三次阈值、请求终态取消、响应头先到仍采样、采集失败停止、禁止并发attach及日志轮转增量；PowerShell 解析通过。没有启动浏览器、真实 jstack、业务提交或读取接口响应内容。线程关联仍有限：栈不自带请求路径/文件ID；在没有既有关联ID日志时，时间窗加唯一页面POST可提高对应可信度，但不能保证线程唯一映射。结果不可用于判定根因。

本阶段只完成观测准备，真实新任务和采集执行仍 HOLD，等待 Root 单次业务 gate。旧 UNKNOWN 训练/签名请求不重放；无生产变更、服务重启、数据库/Git操作。
