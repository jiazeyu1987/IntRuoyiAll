# P0 生产执行主闭环测试数据计划

## Purpose and Scope

本文档定义 P0 主闭环实现和真实 E2E 所需的任务自有测试数据。所有写入型数据必须带任务标识，可清理、可复验、可追溯，不得污染生产租户、admin 基线数据或无关真实业务记录。

## Evidence Reviewed

- `docs/acceptance/production-execution-main-loop/bdd-scenarios.md`
- `docs/acceptance/production-execution-main-loop/e2e-plan.md`
- `docs/acceptance/production-line-process-pool/test-data.md`
- `doc/tasks/20260731-team-leader-workbench-prd-plan/test-plan.md`
- `docs/database-rules.md` 在建数或迁移前必须再读取。
- `docs/login-access.md` 在真实登录前必须再读取。

## Required Test Data

| 数据类型 | 最小要求 |
| --- | --- |
| 测试租户 | 一个明确授权 MES 写入型 E2E 的非生产租户。 |
| 数据前缀 | 所有任务自有数据使用 `P0-EXEC-<runId>`。 |
| 生产组长账号 | 拥有班组配置、提交看板、复核、FIFO 确认、trace 查看权限。 |
| PQC 组长账号 | 拥有 PQC 提交看板、PQC 复核、trace 查看权限。 |
| 强制复核角色配置 | 明确生产组长复核和 PQC 组长复核是否为 FIFO 前置；测试证据必须区分强制、非强制、待复核和已复核。 |
| 生产员工账号或员工档案 | 绑定目标工序，可在一线填报入口作为实际员工提交。 |
| PQC 员工账号或员工档案 | 绑定目标 PQC 工序，可提交 QA 规程驱动的逐件检验。 |
| 设备账号 | 可登录现场入口或承载设备端操作上下文；不得替代实际员工。 |
| 设备和工作站 | 一个启用设备和工作站，设备有参数 `pressure`，默认值在上下限内。 |
| 生产工单 | 至少两张可加入活跃订单池的本地生产工单，其中一张用于完成并回填批记录。 |
| 活跃订单 | 由生产组长通过真实 UI 加入，记录加入时间和队列顺序。 |
| 工艺路线和路线工序 | 目标生产工序和目标 PQC 工序均可解析正式工序。 |
| 固定生产模板 | 包含完成数量、损耗数量、设备参数和不良原因。 |
| PQC 任务 | 关联活跃订单、路线工序、检验类型、业务日期、班次、轮次和 QA 规程版本。 |
| QA 规程快照 | 至少包含压力和外观两个检验项，可生成逐件明细。 |
| 电子签名 | 生产员工、PQC 员工、生产组长、PQC 组长均有正式测试签名能力；明文凭据不得写入文档。 |
| 正式批记录绑定 | 目标路线工序绑定正式逐工序批记录表单，含定义 ID、版本 ID、报表 ID。 |
| 字段映射 | `PROCESS_POOL_REPORT` 到正式批记录单元格的映射，至少覆盖完成数量和设备参数。 |
| 幂等键样本 | 生产提交、PQC 提交、组长复核/确认各一组固定幂等键，用于重复点击验证。 |
| 结构化绑定样本 | PQC 事件必须有正式生产提交绑定字段或关系表记录；另准备 rawPayload-only 负向样本。 |
| 质量数量样本 | 正向样本必须让 PQC 检验数量、合格数量和确认数量可勾稽；负向样本必须让合格数量小于确认数量。 |
| schema 版本样本 | 运行态必须已应用 P0 正式迁移，且测试 schema、DO/Mapper、索引和唯一约束与迁移一致；真实 E2E 写入前必须具备 `P0_RUNTIME_DB_*` 只读核验连接。 |
| 租户权限样本 | 正向样本使用授权测试租户和角色；负向样本覆盖跨租户、跨工单或跨工序事实不得进入 trace。 |
| trace 负向样本 | 缺 PQC 事件、PQC 多候选歧义、rawPayload-only PQC 绑定、缺复核签名、缺字段审计投影各一组。 |
| 负向样本 | 非活跃订单、PQC 失败、缺复核签名、缺批记录绑定、缺字段映射各一组。 |
| 闭环证据包样本 | 正向样本必须能生成九个审计问题的脱敏答案；负向样本至少覆盖一个答案缺正式来源 ID 时不得 PASS。 |

