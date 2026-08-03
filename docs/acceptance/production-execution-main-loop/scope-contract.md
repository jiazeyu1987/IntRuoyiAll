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
- 当前缺口证据：`MesFrontlinePqcContextServiceTest#shouldSubmitPqcInspectionFromQaRegulationTaskSource` 目前断言 `processPoolEventService.createPqcInspectionEvent(...)` 不被调用；`MesProcessPoolSubmissionReviewDO` 和组长复核/确认请求未包含电子签名字段；现有追溯服务分散为分配、订单工序、批记录三段，还不是按单个工序池事件返回一条完整闭环视图。

## P0 Boundary

P0 只交付主闭环，不优先扩展零散页面。以下内容属于 P0：

- 生产员工正式提交：报工、记录本原始条目、工序池提交事件、服务端时间、实际员工电子签名必须同事务完成。
- PQC 正式提交：PQC 任务、逐件明细、质量结果必须进入工序池质量链路，并能和生产工单、工序、员工、签名互相追溯。
- 班组长复核：生产组长和 PQC 组长在负责范围内复核提交，必须留下复核状态、复核人、复核时间、复核说明和复核电子签名。
- 生产工单 FIFO 分配：分配目标必须是活跃生产工单；自动 FIFO 使用当前已确认的活跃订单队列顺序，手工调整仍必须满足活跃订单、当前工序、剩余数量和总数校验。
- 批记录追溯：订单工序完成后，只能按工序设置中的正式逐工序批记录表单绑定和字段映射回填，不得使用 `formBindings`、默认 `MAIN`、工序开始配置或前端文案替代。
- 闭环追溯视图：按 `processPoolEventId` 或生产工单 + 工序返回统一 trace，包含提交、质量、复核、分配、工序完成、批记录执行和字段审计投影。

## Canonical Event Contract

- `processPoolEventId` 是 P0 主闭环的首选 trace 根 ID；任何 P0 写链路完成后都必须返回或记录可追溯的工序池事件 ID。
- 生产提交事件类型建议固定为 `PRODUCTION_SUBMIT`；PQC 提交事件类型建议固定为 `PQC_INSPECTION`。若 PQC 作为独立事件保存，必须通过正式结构化字段关联到同一生产工单、路线工序、PQC 任务和生产提交事件，不得靠工序名、页面文案、备注或时间接近推断。
- 使用“生产工单 + 工序”查询 trace 时，如果命中多条事件，接口必须返回候选事件列表或要求用户选择 `processPoolEventId`；不得把多条提交/PQC/复核记录合并成一条看似完整的事实。
- 每条主事件至少需要保存租户、生产工单、路线工序、MES 工序、实际员工、设备账号、设备、工作站、提交幂等键、服务端时间、签名 ID、签名员工和签名快照引用。
- 任何由 P0 主闭环派生的复核、分配、完成、批记录字段审计都必须反向持有正式来源事件或来源分配 ID；缺 ID 时 trace 必须 `BLOCKED`。

## Non-Goals

- 不新增与主闭环无关的管理页、看板页或报表页。
- 不重做已存在的工序池基础模型、固定模板、审核副本、原始 revision 或生产组长配置能力，除非 P0 闭环发现缺口。
- 不把静态合同、API-only、历史截图、默认成功或 mock 数据作为真实闭环验收。
- 不把表单槽位 `formBindings`、特殊工序开始配置、记录本模板或默认槽位当成正式批记录表单来源。

## No-Fallback Gate

- 缺少正式员工、设备、工序、生产工单、PQC 结果、电子签名、班组长复核、FIFO 分配或正式批记录绑定时，P0 验收必须阻塞。
- 不允许用 mock、默认成功、API-only、前端拼接、旧字段猜测、`formBindings` 或默认 `MAIN` 槽位替代正式主闭环事实。
- 新实现不得产生“业务写入成功但工序池事件缺失”的部分成功状态；历史或旧数据已经存在这种断链时，只能在 trace 中明确标记 `BLOCKED`，不得补默认事件冒充完成。
- 任何后续实现若确需 fallback，必须先由用户明确批准，并在任务文档记录触发条件、风险和移除策略。

## Current Gap Baseline

| Gap ID | 当前缺口 | P0 影响 | 期望解决方式 |
| --- | --- | --- | --- |
| P0-G01 | PQC 正式提交目前更新 PQC 任务和逐件明细，但未进入 `mes_pro_process_pool_event`。 | 质量结果不能从主事件反查，主闭环缺“质量结果怎样”。 | PQC 提交必须创建或绑定工序池事件，并在 trace 中返回任务、逐件明细、质量结论和签名。 |
| P0-G02 | 班组长复核和报工分配确认缺复核电子签名字段。 | 主闭环缺“复核签名是谁”，审计责任不完整。 | 复核和确认必须要求电子签名，并保存签名员工、签名 ID 和签名快照。 |
| P0-G03 | 追溯接口分散，尚不能按一个事件返回从提交到批记录的完整闭环。 | 用户需要跨页面拼接事实，无法一键审计。 | 新增统一闭环 trace 合同，聚合提交、PQC、复核、分配、完成、批记录字段审计。 |
| P0-G04 | 一线提交请求没有主提交级幂等键，只有记录本 payload 存在幂等字段。 | 员工重复点击可能产生重复主事件或部分链路重复。 | 提交请求、PQC 提交、组长确认应有业务幂等键或唯一提交凭证，重复请求返回同一结果或明确重复拒绝。 |
| P0-G05 | 生产/PQC 质量结果与可分配数量的统一门禁仍需冻结。 | FIFO 可能消费尚未质量放行的数据。 | P0 第一版只允许 `QUALITY_PASS` 或业务确认的可分配状态进入 FIFO；失败、待检、无法确认状态必须阻塞。 |

## Definition of Done

- P0 BDD 场景全部有 RED/GREEN 映射。
- P0 后端定向测试覆盖生产提交、PQC 入池、复核签名、FIFO 确认、批记录回填和统一 trace。
- P0 前端静态合同覆盖真实入口、请求字段、错误展示和 trace 页面。
- P0 真实 E2E 使用真实前端路径完成生产提交、PQC 提交、班组长复核、FIFO 确认、批记录回填和闭环追溯。
- P0 实现任务必须先通过 `implementation-readiness-gates.md` 的 M0 前置门禁，再进入生产代码 GREEN。
- 缺任何正式前置时记录 BLOCKED，不用 fallback、mock、默认成功或 API-only 冒充完成。
