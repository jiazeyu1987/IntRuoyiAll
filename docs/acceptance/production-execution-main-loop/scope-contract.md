# P0 生产执行主闭环范围契约

## Purpose and Scope

本文档把 P0 后续开发重点收敛为“生产执行主闭环”：以一次“工序池提交事件”为主事实源，串联生产报工、记录本、PQC、电子签名、班组长复核、生产工单 FIFO 分配、订单工序完成和正式批记录追溯。

P0 的验收问题只有一个：一线员工或 PQC 员工完成一次正式提交后，系统能否完整回答“谁、在哪台设备、做了哪个工序、做了多少、质量结果怎样、签名是谁、进入哪个生产工单、班组长是否复核、最后如何进入批记录追溯”。

## Evidence Reviewed

- `docs/acceptance/production-line-process-pool/`：既有工序池 BDD/TDD/E2E/test-data 文档。
- `doc/tasks/20260730-production-line-process-pool-implementation/task.md`：F1/F2/F3/F4/F7/F8 已实现和验证摘要。
- `doc/tasks/20260730-process-pool-f5-f6-implementation/task.md`：审核副本和原始记录 revision 已实现和真实 E2E 摘要。
- `doc/tasks/20260731-team-leader-workbench-prd-plan/prd.md`：生产组长工作台、活跃订单、FIFO、工序完成、批记录回填口径。
- `doc/tasks/20260731-team-leader-workbench-prd-plan/p6-real-e2e-evidence.md`：生产组长真实 E2E 已证明员工提交、FIFO 确认、工序完成和批记录回填可跑通。
- 当前代码证据：`MesProFrontlineFeedbackSubmitServiceImpl` 已把报工、记录本和工序池事件放在同一事务；`MesTeamLeaderReportConfirmationServiceImpl` 已做组长确认和分配；`MesTeamLeaderBatchRecordBackfillServiceImpl` 已按正式逐工序批记录表单绑定回填。
- 当前实现风险证据：PQC 入池、复核签名和初版统一 trace 已有定向切片，但仍必须防止把“六个分组已返回”误判为完整闭环；质量分组必须证明 PQC 正式绑定目标生产提交，复核分组必须覆盖所有强制角色，批记录分组必须带正式字段审计和来源分配 ID，多事件查询必须返回 `candidateEvents` 而不是拼接最近记录。

## P0 Boundary

P0 只交付主闭环，不优先扩展零散页面。以下内容属于 P0：

- 生产员工正式提交：报工、记录本原始条目、工序池提交事件、服务端时间、实际员工电子签名必须同事务完成。
- PQC 正式提交：PQC 任务、逐件明细、质量结果必须进入工序池质量链路，并能和生产工单、工序、员工、签名互相追溯。
- 班组长复核：生产组长和 PQC 组长在负责范围内复核提交，必须留下复核状态、复核人、复核时间、复核说明和复核电子签名。
- 生产工单 FIFO 分配：分配目标必须是活跃生产工单；自动 FIFO 使用当前已确认的活跃订单队列顺序，手工调整仍必须满足活跃订单、当前工序、剩余数量和总数校验。
- 批记录追溯：订单工序完成后，只能按工序设置中的正式逐工序批记录表单绑定和字段映射回填，不得使用 `formBindings`、默认 `MAIN`、工序开始配置或前端文案替代。
- 闭环追溯视图：按 `processPoolEventId` 或生产工单 + 工序返回统一 trace，包含提交、质量、复核、分配、工序完成、批记录执行和字段审计投影。

## Canonical Event Contract