## Environment Variables and ID Capture

真实 E2E 前置变量必须只通过本机进程环境或受控 secret 注入，不写入 Markdown、截图或提交信息。缺少任一必需值时记录 `BLOCKED`：

| 变量 | 用途 | 要求 |
| --- | --- | --- |
| `P0_FRONTEND_URL` / `P0_BACKEND_URL` | 真实运行态入口 | 必须成对指向当前 worktree `8092/48092`，或已合入主线的 `8081/48081`。 |
| `P0_RUNTIME_DB_HOST` / `P0_RUNTIME_DB_PORT` / `P0_RUNTIME_DB_NAME` / `P0_RUNTIME_DB_USER` / `P0_RUNTIME_DB_PASSWORD` | 真实 MySQL 只读迁移核验 | 必须指向当前运行态使用的 MySQL；账号仅用于 schema、索引和历史断链只读检查，缺任一变量时真实 E2E `BLOCKED` 且不得启动浏览器写入。 |
| `P0_TENANT` / `P0_USERNAME` / `P0_PASSWORD` | 登录和租户选择 | 必须是授权测试租户账号；密码只读自环境变量。 |
| `P0_RUN_ID` | 任务数据前缀 | 生成 `P0-EXEC-<runId>`，用于搜索、清理和证据归档。 |
| `P0_WORK_ORDER_ID` / `P0_WORK_ORDER_CODE` | 目标生产工单 | 必须为任务自有或授权测试工单，且可加入活跃订单池。 |
| `P0_ROUTE_PROCESS_ID` / `P0_PROCESS_ID` | 目标路线工序和 MES 工序 | 必须与目标工单、PQC 任务和批记录绑定一致。 |
| `P0_DEVICE_ACCOUNT_ID` / `P0_DEVICE_ID` / `P0_WORKSTATION_ID` | 设备端上下文 | 必须能回答“在哪台设备”，不得只给设备名称。 |
| `P0_PQC_TASK_ID` / `P0_QA_REGULATION_VERSION_ID` | PQC 正式来源 | 必须能生成逐件明细和质量结论。 |
| PQC 到生产提交的结构化绑定运行中捕获值 | PQC 到生产提交的结构化绑定 | 必须在本次 run 中从正式字段或关系记录捕获 `pqcProductionBindingId` 或等价正式来源；不得作为写入前历史环境变量输入，不得只来自 rawPayload。 |
| `P0_SUBMIT_QUANTITY` / `P0_CONFIRM_QUANTITY` | 提交和确认数量 | 必须与生产提交、FIFO 分配和订单工序完成勾稽一致。 |
| `P0_PQC_INSPECTION_QUANTITY` / `P0_PQC_QUALIFIED_QUANTITY` / `P0_PQC_ALLOCATABLE_QUANTITY` | PQC 数量门禁 | 正向样本必须覆盖确认数量；负向样本必须能构造合格数量不足 blocker。 |
| `P0_SIGNATURE_ID` / `P0_PQC_SIGNATURE_ID` / `P0_REVIEW_SIGNATURE_ID` / `P0_PQC_REVIEW_SIGNATURE_ID` | 四类签名 | 必须分别对应生产员工、PQC 员工、生产组长、PQC 组长或正式授权签名人。 |
| `P0_SIGNATURE_EMPLOYEE_ID` / `P0_PQC_SIGNATURE_EMPLOYEE_ID` / `P0_REVIEW_SIGNATURE_EMPLOYEE_ID` / `P0_PQC_REVIEW_SIGNATURE_EMPLOYEE_ID` | 四类签名员工 | 必须分别对应四类签名的员工主体，PQC 组长复核不得复用生产组长 FIFO 确认签名主体。 |
| `P0_BATCH_RECORD_REPORT_ID` / `P0_BATCH_RECORD_DEFINITION_ID` / `P0_BATCH_RECORD_VERSION_ID` | 正式批记录绑定 | 必须来自工序设置逐工序绑定，不得来自 `formBindings`。 |
| `P0_SUBMIT_IDEMPOTENCY_KEY` / `P0_PQC_IDEMPOTENCY_KEY` / `P0_CONFIRM_IDEMPOTENCY_KEY` | 重复提交验证 | 每次 run 固定，第二次请求必须返回同一结果或明确重复拒绝。 |
| `P0_SCHEMA_MIGRATION_ID` 或等价运行态版本证据 | schema 核验 | 可作为辅助证据；不能替代 `verify_p0_runtime_migration.py` 对真实 MySQL 的只读 PASS。 |
| `P0_MIGRATION_POLICY_EVIDENCE` 或等价证据路径 | 迁移发布策略 | 必须指向本次 run 使用的 release migration policy gate 结果；不得只记录本地测试 schema。 |