- P0 完整闭环的根事件必须是生产提交事件，`processPoolEventId` 在最终 trace、FIFO、批记录回填和闭环证据包中默认指 `eventType=PRODUCTION_SUBMIT` 的主事件 ID。
- PQC 提交可以产生独立 `eventType=PQC_INSPECTION` 的工序池事件，但它只能作为 `quality.sourceIds.pqcEventId` 或质量子事件进入闭环；按 PQC 事件查询时，后端必须通过结构化绑定解析到唯一生产提交根事件，或返回候选/阻塞，不得把 PQC 事件本身当作完整闭环根。
- 任何 P0 写链路完成后都必须返回或记录可追溯的工序池事件 ID；若该写链路不是生产提交，返回结果还必须包含其绑定的生产提交根事件 ID 或明确说明尚未绑定。
- 生产提交事件类型必须固定为 `PRODUCTION_SUBMIT`；PQC 提交事件类型必须固定为 `PQC_INSPECTION`。若 PQC 作为独立事件保存，必须通过正式结构化字段或正式关联表关联到同一生产工单、路线工序、PQC 任务和生产提交事件，不得靠工序名、页面文案、备注、rawPayload 内嵌字段或时间接近推断。
- PQC 与生产提交的绑定 ID 必须是可查询、可索引、可参与唯一性校验的正式字段或关系记录；`rawPayload` 只能作为审计快照，不得作为 trace `complete=true` 的唯一绑定依据。
- 使用“生产工单 + 工序”查询 trace 时，如果命中多条事件，接口必须返回候选事件列表或要求用户选择 `processPoolEventId`；不得把多条提交/PQC/复核记录合并成一条看似完整的事实。
- 每条主事件至少需要保存租户、生产工单、路线工序、MES 工序、实际员工、设备账号、设备、工作站、提交幂等键、服务端时间、签名 ID、签名员工和签名快照引用。
- 生产提交、PQC 提交、复核、FIFO 确认和批记录字段审计的幂等键必须是业务提交级键；不得把记录本 payload 内部幂等字段、浏览器请求 ID、页面时间戳或签名 ID 单独当作整条闭环的幂等凭证。
- 电子签名必须由后端校验签名 ID、签名员工、动作用途、签名快照和当前业务操作者关系；前端传入签名字段只能作为请求证据，不能绕过后端签名验真。
- 任何由 P0 主闭环派生的复核、分配、完成、批记录字段审计都必须反向持有正式来源事件或来源分配 ID；缺 ID 时 trace 顶层必须 `complete=false`，对应分组 `status=BLOCKED` 并返回机器可读 `blockers`。
- 顶层 `complete=true` 只能在提交、质量、复核、分配、完成和批记录分组都拥有正式结构化 ID、且没有 blocker 时返回；任何缺失、歧义、越权或非正式来源都必须保持 `complete=false`。
- 所有正式 ID、幂等键和来源关系必须落到数据库列、唯一约束、索引、迁移脚本、DO/Mapper 和测试 schema；只在 VO/DTO、前端类型、rawPayload 或内存对象中出现时，不算正式闭环事实。
- 任何 trace 分组的来源事实必须属于同一租户、同一生产工单、同一路线工序和同一 MES 工序；跨租户、跨工单、跨工序或权限越界的事实必须阻塞，不能拼接成完整闭环。

## Closed-Loop Invariants

后续开发必须把以下不变量当成实现合同，而不是页面展示参考：

| Invariant | 必须满足 | 不满足时的处理 |
| --- | --- | --- |
| 事件根唯一 | 每个可声明闭环完成的 trace 必须选定唯一 `processPoolEventId`。 | 返回候选事件或 `complete=false`，不得合并事实。 |
| trace 成熟度 | 初版 trace 只证明接口骨架时，不得作为完整 P0 验收；必须继续证明质量绑定、复核聚合、FIFO 来源和批记录字段审计均由正式 ID 串起。 | M3 可记录 initial GREEN，但 M6/DoD 仍保持未完成；缺成熟度测试时任务不得 completed。 |
| PQC 正式绑定 | PQC 事件必须通过 `pqcEventId`、`pqcTaskId` 和正式生产提交事件绑定字段，证明质量结果属于当前提交数量片段。 | `quality.status=BLOCKED`；同工单、同工序、时间接近或同员工都不足以完成。 |
| 质量数量勾稽 | FIFO 可消耗数量必须由当前生产提交根事件、PQC 结构化绑定、PQC 检验数量、合格数量和已消耗数量共同计算。 | `QUALITY_QUANTITY_MISMATCH` 或 `QUALITY_NOT_ALLOCATABLE`；仅 `inspectionResult=SUCCESS` 不足以放行超量分配。 |
| 写链路同源 | 报工、记录本、PQC、复核、分配、完成、批记录字段审计都必须能反查同一租户、同一生产工单、同一路线工序和来源事件链。 | trace 返回机器可读 blocker；禁止后台补默认关联。 |
| 终态写入原子性 | 新写链路不得产生“业务终态成功但工序池事件或正式来源 ID 缺失”的半成功。 | 当前请求整体失败或返回明确重复拒绝；历史断链只读展示为 BLOCKED。 |
| 签名责任一致 | 提交签名、PQC 签名、复核签名必须由后端校验用途、签名员工、快照和当前操作者关系。 | 写入失败；不得以登录用户、备注或确认弹窗代替。 |
| 批记录正式来源 | 批记录执行和字段审计只能来自工序设置逐工序正式批记录表单绑定和 `PROCESS_POOL_REPORT` 映射。 | `batchRecord.status=BLOCKED`；禁止 `formBindings`、默认 `MAIN` 或工序开始配置补齐。 |
| 结构化绑定优先 | PQC、复核、分配、完成和批记录审计必须使用结构化来源字段或关系表串联主事件。 | 只有 rawPayload、备注、名称或时间窗口时，对应分组必须 `BLOCKED`。 |
| Schema 可验证 | 关键字段、幂等键、来源 ID、批记录字段审计必须有迁移脚本、DO 字段、Mapper 映射、测试 schema 和唯一/索引约束。 | 只有接口字段或测试对象时，当前 slice 保持 RED/BLOCKED。 |
| 租户和权限隔离 | trace、复核、FIFO、批记录回填只能读取当前租户和当前操作者有权访问的正式事实。 | 跨租户、越权复核、跨工单拼接或共享样本污染时阻塞。 |

## Idempotency And Concurrency Contract

P0 写链路必须把“员工点一次提交”和“网络/浏览器重复提交”区分清楚；后续开发不得只依赖前端禁用按钮。

- 生产提交、PQC 提交、复核、FIFO 确认和批记录回填都必须有业务级幂等键或正式唯一约束，唯一范围至少包含租户、动作类型、目标工单、路线工序、实际操作者或复核人、来源事件和客户端提交键。
- 相同幂等键的重复请求必须返回同一正式结果，或在业务上明确拒绝重复；不得创建第二条有效事件、复核、分配、完成或字段审计。
- 不同幂等键但指向同一来源事件和同一动作终态的并发请求，必须在服务端事务内二次校验剩余数量、质量状态、强制复核状态和批记录字段审计唯一性。
- FIFO 确认必须在写分配的同一事务中重新读取质量可分配状态和强制复核完成状态；页面打开时的旧状态不能作为写入依据。
- 任何写链路出现部分失败时，不能留下“终态已成功但来源事件、幂等键或字段审计缺失”的半成功；重试必须命中同一正式结果或明确失败原因。
- 幂等唯一性必须由服务层事务校验和数据库唯一约束共同证明；只靠 Java 内存集合、前端防抖、页面按钮禁用或日志去重时不得进入 GREEN。
- 批记录回填必须在确认分配和订单工序完成的正式来源已写入后执行；若采用异步任务或事件驱动，任务状态、来源 ID、幂等键和失败重试状态必须结构化持久化，trace 在回填完成前保持 `batchRecord.status=BLOCKED`。

## Migration And Historical Data Contract

P0 主闭环新增正式字段时，迁移脚本也是验收合同的一部分；字段只在代码、测试对象或本地 schema 里存在，不能证明真实环境可闭环。