运行中必须捕获并写入证据的 ID：生产提交根事件 `processPoolEventId`、质量子事件 `pqcEventId`、`pqcProductionBindingId`、`reviewId`、`allocationId`、`orderProcessCompletionId`、`batchRecordExecutionId`、`fieldAuditBatchId`、`fieldAuditItemId`。这些 ID 是 trace 完整性只读核验的输入，不得由测试脚本自行伪造；`pqcEventId` 不得替代生产提交根事件。

闭环证据包必须保存本次 run 的脱敏摘要字段：`answers.who`、`answers.device`、`answers.process`、`answers.quantity`、`answers.quality`、`answers.signature`、`answers.workOrder`、`answers.review`、`answers.batchRecord`、`sameSourceChecks` 和 `blockers`。`answers.quantity` 必须包含提交数量、PQC 检验数量、PQC 合格数量、确认数量、分配数量和累计完成数量。 `answers.quality` 必须包含结构化绑定和数量覆盖结果。这些字段只能来自后端 trace 或只读核验结果，不得由 Playwright 用常量、截图或历史环境变量补齐。

## Data Setup Gates

- 写入型 E2E 的业务主路径必须通过真实页面创建或提交；只允许用 API/DB 做只读核验、前置存在性检查、任务自有数据清理证据，或在用户明确批准后执行可回滚测试种子。
- 所有任务自有对象必须带 `P0-EXEC-<runId>` 或等价可搜索标识；无法打标的共享配置只能复用授权测试基线，不得修改生产模板、admin 基线或无关真实业务记录。
- 批记录绑定和字段映射必须指向正式逐工序批记录表单；测试数据准备阶段不得用 `formBindings`、默认 `MAIN`、特殊开始节点配置或前端文案补齐缺口。
- 电子签名测试能力必须通过安全运行环境注入；文档、日志、截图、trace、JSON 证据和提交信息均不得包含签名密码或 token。
- `pqcProductionBindingId` 是本次 PQC 提交后的运行中捕获值，只能来自后端正式字段、关系表或 trace `closureEvidence`，不得作为写入前环境变量输入，避免历史绑定 ID 被误用为本轮闭环证据。
- `P0_PROCESS_POOL_EVENT_ID` 只能用于 trace 只读诊断；若真实 E2E 要证明主闭环 PASS，必须在同一次 run 中从真实页面创建并捕获新的生产提交根事件 `processPoolEventId`，并单独捕获绑定的 `pqcEventId`。
- 如果测试数据只能通过 SQL 或 API 直接写入主路径事实，不得用于真实 E2E PASS；只能作为只读前置核验或用户明确批准的可回滚种子，并记录风险。
- E2E PASS 证据必须包含本次 run 生成的 ID 清单；使用历史 ID、预置 ID 或手工拼接 ID 只能做诊断，不能证明本次主闭环已跑通。
- schema 前置检查只能证明运行态具备正式字段、索引和唯一约束；不能替代页面主路径写入。
- 真实 E2E 浏览器写入前必须先通过 `P0_RUNTIME_DB_*` 执行运行态迁移验证器；缺 env、缺字段、缺索引或历史断链 blocker 时只记录 `BLOCKED`，不得进入生产提交页面写入。
- 迁移 policy 前置检查只能证明 SQL 元数据和发布策略合规；不能替代 JUnit 行为测试、运行态迁移核验或真实页面主路径写入。
- 同源前置检查必须证明正向样本的租户、工单、路线工序、MES 工序、设备、PQC 任务和批记录绑定一致。