- 新增来源 ID、结构化绑定、签名字段、幂等键、索引或唯一约束时，正式 SQL 必须带 release migration metadata，并通过 `run-release-migration-policy-gate.py`；缺 metadata、依赖、风险等级或环境范围时不得进入 GREEN。
- 将字段收紧为 `NOT NULL` 前，迁移必须先显式检查现有未删除业务行；若存在缺正式来源 ID 的历史行，迁移必须 fail fast，并输出机器可读 blocker，不得默认填 `0`、复制最近事件、解析 rawPayload、按时间接近回填或静默跳过。
- 历史断链数据只能按正式修复方案处理：先定位来源事实、生成可审计 backfill 记录、复验同租户/同工单/同工序同源，再允许二次迁移；没有正式来源证据时，该历史 trace 必须保持 `complete=false`。
- SQL 中的过程、动态 SQL、`SIGNAL`、分隔符和幂等 `ALTER` 必须通过策略门禁和目标 MySQL 语法验证；语法失败是 `COMMAND-BLOCKED` 或迁移 RED，不得改成无条件成功 SQL。
- 迁移通过后仍必须同步测试 schema、DO/Mapper、索引或唯一约束测试；只有 release policy PASS 但测试 schema 漂移时，业务 GREEN 仍无效。
- 真实 E2E 前必须通过 `P0_RUNTIME_DB_HOST`、`P0_RUNTIME_DB_PORT`、`P0_RUNTIME_DB_NAME`、`P0_RUNTIME_DB_USER`、`P0_RUNTIME_DB_PASSWORD` 调用 `verify_p0_runtime_migration.py` 只读核验运行态已应用对应迁移，且目标列、索引、唯一约束和历史断链 blocker 与当前代码一致；缺运行态 DB env 或验证器非 PASS 时 E2E 只能 `BLOCKED`，不得启动浏览器写入。

## Review And Quality Sequencing

复核和质量门禁必须先作为后端规则冻结，再允许 FIFO 或批记录回填进入 GREEN：

- FIFO 确认写事务的固定顺序必须是：锁定或重新读取生产提交根事件；校验 PQC 结构化绑定和可分配数量；校验所有强制复核和复核签名；校验活跃订单和剩余数量；写入确认/分配/完成/批记录回填终态。任一前置失败时，不得先写确认终态再回滚式补 blocker。
- 生产组长复核是 FIFO 确认的强制前置；缺生产组长复核、复核签名或复核来源事件时，不得写入分配、订单工序完成或批记录回填。
- PQC 组长复核是否为强制前置必须由后端配置或业务规则明确表达；若配置为强制，PQC 组长复核和复核签名未完成时，质量分组保持 `BLOCKED`，FIFO 不得消耗该数量。
- 若 PQC 组长复核暂不作为 P0 第一版强制前置，trace 仍必须返回 PQC 组长复核配置状态、实际复核状态和 blocker；不得把“未配置强制复核”和“已复核通过”混为一谈。
- FIFO 只能消费质量状态已进入后端白名单、PQC 合格数量覆盖本次确认数量、且所有强制复核角色均完成签名复核的数量片段；质量未知、合格数量不足、复核缺失或配置歧义时必须 fail-fast。
- 复核动作只写复核事实和签名审计，不得修改原始提交、PQC 逐件明细、分配明细、订单工序完成或批记录字段审计。

## Formal ID Minimums

每个分组进入 GREEN 或 `complete=true` 前，至少必须具备以下正式 ID；字段命名可按现有代码风格调整，但语义不得弱化：

| 分组 | 最小正式 ID |
| --- | --- |
| `submitEvent` | `processPoolEventId`、`feedbackId`、`recordbookEntryId`、`recordbookEventId`、`actualEmployeeId`、`deviceAccountId`、`deviceId`、`workstationId`、`submitSignatureId` |
| `quality` | `pqcEventId`、`pqcTaskId`、`qaRegulationVersionId`、`pqcDetailSummaryId` 或逐件明细 ID 集合、`inspectionQuantity`、`qualifiedQuantity`、`allocatableQuantity`、`consumedQualityQuantity`、`pqcSignatureId`、正式生产提交绑定 ID、绑定关系 ID 或结构化绑定字段 |
| `review` | `reviewId`、`reviewerUserId`、`reviewSignatureId`、`reviewSourceProcessPoolEventId` |
| `allocation` | `allocationId`、`activeOrderId`、`targetWorkOrderId`、`sourceReviewId`、`sourceProcessPoolEventId` |
| `completion` | `orderProcessCompletionId` 或等价订单工序完成记录 ID、`targetWorkOrderId`、`routeProcessId`、`lastSourceProcessPoolEventId`、`lastReviewId` |
| `batchRecord` | `batchRecordExecutionId`、`batchRecordReportId`、`batchRecordDefinitionId`、`batchRecordVersionId`、`fieldAuditBatchId`、`fieldAuditItemId`、`sourceProcessPoolEventId` 或 `sourceAllocationId` |

## Trace Completion Algorithm

实现 `complete` 时必须按后端结构化事实逐项计算，不得由前端展示状态、中文文案或空数组推断：

1. `submitEvent` 必须有 `processPoolEventId`、`feedbackId`、`recordbookEntryId`、`recordbookEventId`、实际员工、设备账号、设备、工作站、提交签名和服务端提交时间。
2. `quality` 必须有正式 `pqcEventId`、`pqcTaskId`、规程版本、逐件明细摘要、检验数量、合格数量、已消耗质量数量、质量结论、质量可分配状态、PQC 签名和结构化生产提交绑定；若同一工单工序存在多条 PQC 候选且无法用正式 ID 唯一绑定生产提交事件，或合格数量不足以覆盖确认/分配数量，质量分组必须 `BLOCKED`。
3. `review` 必须有生产组长复核记录；如业务把 PQC 组长复核纳入同一闭环，则也必须有 PQC 组长复核记录。任一复核缺签名 ID、签名员工或签名快照时不得完成。
4. `allocation` 必须有活跃订单来源、FIFO 或手工分配明细、确认数量、来源复核和来源事件；分配数量必须等于确认数量且不得超过目标工单当前工序剩余数量。
5. `completion` 必须有订单工序完成记录、累计确认数量、目标数量、最后来源事件、最后复核记录和完成时间；并发重复完成必须返回同一正式结果或明确重复拒绝。
6. `batchRecord` 必须有正式逐工序批记录表单绑定、批记录执行 ID、字段审计 batch、字段审计 item、字段路径、单元格位置、来源事件或分配 ID、来源值、写入值和幂等键。
7. 所有分组必须属于同一租户，并在生产工单、路线工序、MES 工序和来源事件链上可证明一致；任一分组只靠名称、备注、时间接近或页面文案匹配时，顶层必须 `complete=false`。

## Closure Evidence Contract

P0 最终验收必须产出后端 trace 可复验的闭环证据包；该证据包不是新页面范围，而是防止“分段测试都绿、但仍无法回答业务问题”的收口门禁。

- 闭环证据包必须以本次真实提交捕获的 `processPoolEventId` 为根，并逐项回答：谁、在哪台设备、做了哪个工序、做了多少、质量结果怎样、签名是谁、进入哪个生产工单、班组长是否复核、如何进入批记录追溯。
- 每个答案必须同时包含业务可读值和正式 `sourceIds`；只有中文摘要、页面标签、截图、旧 API 返回值或人工说明时，不得计入完成证据。
- 证据包中的 `who`、`device`、`process`、`quantity`、`quality`、`signature`、`workOrder`、`review`、`batchRecord` 必须都能从 trace 响应或只读核验接口反查到同一租户、同一生产工单、同一路线工序和同一来源事件链。
- 任一答案缺正式 ID、缺同源证据、缺权限边界、来自 rawPayload-only、来自 `formBindings` 或需要人工拼接多个页面时，最终验收必须保持 `complete=false` 或记录 `FAIL/RED`。
- 真实 E2E 的 PASS 证据必须保存该闭环证据包的脱敏摘要和只读复验命令；静态合同、单元测试 GREEN、健康检查或历史 trace ID 不能替代本次 run 的闭环证据包。

## Non-Goals