## Negative Sample Isolation

- 负向样本必须使用独立 `P0-EXEC-<runId>-NEG-*` 标识，不得复用正向 PASS 主工单，避免失败状态污染主闭环。
- `PQC_FAIL` 样本必须证明质量不可分配，不能继续执行 FIFO、完成或批记录回填。
- `PQC_QUALIFIED_QUANTITY_SHORT` 样本必须让 PQC 合格数量小于 FIFO 确认数量，用来证明不会仅凭 `inspectionResult=SUCCESS` 放行超量分配。
- `MISSING_REVIEW_SIGNATURE` 样本必须在复核或确认写入前失败，不能留下无签名复核记录。
- `MISSING_BATCH_RECORD_BINDING` 样本只能验证 blocker，不得临时用 `formBindings` 或默认槽位补绑定。
- `PQC_BINDING_AMBIGUOUS` 样本必须至少有两条候选生产提交或两条候选 PQC 事件，用来证明 trace 不会拼接事实。
- `PQC_RAW_PAYLOAD_ONLY` 样本只能把生产提交 ID 放在 rawPayload，不创建正式绑定字段或关系，用来证明 trace 和 FIFO 不把 rawPayload 当完成证据。
- `CROSS_TENANT_OR_ORDER` 样本必须构造同名或相似工序、PQC、复核或批记录事实，用来证明 trace 不会跨租户、跨工单或跨工序拼接。
- `DUPLICATE_CONFIRM` 样本必须重复提交同一复核/确认幂等键，用来证明不会重复分配、重复完成或重复写批记录字段审计。
- `CLOSURE_EVIDENCE_MISSING_SOURCE` 样本必须让九个审计答案中的至少一项缺正式来源 ID，用来证明真实 E2E 和 trace 不会把证据包补成 PASS。

## Reset Procedure

- 按 `P0-EXEC-<runId>` 清理活跃订单、员工绑定、设备参数、不良原因、生产提交、PQC 提交、复核记录、分配记录、订单工序完成记录。
- 批记录字段审计若属于 append-only 治理数据，应保留但必须记录来源 runId、executionId、fieldAuditBatchId 和不强删原因。
- 共享设备、员工、权限或配置若被临时修改，执行前记录原值，执行后恢复并复验。
- 已用于证明“缺正式绑定阻塞”或“PQC 失败阻塞”的负向样本不得被清理成看似成功状态。
- 失败时保留最小证据，不记录密码、token、cookie、Authorization 或签名密码。

## Data Ownership

- P0 任务拥有自己创建的活跃订单记录、提交事件、PQC 明细、复核记录、分配记录、完成记录和配置关系。
- 正式主数据只允许使用授权测试租户内的数据；如需临时创建，必须带任务前缀并在清理计划内。
- 批记录模板和字段映射可复用测试模板，但不得修改生产模板或共享业务模板。
- 电子签名凭据只通过安全运行环境注入，不写入 Markdown、日志或提交信息。
- 若 append-only 审计数据不能硬删除，必须在验证报告记录保留原因、对象 ID、runId 和只读复验方式。

## Test Blockers

- 缺少任一必须角色、签名能力、活跃订单、PQC 任务、QA 规程或正式批记录绑定时，真实 E2E 阻塞。
- 无法安全清理任务自有写入数据时，写入型 E2E 阻塞。
- 字段映射不可证明来自 `PROCESS_POOL_REPORT` 且目标为正式批记录表单时，批记录追溯阻塞。
- PQC 失败或质量未知样本无法构造时，质量门禁负向验证阻塞。
- 缺少 `P0_RUNTIME_DB_*`、运行态 schema 未应用 P0 迁移、缺索引、缺唯一约束或历史断链检查未 PASS 时，后端运行态核验和真实 E2E 阻塞。
- 无法构造跨租户、跨工单或跨工序负向样本时，trace 同源隔离验证阻塞。
- 无法生成九个审计问题的脱敏闭环证据包时，P0 最终验收阻塞。