- 不新增与主闭环无关的管理页、看板页或报表页。
- 不重做已存在的工序池基础模型、固定模板、审核副本、原始 revision 或生产组长配置能力，除非 P0 闭环发现缺口。
- 不把静态合同、API-only、历史截图、默认成功或 mock 数据作为真实闭环验收。
- 不把表单槽位 `formBindings`、特殊工序开始配置、记录本模板或默认槽位当成正式批记录表单来源。

## No-Fallback Gate

- 缺少正式员工、设备、工序、生产工单、PQC 结果、电子签名、班组长复核、FIFO 分配或正式批记录绑定时，P0 验收必须阻塞。
- 不允许用 mock、默认成功、API-only、前端拼接、旧字段猜测、`formBindings` 或默认 `MAIN` 槽位替代正式主闭环事实。
- 新实现不得产生“业务写入成功但工序池事件缺失”的部分成功状态；历史或旧数据已经存在这种断链时，只能在 trace 中返回 `complete=false` 和对应分组 `status=BLOCKED`，不得补默认事件冒充完成。
- 任何后续实现若确需 fallback，必须先由用户明确批准，并在任务文档记录触发条件、风险和移除策略。

## Current Gap Baseline

| Gap ID | 当前缺口 | P0 影响 | 期望解决方式 |
| --- | --- | --- | --- |
| P0-G01 | PQC 正式提交目前更新 PQC 任务和逐件明细，但未进入 `mes_pro_process_pool_event`。 | 质量结果不能从主事件反查，主闭环缺“质量结果怎样”。 | PQC 提交必须创建或绑定工序池事件，并在 trace 中返回任务、逐件明细、质量结论和签名。 |
| P0-G02 | 班组长复核和报工分配确认缺复核电子签名字段。 | 主闭环缺“复核签名是谁”，审计责任不完整。 | 复核和确认必须要求电子签名，并保存签名员工、签名 ID 和签名快照。 |
| P0-G03 | 追溯接口分散，尚不能按一个事件返回从提交到批记录的完整闭环。 | 用户需要跨页面拼接事实，无法一键审计。 | 新增统一闭环 trace 合同，聚合提交、PQC、复核、分配、完成、批记录字段审计。 |
| P0-G04 | 一线提交请求没有主提交级幂等键，只有记录本 payload 存在幂等字段。 | 员工重复点击可能产生重复主事件或部分链路重复。 | 提交请求、PQC 提交、组长确认应有业务幂等键或唯一提交凭证，重复请求返回同一结果或明确重复拒绝。 |
| P0-G05 | 生产/PQC 质量结果与可分配数量的统一门禁仍需冻结。 | FIFO 可能消费尚未质量放行的数据。 | P0 第一版只允许 `QUALITY_PASS` 或业务确认的可分配状态进入 FIFO；失败、待检、无法确认状态必须阻塞。 |
| P0-G06 | 初版 trace 可返回六个分组，但质量绑定、复核角色聚合、`sourceAllocationId`、字段审计和 `candidateEvents` 仍可能不完整。 | trace 页面看似可用，但无法可靠回答“质量结果怎样、谁复核、进了哪个工单、如何进批记录”。 | 增加 trace 成熟度 TDD：质量正式绑定/多候选、强制复核聚合、批记录来源分配和字段审计缺失均必须让 `complete=false`。 |
| P0-G07 | 当前 trace 可能从 PQC `rawPayload` 解析生产提交 ID。 | rawPayload 不是正式关系，无法保证索引、唯一性、迁移和多候选判定，后续容易假绿。 | 增加结构化绑定门禁：PQC 生产提交绑定必须沉淀为正式字段或关系表，rawPayload 只能作为审计快照。 |
| P0-G08 | 幂等和并发边界尚未完整冻结。 | 重复点击、网络重试或并发组长确认可能重复分配、重复完成或重复回填批记录。 | P0-T01/P0-T07/P0-T08 必须覆盖重复请求、并发请求、部分失败重试和事务内二次校验。 |
| P0-G09 | 关键字段若只加到接口或测试对象，未同步迁移、DO、Mapper、测试 schema 和唯一索引。 | 开发环境单测可能假绿，真实库无法保存正式来源 ID 或幂等键。 | 每个新增正式字段必须由 schema 合同测试和迁移脚本证明，缺任一项时阻塞。 |
| P0-G10 | trace 聚合若没有租户、工单、路线工序和权限边界。 | 不同租户或不同工单的事实可能被拼接，审计结果不可信。 | trace、复核、FIFO 和批记录查询都必须以租户和来源事件链校验同源。 |
| P0-G11 | 正式迁移未通过发布策略门禁，或历史未删除行缺新增结构化来源 ID。 | 本地单测通过但测试服/正式服迁移失败，或真实 trace 只能靠默认值、rawPayload 或人工补齐。 | 新增 SQL 必须通过 release migration policy gate；历史缺 ID 必须 fail fast 并保留 `complete=false`，直到有正式 backfill 证据。 |

## False-GREEN Risks To Guard

- 旧单测若仍断言 `processPoolEventService.createPqcInspectionEvent(...)` `never()`，只能证明旧缺口存在，不能作为 P0 质量链路通过证据。
- 前端静态合同只能证明脚本、字段和入口被接入；不能证明真实提交、PQC、复核、FIFO、批记录回填已经跑通。
- 真实 E2E 前置检查通过后，如果完整页面步骤尚未实现或未完成目标断言，结果必须是 `FAIL/RED`，不得记为 PASS。
- trace 返回 `complete=true` 但任一分组缺正式 `sourceIds`、机器可读 `blockers` 或批记录字段审计投影时，视为验收失败。
- 初版 trace 定向测试只证明 endpoint、DTO 或基础分组存在时，只能记录为 M3 initial GREEN；未覆盖质量唯一绑定、多候选 `candidateEvents`、全部强制复核角色和批记录字段审计前，不得把 P0 标记 completed。
- `candidateEvents` 永远为空、review 永远取第一条、batchRecord 没有 `sourceAllocationId` 或 field audit item 时，即使页面能展示中文摘要，也必须视为假绿风险。
- PQC 绑定只存在于 `rawPayload`、批记录审计只记录新值不记录来源值、或 FIFO 只信任页面预检状态时，即使当前定向测试通过，也必须视为假绿风险。
- 迁移脚本未跑 release policy gate、`NOT NULL` 收紧未处理历史缺 ID 行、索引只存在于测试 schema、或 SQL 失败后改成默认填充时，即使 Java 定向测试通过，也必须视为假绿风险。

## Definition of Done

- P0 BDD 场景全部有 RED/GREEN 映射。
- P0 后端定向测试覆盖生产提交、PQC 入池、复核签名、FIFO 确认、批记录回填和统一 trace。
- P0 前端静态合同覆盖真实入口、请求字段、错误展示和 trace 页面。
- P0 真实 E2E 使用真实前端路径完成生产提交、PQC 提交、班组长复核、FIFO 确认、批记录回填和闭环追溯。
- P0 实现任务必须先通过 `implementation-readiness-gates.md` 的 M0 前置门禁，再进入生产代码 GREEN。
- M3 initial trace GREEN 不等于 Definition of Done；P0 完成前必须通过 trace 成熟度、FIFO/质量门禁、批记录回填和真实 E2E 当前可用前置的全部门禁。
- 缺任何正式前置时记录 BLOCKED，不用 fallback、mock、默认成功或 API-only 冒充完成。
- 新增字段或关系必须具备迁移脚本、测试 schema、DO/Mapper、唯一/索引约束和 schema 合同测试证据；缺 schema 证据的业务 GREEN 不能进入 M6。
- trace 完成证据必须包含租户、工单、路线工序、MES 工序、来源事件链和权限边界同源校验；缺同源校验时不得返回 `complete=true`。
- P0 最终 PASS 必须提供闭环证据包，且九个审计问题均由正式来源 ID 支撑；缺任一项时不能把任务状态标记为 completed。
