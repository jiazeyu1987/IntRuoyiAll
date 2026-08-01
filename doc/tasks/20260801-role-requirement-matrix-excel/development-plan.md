# 岗位需求分解矩阵全链路差距收敛开发计划

## Planning Basis

- 需求基线：`C:\Users\BJB110\Desktop\文档\职责\岗位需求分解矩阵.xlsx`
- 主表：`岗位需求分解矩阵`，23 项需求，编号 `M01-M23`。
- 衍生表：`衍生需求`，39 项需求，编号 `D01-D39`。
- 已交付基线：`doc/tasks/20260731-team-leader-workbench-prd-plan/`。
- 当前代码证据：
  - 生产组长活跃订单：`mes_pro_process_pool_active_order`、`MesTeamLeaderActiveOrderServiceImpl`。
  - PQC 活跃订单：`MesFrontlinePqcContextServiceImpl` 通过 `mes_pro_process_pool` 活跃行查询。
  - 报工分配/完成：`MesTeamLeaderReportConfirmationServiceImpl`、`MesTeamLeaderOrderProcessCompletionService` 当前按 `MesProWorkOrderDO.quantity`。
  - 批记录回填：`MesTeamLeaderBatchRecordBackfillServiceImpl` 正确读取 `MesProRouteFlowProcessBatchRecordDO`，但只使用一个事件和一条代表分配。
  - PQC 数据：`MesProProcessPoolPqcRecordDO` 尚缺规程版本、检验类型、日期、班次、轮次、数量、逐件明细和复核状态。
  - PQC 页面：`FrontlineFixedTemplatePanel.vue` 仍固定 `length/appearance/seal/pressure`、`PATROL`、数量 `30`、损耗 `1`。
  - 放行预检：`MesProEdhrReleaseServiceImpl` 的检验、偏差、返工、报废和库存检查仍生成“来源未接入”阻塞项。

本计划是差距驱动的增量计划，不重复建设已通过真实 E2E 的生产组长工作台能力。

## Implementation Areas

| 区域 | 目标 | 现有锚点 | 计划新增/调整锚点 |
|---|---|---|---|
| A1 ERP 与权威活跃订单 | 统一生产、PQC、调拨、批记录、放行的订单身份和状态 | `MesProcessPoolActiveOrderDO`、`MesTeamLeaderActiveOrderServiceImpl`、`MesFrontlinePqcContextServiceImpl` | 计划新增 `MesActiveOrderCommandService`、`MesActiveOrderQueryService`、责任范围关系和迁移校验；禁止业务服务直接从两张表判断活跃状态 |
| A2 调拨追溯与开工检查 | 关联多个调拨/发货/补料/退料和物料批次，输出开工检查 | 现有 ERP 库存调拨/MES 调拨查询能力待正式确认 | 计划新增活跃订单-调拨关系、物料覆盖读模型、开工检查服务与页面区块；正式 ERP 表/API 未确认时阻塞 |
| A3 班组配置、设备和范围 | 只绑定正式设备台账，扩展责任范围和审计 | `MesProcessPoolTeamLeaderController`、`MesTeamLeaderRuntimeConfigServiceImpl`、`MesProcessPoolTeamLeaderScopeDO` | 移除独立创建班组设备入口；新增设备台账选择关系、产线/设备/订单范围和一致审计 |
| A4 生产事实、分配、进度和批记录 | 工序事实先提交，按系数分配并确定性汇总批记录 | `FrontlineFixedTemplatePanel.vue`、`MesTeamLeaderReportConfirmationServiceImpl`、`MesTeamLeaderOrderProcessCompletionService`、`MesTeamLeaderBatchRecordBackfillServiceImpl` | 调整原始报工命令和模型；读取正式生产系数快照；新增订单工序聚合器和字段聚合策略 |
| A5 QA 规程与 PQC | 版本化规程、任务、逐件明细、复核、修订和过程检验 | `MesFrontlinePqcContextServiceImpl`、`MesProProcessPoolPqcRecordDO`、`MesTeamLeaderSubmissionReviewServiceImpl`、`FrontlineFixedTemplatePanel.vue` | 计划新增 QA 规程/版本/项目、PQC 任务/逐件明细/修订；动态渲染规程；禁止依赖最新生产事件 |
| A6 异常、日结、完整性和放行 | 分离生产/质量异常，形成日结、追溯和真实放行检查 | `MesWorkOrderAbnormalReportServiceImpl`、`MesProEdhrReleaseServiceImpl`、eDHR 放行页面 | 计划新增过程检验聚合、质量异常、班组/PQC 日结、完整性编排器和放行来源适配器 |

## Milestones

| 里程碑 | 目标 | 依赖 | 主要 Excel 覆盖 | 完成门禁 |
|---|---|---|---|---|
| M0 契约冻结 | 冻结术语、权威来源、状态机、业务键、已交付基线和迁移策略 | 无 | 全部 62 项的跨切面约束 | 评审记录明确单一活跃订单来源、正式批记录来源、QA 所有权、错误策略和非范围 |
| M1 权威来源与增量模型 | 建立统一活跃订单服务、增量表结构、迁移预检和跨角色查询 | M0 | `M01`、`M03-M04` | 生产和 PQC 只通过同一服务读取活跃订单；迁移冲突可检测；无双读 fallback |
| M2 生产事实到正式批记录 | 完成订单无关报工、退回修订、系数分配、进度和确定性批记录汇总 | M1 | `M10-M11`、`M16-M19`、`D11` | 目标量按固定 ERP 数量乘系数；多事件聚合完整；并发不超额、不重复回填 |
| M3 QA 规程与 PQC 闭环 | 完成规程版本、任务、首检/巡检/末检、逐件提交、签名、复核和补正 | M1 | `M09`、`M12-M15`、`M20`、`D15-D35` | 页面不再固定示例项目或默认数量；PQC 不依赖生产事件；自我确认被阻塞 |
| M4 调拨、异常、完整性与放行 | 完成调拨追溯、开工检查、过程检验、质量异常、完整性和真实放行来源 | M2、M3 | `M02`、`M05-M08`、`M21-M23`、`D36-D37` | 所有放行项可追溯到真实来源；缺项和阻塞异常不能放行 |
| M5 日结、范围、权限、审计和快照 | 补齐班组配置、范围、日结、只读看板、历史快照和审计 | M2、M3、M4 | `D01-D10`、`D12-D14`、`D38-D39` | 角色权限和数据范围真实隔离；配置变更不改写历史；日结逐项解释未闭环 |
| M6 迁移、并发与真实验收 | 完成迁移、性能、并发、真实 Playwright E2E、回归、清理和发布门禁 | M1-M5 | 全部 62 项 | 62 个 AC 全部通过；真实主链路、失败路径、权限、并发和清理证据齐全 |

## Strict BDD + TDD Delivery Contract

### Atomic Delivery Unit

- 后续实现的最小交付单位是一个 `AC-*`，或一个仍能独立产生可观察 RED/GREEN 的更小行为切片。
- 每个 AC 必须先在 `docs/acceptance/bdd-scenarios.md` 中确认 Given/When/Then，再进入测试编写。
- 一个 BDD 场景可以覆盖多个 AC，但每个 AC 必须在 `test-plan.md` 的 62 项验收测试矩阵中拥有独立 `TC-*`、正向断言和失败/边界断言。
- 禁止按“先完成整个 milestone，再统一补测试”的方式开发；milestone 内必须逐 AC 循环。

### Mandatory Sequence

每个 AC 只能按以下状态推进：

`PLANNED -> BDD_APPROVED -> TEST_ADDED -> RED_VALID -> IMPLEMENTING -> GREEN -> REFACTORED -> REGRESSION_PASS -> E2E_PASS -> ACCEPTED`

1. `BDD_APPROVED`：业务、产品和开发确认可观察 Given/When/Then、正式来源和非范围。
2. `TEST_ADDED`：测试类/脚本已创建，测试运行器能发现，tests run 大于 `0`。
3. `RED_VALID`：生产代码尚未修改，失败原因是目标业务行为缺失。
4. `IMPLEMENTING`：只实现令当前 RED 通过的最小正式方案。
5. `GREEN`：重跑同一命令通过，不得换成更弱测试。
6. `REFACTORED`：在测试保护下清除重复、死分支、隐式 fallback 和跨层业务规则。
7. `REGRESSION_PASS`：相邻模块、权限、租户、并发、迁移或快照回归通过。
8. `E2E_PASS`：用户可见行为通过真实 Playwright 页面路径；纯 schema/迁移 AC 必须由其所属真实主链路间接覆盖。
9. `ACCEPTED`：证据写入实施任务 `execution-log.md`，并关联测试报告、业务 ID 和清理结果。

任何阶段失败都保持当前 AC 未完成；不得因为 milestone 其他 AC 通过而跳过。

### Required Test Layers

| 变更类型 | 最低测试层级 |
|---|---|
| 数据表、索引、唯一键、迁移 | Schema/Contract + Migration + Service/API + 所属真实 E2E |
| 领域规则、状态机、数量和聚合 | Service Unit + API + Failure/Boundary + Concurrency（适用时） |
| 页面渲染、输入和操作入口 | Frontend Static/Component + Type Check + Real Playwright |
| 权限、范围和租户 | Backend Authorization + API Negative + Multi-role Real Playwright |
| 历史快照、审计和版本 | Service/API + Snapshot/Audit + 配置变更后的 Real Playwright |
| 列表、分页和性能 | Read-model Test + Query/Index Evidence + Pagination/Performance |
| 放行和跨模块闭环 | Source Adapter + Completeness Service + API + Full Real E2E |

单元测试或静态合同不能单独证明用户可见 AC；真实 E2E 也不能替代 schema、并发、迁移或服务级失败测试。

### Milestone TDD Slices

| 里程碑 | AC 切片 | 首批 RED | 必须完成的回归 |
|---|---|---|---|
| M1 | `AC-M01`、`AC-M03`、`AC-M04` | ERP 候选合同、唯一 activeOrderId、双来源迁移冲突 | 活跃订单、生产组长、PQC 上下文和 schema 回归 |
| M2 | `AC-M10`、`AC-M11`、`AC-M16`、`AC-M17`、`AC-M18`、`AC-M19`、`AC-D11` | 无订单报工、修订链、系数 `3.0`、多事件聚合、并发回填 | 已交付生产组长工作台、正式报工和批记录回归 |
| M3 | `AC-M09`、`AC-M12`、`AC-M13`、`AC-M14`、`AC-M15`、`AC-M20`、`AC-D15`、`AC-D16`、`AC-D17`、`AC-D18`、`AC-D19`、`AC-D20`、`AC-D21`、`AC-D22`、`AC-D23`、`AC-D24`、`AC-D25`、`AC-D26`、`AC-D27`、`AC-D28`、`AC-D29`、`AC-D30`、`AC-D31`、`AC-D32`、`AC-D33`、`AC-D34`、`AC-D35` | 规程发布缺项、301×5%=16、无生产事件 PQC、签名不一致、自我确认 | QA 版本、PQC 上下文、提交复核、过程检验和前端动态表单回归 |
| M4 | `AC-M02`、`AC-M05`、`AC-M06`、`AC-M07`、`AC-M08`、`AC-M21`、`AC-M22`、`AC-M23`、`AC-D36`、`AC-D37` | 多调拨净额、开工缺项、未确认不汇集、真实放行来源 | 调拨、异常、过程检验、eDHR 完整性和放行回归 |
| M5 | `AC-D01`、`AC-D02`、`AC-D03`、`AC-D04`、`AC-D05`、`AC-D06`、`AC-D07`、`AC-D08`、`AC-D09`、`AC-D10`、`AC-D12`、`AC-D13`、`AC-D14`、`AC-D38`、`AC-D39` | 正式设备绑定、范围后端拒绝、日结漏项、三配置分离、历史快照 | 班组配置、权限范围、只读看板、审计、分页和快照回归 |
| M6 | 全部 62 个 AC | 迁移冲突、并发重复终态、权限越界、N+1、真实主链路缺口 | 全量定向回归、六角色真实 E2E、清理和发布门禁 |

### Evidence And Commit Gate

- 实施任务日志必须逐 AC 记录：
  - `BDD: <AC-ID> -> Given/When/Then`
  - `TEST_ADDED: <test> -> discovered, tests run > 0`
  - `RED: <command> -> FAIL, <expected business reason>`
  - `GREEN: <same command> -> PASS`
  - `REFACTOR: <decision> -> no fallback/duplicate rule`
  - `REGRESSION: <command> -> PASS`
  - `E2E: <path> -> PASS`
  - `ACCEPTED: <AC-ID> -> <evidence>`
- 没有有效 RED、同命令 GREEN、相邻回归和适用 E2E 的生产代码不得提交。
- 一个 milestone 只有在其全部 AC 状态为 `ACCEPTED` 且 blocker 为空时才能完成。
- 详细 BDD、TDD、E2E 和测试数据方案位于 `docs/acceptance/`，`test-plan.md` 是 62 项统一验收索引。

## Implementation Steps

### M0 - Freeze Contracts and Baseline

1. 将 `M01-M23`、`D01-D39` 和对应 `AC-*` 固定为实施追踪主键，后续需求变更必须更新矩阵、PRD、测试计划和任务状态。
2. 冻结术语：
   - `工序开始` 只负责特殊开始动作/附件责任。
   - 正式 `批记录表单` 只来自 `MesProRouteFlowProcessBatchRecordDO` 的逐工序绑定。
   - `formBindings` 只负责特殊表单/动态表单槽位。
3. 冻结权威活跃订单：
   - `mes_pro_process_pool_active_order` 是唯一业务聚合。
   - `mes_pro_process_pool` 是订单工序执行投影，不再独立决定订单是否活跃。
   - 生产组长责任关系从活跃订单身份中拆出，支持多个角色读取同一个订单 ID。
4. 冻结状态机：
   - 活跃订单：`CANDIDATE -> ACTIVE -> REMOVED / TERMINATED / COMPLETED`。
   - 原始报工：`SUBMITTED -> REJECTED / APPROVED -> ALLOCATED`，补正形成新修订。
   - QA 规程：`DRAFT -> PUBLISHED -> RETIRED`，发布版本不可原地改写。
   - PQC 任务：`PENDING -> SUBMITTED -> REJECTED / CONFIRMED`，补正形成新提交。
   - 放行：`PRECHECK_REQUIRED -> PRECHECK_FAILED / PRECHECK_PASSED -> PENDING_APPROVAL -> RELEASED / REJECTED`。
5. 冻结数量契约：
   - ERP 订单数量不可被报工分配修改。
   - 工序目标量 = ERP 固定数量 × 路线工序正式生产系数快照。
   - 缺系数或系数不大于零时阻塞，不允许默认 `1`。
6. 完成 ERP 调拨、QA 规程所有权、质量异常/返工/报废/库存来源的正式 source map。任何未确认来源记录为 blocker，不进入实现猜测。

### M1 - Authoritative Sources and Additive Schemas

#### Database

1. 以版本化 migration 扩展 `mes_pro_process_pool_active_order`：
   - 增加正式路线、路线版本、产品、ERP 固定数量快照、业务状态、加入人/加入时间、终止/完成时间、版本号。
   - 将唯一业务键调整为租户 + 生产订单 + 正式路线/路线版本 + 未删除记录。
   - 把生产组长/班组责任拆到独立关系表，避免 `leader_user_id` 成为跨角色订单身份。
2. 新增活跃订单调拨追溯表：
   - 订单-调拨头关系。
   - 调拨物料明细、发货/补料/退料方向、数量、单位、物料批次和状态快照。
   - 唯一键和幂等键必须使用正式 ERP/MES ID，不使用单号文本作为唯一身份。
3. 新增 QA 规程、规程版本、检验项目、检验类型规则表；发布版本保持不可变。
4. 新增 PQC 任务、逐件明细、提交修订和复核字段/表；旧 `mes_pro_process_pool_pqc_record` 只可按正式迁移方案扩展或拆分，不保留两套可写模型。
5. 扩展 `mes_pro_process_pool_team_leader_scope` 支持产线、设备和订单，列设计不得用一个多义 `scope_value` 隐藏外键语义。
6. 为所有状态转换和聚合增加租户级唯一键、乐观锁/版本字段及必要索引。

#### Backend

1. 新增统一 `MesActiveOrderCommandService` / `MesActiveOrderQueryService`，封装加入、移出、状态流转、按角色范围查询和锁定读取。
2. 调整以下现有类只依赖统一服务，不直接从不同 Mapper 判断活跃状态：
   - `MesTeamLeaderActiveOrderServiceImpl`
   - `MesTeamLeaderFifoAllocationService`
   - `MesTeamLeaderReportConfirmationServiceImpl`
   - `MesFrontlinePqcContextServiceImpl`
   - 后续调拨、批记录、完整性和放行服务
3. 增加迁移预检：
   - 同一订单存在多个活跃来源且路线不一致时阻塞。
   - 开放订单缺正式路线版本时阻塞。
   - 历史关闭记录允许保留 legacy 只读状态，但不得猜填后参与新放行。
4. 迁移完成后删除 PQC 对 `mes_pro_process_pool` 活跃行的独立查询路径；禁止保留双读兼容分支。

#### Frontend

1. 复用 `TeamLeaderWorkbenchPage.vue` 活跃订单区块，改为展示统一活跃订单 ID、正式路线、ERP 固定数量和跨链路状态。
2. 复用 `FrontlineFixedTemplatePanel.vue` 的 PQC 订单选择器，通过统一活跃订单 API 查询。
3. 所有错误直接展示正式缺项或冲突，不把空列表改写成“暂无数据但可继续”。

### M2 - Raw Reporting, Review, Allocation, Progress and Batch Record

#### Raw Production Facts

1. 调整生产报工命令/DTO/事件模型，使原始报工只强制：
   - 正式路线工序/工序。
   - 实际员工。
   - 一个或多个正式设备台账设备及参数。
   - 完成数量、损耗数量、损耗原因。
   - 客户端时间和电子签名。
2. 生产订单、生产任务、工作站和活跃订单归属在原始报工阶段不再是必填；若现场已有上下文可保存为非权威快照，但不得用于跳过组长分配。
3. `FrontlineFixedTemplatePanel.vue` 的生产模式改为工序事实优先，不因 URL 缺订单/任务/工作站而阻塞正式报工。
4. 设备只能来自设备台账绑定；当前 `team-device/create` 和独立设备主数据写入路径必须移除或改为“从台账绑定”。

#### Review and Revision

1. 保留 `MesTeamLeaderSubmissionReviewServiceImpl` 原始提交不可覆盖行为。
2. 退回必须保存退回人、原因、时间和源事件；员工补正创建新修订/事件并链接源事件。
3. 已退回、已作废、未确认或已分配事件不得再次进入可分配池。

#### Coefficient-based Allocation

1. `MesTeamLeaderFifoAllocationService` 和 `MesTeamLeaderReportConfirmationServiceImpl` 读取订单正式路线工序的生产系数快照。
2. 每个订单工序保存：
   - ERP 固定订单数量快照。
   - 生产系数快照。
   - 目标数量。
   - 已确认数量和剩余数量。
3. FIFO 只生成建议；确认时对活跃状态、路线工序、总量、剩余量、重复事件和版本号重新加锁校验。
4. 报工分配不更新 `MesProWorkOrderDO.quantity`。

#### Deterministic Batch-record Aggregation

1. `MesTeamLeaderOrderProcessCompletionService` 不再选择代表事件/代表分配触发回填。
2. 新增订单工序事实聚合器，锁定并读取该订单 + 路线工序 + 工序的全部已确认分配及源报工。
3. 扩展批记录字段映射，要求每个多值字段明确聚合策略，例如：
   - 数量：`SUM`。
   - 人员/设备/原因：按提交时间、事件 ID 稳定排序的 `DISTINCT_LIST`。
   - 数值参数：按字段配置使用 `LIST`、`MIN`、`MAX`、`FIRST`、`LAST` 或明确统计策略。
   - 未配置或目标单元格无法承载多值时阻塞，不取代表事件。
4. 回填幂等键基于订单工序完成聚合版本，而不是单个 event ID。
5. 保留正式绑定读取；不得使用 `formBindings`、默认 `MAIN` 或 `工序开始` 替代。

### M3 - Versioned QA Regulation and Complete PQC Flow

#### QA Regulation

1. 复用现有质量指标/质检方案能力前，先确定 MES/QMS 所有权和唯一写入口。
2. QA 规程页面按产品、正式路线版本和路线工序展示：
   - 工序基础信息、是否质检、SOP、生产系数。
   - 正式逐工序批记录表单绑定。
   - 不展示 `formBindings` 作为批记录表单。
3. 规程项目支持项目、方法、工具、结果类型、标准、上下限、关键项和失败规则。
4. 首检必须配置固定数量。
5. 上午/下午巡检分别配置比例、项目和记录要求，数量向上取整。
6. 末检必须显式配置适用或不适用；适用时配置数量/规则和项目。
7. 草稿保存执行字段/冲突校验；完整性检查逐项列缺失；发布生成不可变版本快照。

#### PQC Task and Submission

1. PQC 任务通过统一活跃订单、正式路线工序和发布规程生成，不依赖最新生产事件。
2. 任务唯一键包含活跃订单、路线工序、检验类型、业务日期、班次、轮次和规程版本。
3. 跨天/跨班次：
   - 已提交/已确认任务保持原任务和规程快照。
   - 新业务日期按新规程和订单数量生成新任务。
   - 重复调度命中唯一键，不生成重复任务。
4. `MesProProcessPoolPqcRecordDO` 或替代正式模型必须保存：
   - `inspectionType`、`businessDate`、`shiftCode`、`roundNo`。
   - `regulationVersionId`、`plannedInspectionQuantity`、`actualInspectionQuantity`。
   - 实际 PQC 人、签名快照、提交结果、复核状态和修订来源。
5. 逐件明细使用独立行模型，支持数值、判断和枚举结果；整批汇总必须可从逐件数据重算。
6. `FrontlineFixedTemplatePanel.vue` 删除固定项目、默认 `PATROL`、默认数量 `30` 和默认损耗 `1`，完全按任务/规程渲染。
7. 缺活跃订单、路线、规程、实际人员或签名时阻塞；不得默认人员、数量、项目或合格结果。

#### PQC Review

1. PQC 组长列表支持订单、产品、工序、类型、轮次、检验员、日期和状态筛选。
2. 详情展示逐件明细、结果、原因、签名、规程版本和原始提交。
3. `MesTeamLeaderSubmissionReviewServiceImpl` 在 PQC leader 类型确认时校验确认人不等于实际检验人。
4. 退回后原始提交保持只读，补正形成新修订和新签名；只有最终已确认修订进入过程检验。

### M4 - Transfer Trace, Start Check, Anomaly, Completeness and Release

#### Transfer Trace and Start Check

1. 先确认 ERP/MES 调拨正式 source map，再实现适配器；不得通过名称模糊匹配或新建重复调拨主数据。
2. 活跃订单可关联多个调拨单和多个物料行，记录发货、补料、退料方向、数量、单位、批次和状态。
3. 物料覆盖计算按订单所需物料与净发货事实比较，并保留每个来源行。
4. 开工检查至少包含：
   - 权威活跃订单与正式路线。
   - 调拨关联、物料、数量、批次和发货状态。
   - SOP、生产系数、正式批记录绑定和发布 QA 规程。
5. 检查失败只阻塞系统“就绪/通过”状态和后续放行，不阻止原始事实保存，也不自动创建异常；班组长可主动上报生产异常。

#### Process Inspection and Anomaly

1. PQC 已确认提交按订单、产品、路线版本、路线工序、类型、轮次、项目和规程版本汇集过程检验记录。
2. 汇集使用任务/提交唯一键幂等；退回或旧修订不参与完成。
3. 生产异常和质量异常使用独立类型、责任角色、处理入口、状态和阻塞规则。
4. 所有阻塞异常必须返回来源 ID、当前状态、责任角色、解除条件和操作入口。

#### Completeness and Release

1. 新增完整性编排服务，按活跃订单/批次检查：
   - 全部订单工序达到系数目标。
   - 每个工序存在正式批记录执行且必填项完成。
   - 所有必需 PQC 任务已被不同人员确认并汇集。
   - 调拨/物料/批次追溯覆盖完整。
   - 阻塞生产异常和质量异常已关闭或具有正式可放行决策。
   - 必需电子签名存在且身份一致。
2. 调整 `MesProEdhrReleaseServiceImpl.buildCheckItems`：
   - 用正式来源适配器替换“来源未接入”占位项。
   - 对尚无正式来源的检查继续返回 blocker，不得返回 pass 或 not applicable。
   - 检查项保存来源模块、对象类型、对象 ID、结果、原因和处理建议。
3. 只有完整性通过才允许生成/提交放行待办。
4. 放行和退回都必须电子签名并记录原因；退回回到明确责任角色，补齐后重新预检。

### M5 - Daily Close, Read Models, Scope, Permission, Audit and Snapshots

1. 班组配置：
   - 添加/禁用员工。
   - 维护不良和损耗原因。
   - 从正式设备台账绑定工序设备。
   - 报修/禁用设备不可用于新报工。
   - 维护参数上下限和超限提醒，绝不覆盖员工原始值。
2. 责任范围扩展到员工、工序、工作站、产线、设备和订单；后端每个查询/写入都校验，前端隐藏按钮不能替代后端授权。
3. 班组日结展示订单、调拨、报工、分配、PQC、批记录、异常和次日延续事项。
4. PQC 日结展示未提交、未确认、退回未补正、不合格未处理和影响放行事项。
5. 生产班组长只读查看 PQC 状态和批记录进度，不获得填写/确认质量记录的权限。
6. 配置、规程、人员、设备、原因、签名和范围变更保留历史快照；新配置只影响后续任务/提交。
7. 所有维护、发布、确认、退回、分配、回填、异常和放行操作写入审计日志。
8. 一对多读模型先在子查询/服务中聚合再分页，避免调拨、PQC 明细和审计 JOIN 扩大总数。

### M6 - Migration, Concurrency, Real E2E and Release Acceptance

1. 执行迁移预检并输出：
   - 双活跃来源冲突。
   - 开放订单缺路线版本/系数。
   - 开放 PQC 缺任务身份/规程版本。
   - 正式批记录绑定缺失或冲突。
2. 只迁移能确定性映射的数据；无法映射的开放数据阻塞切换。关闭历史可标记 legacy 只读，但不得参与新完成/放行。
3. 并发覆盖：
   - 同一订单重复加入/移出。
   - 同一报工重复确认或超额分配。
   - 同一 PQC 任务重复提交/确认。
   - 同一订单工序重复完成/回填。
   - 同一放行事务重复预检/放行。
4. 性能覆盖：
   - 活跃订单、调拨覆盖、组长待办、PQC 待办、日结和放行列表按生产规模验证索引和分页。
   - 大量逐件明细不得通过 N+1 查询加载。
5. 新增真实 Playwright E2E 脚本和 package scripts，覆盖登录、菜单、角色切换、签名、页面写入、只读 API 核验和任务数据清理。
6. 正式 E2E 数据使用任务前缀和真实业务 ID，清理只删除任务自有数据；审计/签名 append-only 证据按治理规则保留。
7. 全部定向回归、类型检查、真实 E2E、迁移、并发、权限、审计和清理通过后，才允许进入发布验收。

## Verification Gates

### Universal TDD Gate

- 每个行为先创建能编译和执行的测试，再运行 RED。
- 缺测试类、缺脚本、No tests、缺数据库/账号/运行服务不算有效 RED，只能记录为 blocker。
- GREEN 必须重跑同一 RED 命令；随后执行 REFACTOR、相邻模块 REGRESSION 和适用真实 E2E。
- 每个 AC 必须在 `test-plan.md` 的验收测试矩阵中拥有独立 `TC-*`，不得只用 BDD 范围表达代替逐项测试。
- 生产代码提交必须包含对应测试改动和 BDD/RED/GREEN/REFACTOR/REGRESSION 证据。
- 测试类和脚本名称以下为计划新增项，不代表当前仓库已经存在。

### M0 Gate

- 结构合同：62 个需求 ID、62 个 AC、16 个 BDD 场景和 M0-M6 映射完整。
- 测试合同：62 个 AC、62 个唯一 `TC-*`、正向断言和失败/边界断言全部显式存在。
- `bdd-tdd-acceptance-planner` 验证四份 `docs/acceptance/` 文档通过。
- 静态搜索确认计划没有把 `formBindings`、`MAIN` 或 `工序开始` 写成正式批记录替代来源。
- source map 中任何未知正式来源必须保持 blocker。

### M1 Gate

计划新增测试后执行：

```powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesActiveOrderAuthorityServiceTest,MesActiveOrderMigrationContractTest,MesActiveOrderSchemaTest,MesFrontlinePqcActiveOrderAuthorityTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

RED 必须证明 PQC 仍读取旧活跃行、同一订单多来源冲突或迁移缺路线版本；GREEN 必须证明所有角色读取同一 activeOrderId 且无双读分支。

### M2 Gate

计划新增/扩展测试后执行：

```powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesFrontlineProductionFactSubmitServiceTest,MesTeamLeaderSubmissionRevisionServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderBatchRecordAggregationServiceTest,MesTeamLeaderBatchRecordBackfillServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
pnpm --dir IntRuoyiFronted test e2e:frontline-formal-submit:static
pnpm --dir IntRuoyiFronted test e2e:team-leader-report-allocation:static
pnpm --dir IntRuoyiFronted ts:check
```

RED 必须分别证明订单上下文仍被强制、生产系数未应用、代表事件丢失多次报工和并发重复回填；GREEN 必须验证系数 `1.0` / `3.0`、多员工/多设备/多事件聚合和幂等。

### M3 Gate

计划新增测试和前端静态合同后执行：

```powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesQaInspectionRegulationServiceTest,MesQaInspectionRegulationPublishServiceTest,MesPqcTaskGenerationServiceTest,MesFrontlinePqcContextServiceTest,MesPqcSubmissionServiceTest,MesTeamLeaderSubmissionReviewServiceTest,MesPqcProcessInspectionAggregationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
pnpm --dir IntRuoyiFronted test e2e:role-matrix-qa-regulation:static
pnpm --dir IntRuoyiFronted test e2e:role-matrix-pqc-dynamic-form:static
pnpm --dir IntRuoyiFronted ts:check
```

实施时必须先在 `IntRuoyiFronted/package.json` 新增上述两个静态脚本；脚本缺失时记录 blocker，不得声称命令可用。

### M4 Gate

计划新增测试后执行：

```powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesActiveOrderTransferTraceServiceTest,MesActiveOrderStartCheckServiceTest,MesQualityAbnormalServiceTest,MesPqcProcessInspectionAggregationTest,MesOrderReleaseCompletenessServiceTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
pnpm --dir IntRuoyiFronted test e2e:role-matrix-transfer-start-check:static
pnpm --dir IntRuoyiFronted test e2e:edhr:release:check
pnpm --dir IntRuoyiFronted ts:check
```

RED 必须证明多调拨覆盖、阻塞异常或真实放行来源仍缺失；GREEN 不得通过把“来源未接入”改为默认 PASS。

### M5 Gate

计划新增测试后执行：

```powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesTeamLeaderRuntimeConfigServiceTest,MesTeamLeaderScopeServiceTest,MesTeamLeaderDailyCloseServiceTest,MesPqcDailyCloseServiceTest,MesRoleMatrixReadModelServiceTest,MesRoleMatrixHistorySnapshotTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
pnpm --dir IntRuoyiFronted test e2e:role-matrix-daily-close-scope:static
pnpm --dir IntRuoyiFronted ts:check
```

权限验证必须使用生产员工、生产组长、QA、PQC 检验员、PQC 组长和放行负责人真实角色，不得只检查按钮隐藏。

### M6 Gate

实施时新增以下 package scripts：

- `e2e:role-requirement-matrix:real:check`
- `e2e:role-requirement-matrix:real`

前置齐全后执行：

```powershell
pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check
pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real
pnpm --dir IntRuoyiFronted test e2e:team-leader-workbench:static
pnpm --dir IntRuoyiFronted test e2e:frontline-formal-submit:static
pnpm --dir IntRuoyiFronted test e2e:frontline-team-config:static
pnpm --dir IntRuoyiFronted test e2e:team-leader-report-allocation:static
pnpm --dir IntRuoyiFronted e2e:edhr:release:check
pnpm --dir IntRuoyiFronted ts:check
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesProcessPoolTeamLeaderSchemaTest,MesFrontlineRuntimeConfigControllerTest,MesFrontlineRuntimeConfigServiceTest,MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderRuntimeConfigServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesTeamLeaderFifoAllocationServiceTest,MesTeamLeaderOrderProcessCompletionServiceTest,MesTeamLeaderBatchRecordBackfillServiceTest,MesProEdhrReleaseServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

真实 E2E 必须记录前后端 URL、租户、角色账号标签、数据前缀、订单/路线/调拨/规程/签名 ID、页面断言、最终只读核验和清理结果。

## Excel Traceability Matrix

矩阵中的“可观察验收”同时继承源 Excel 对应行的“输出什么”和“怎么测试”。ID 只在本矩阵中各出现一次。

<!-- TRACEABILITY_MATRIX_START -->
| ID | Excel 任务 | 里程碑 | 实施区域 | BDD | Acceptance | 可观察验收 |
|---|---|---|---|---|---|---|
| M01 | 确认生产订单 | M1 | A1 | BDD-01 | AC-M01 | ERP 已确认订单可在候选列表按正式订单 ID/编号查询，MES 不替代确认动作 |
| M02 | 填写调拨申请单 | M4 | A2 | BDD-02 | AC-M02 | ERP 调拨申请同步后可追溯，MES 不提供创建/编辑入口 |
| M03 | 同步 ERP 候选数据 | M1 | A1 | BDD-01 | AC-M03 | 订单、调拨、发货和批次按正式 ID 同步一致，未自动进入生产执行 |
| M04 | 加入活跃订单池 | M1 | A1 | BDD-01 | AC-M04 | 加入后生产、PQC、批记录和放行读取同一 activeOrderId |
| M05 | 生成调拨单并发货 | M4 | A2 | BDD-02 | AC-M05 | ERP 发货后 MES 可读取调拨号、物料、数量、批次和发货状态 |
| M06 | 核对并解包到线边仓 | M4 | A2 | BDD-02 | AC-M06 | 实物核对结果和批次追溯可记录，缺失/不一致进入开工检查缺项 |
| M07 | 关联调拨单 | M4 | A2 | BDD-02 | AC-M07 | 一个活跃订单可关联多调拨/多批次并追溯关联人、时间和数量 |
| M08 | 订单开工检查 | M4 | A2 | BDD-02 | AC-M08 | 系统展示逐项结果和阻塞原因，不自动生成异常或替班组长决策 |
| M09 | 维护检验规程 | M3 | A5 | BDD-07 | AC-M09 | 产品/路线版本/工序规程完整后可发布，缺首检或巡检规则时阻塞 |
| M10 | 按 SOP 生产 | M2 | A4 | BDD-04 | AC-M10 | 员工可先按 SOP 生产，系统不强制生产前选择订单或登记订单上下文 |
| M11 | 生产报工 | M2 | A4 | BDD-04 | AC-M11 | 工序、人员、设备、参数、数量、损耗、原因和签名保存且原始事实不可覆盖 |
| M12 | 执行首检 | M3 | A5 | BDD-08 | AC-M12 | 每个适用订单工序按发布规程生成固定数量首检并支持逐件提交 |
| M13 | 执行上午巡检 | M3 | A5 | BDD-08 | AC-M13 | 上午任务按订单数量和上午比例向上取整，保存日期/班次/轮次 |
| M14 | 执行下午巡检 | M3 | A5 | BDD-08 | AC-M14 | 下午任务按订单数量和下午比例向上取整，与上午任务身份分离 |
| M15 | 执行末检 | M3 | A5 | BDD-08 | AC-M15 | 规程要求时生成末检，不适用时保存明确依据且不误卡放行 |
| M16 | 确认员工报工 | M2 | A4 | BDD-05 | AC-M16 | 通过后进入分配，退回记录原因和修订链，原始提交保持不变 |
| M17 | 分配报工到生产订单 | M2 | A4 | BDD-05 | AC-M17 | FIFO 建议/手工调整都只分配活跃订单，目标量按固定数量乘系数 |
| M18 | 更新生产订单进度 | M2 | A4 | BDD-05 | AC-M18 | 只更新工序已完成/剩余/状态，ERP 产品数量保持不变 |
| M19 | 写入工序批记录表单 | M2 | A4 | BDD-06 | AC-M19 | 全部已确认报工按明确策略汇总到正式逐工序批记录，并发不重复回填 |
| M20 | 确认 PQC 检验单 | M3 | A5 | BDD-10 | AC-M20 | PQC 组长可确认或退回，未确认/退回不算完成且自我确认被阻塞 |
| M21 | 汇集过程检验记录 | M4 | A5 | BDD-11 | AC-M21 | 只把最终已确认修订按任务、轮次、项目和规程版本汇集 |
| M22 | 检查批记录完整性 | M4 | A6 | BDD-12 | AC-M22 | 缺任一工序批记录、PQC、调拨、签名或存在阻塞异常时不生成可放行结果 |
| M23 | 审核并放行生产订单 | M4 | A6 | BDD-12 | AC-M23 | 放行负责人查看完整来源并签名放行或退回，结果和原因可审计 |
| D01 | 添加本班组员工 | M5 | A3 | BDD-03 | AC-D01 | 新员工进入班组范围并可用于后续绑定，历史不受影响 |
| D02 | 禁用本班组员工 | M5 | A3 | BDD-03 | AC-D02 | 禁用后不可用于新报工/绑定，历史记录仍显示当时人员 |
| D03 | 维护不良原因 | M5 | A3 | BDD-03 | AC-D03 | 新报工只选择当前工序启用的不良原因，历史原因快照保留 |
| D04 | 维护损耗原因 | M5 | A3 | BDD-03 | AC-D04 | 损耗原因按工序配置并用于新报工，不用固定前端列表 |
| D05 | 绑定工序可用设备 | M5 | A3 | BDD-03 | AC-D05 | 只能从正式设备台账绑定，不能创建重复设备主数据 |
| D06 | 设备报修或禁用后的可选控制 | M5 | A3 | BDD-03 | AC-D06 | 报修/禁用设备不出现在新报工选择中，恢复后重新可用 |
| D07 | 维护设备参数上下限 | M5 | A3 | BDD-03 | AC-D07 | 默认值必须在上下限内，单位和规则按设备/工序保存 |
| D08 | 超限参数复核提醒 | M5 | A3 | BDD-03 | AC-D08 | 超限值被标记并提醒复核，员工原始值不被改写 |
| D09 | 配置负责范围 | M5 | A3 | BDD-13 | AC-D09 | 员工、工序、工作站、产线、设备和订单范围均在后端生效 |
| D10 | 班组基础维护审计 | M5 | A3 | BDD-13 | AC-D10 | 新增、禁用、修改记录操作人、时间、前后值和生效范围 |
| D11 | 退回员工报工并保留修订记录 | M2 | A4 | BDD-05 | AC-D11 | 原始提交、退回原因和补正提交可串联追溯 |
| D12 | 班组日结与未完成提醒 | M5 | A6 | BDD-13 | AC-D12 | 未分配报工、未确认 PQC、批记录缺项和次日延续逐项显示 |
| D13 | 只读查看 PQC 状态 | M5 | A6 | BDD-13 | AC-D13 | 生产组长可看状态但后端拒绝填写或确认 PQC |
| D14 | 查看批记录进度 | M5 | A6 | BDD-16 | AC-D14 | 按正式批记录绑定显示工序进度，缺绑定明确阻塞且不读表单槽位 |
| D15 | 按产品/路线/版本/工序维护检验规程 | M3 | A5 | BDD-07 | AC-D15 | 同一产品不同路线工序可发布不同规程并生成对应任务 |
| D16 | 查看工序基础信息 | M3 | A5 | BDD-07 | AC-D16 | QA 页面显示正式 SOP、生产系数和逐工序批记录绑定 |
| D17 | 配置检验项目和标准 | M3 | A5 | BDD-07 | AC-D17 | PQC 页面完全按规程项目、方法、工具、标准和判定规则渲染 |
| D18 | 配置首检规则 | M3 | A5 | BDD-07 | AC-D18 | 缺固定数量或项目时不能发布，发布后任务带出固定数量 |
| D19 | 配置上午/下午巡检规则 | M3 | A5 | BDD-07 | AC-D19 | 两个轮次独立配置；301 × 5% 向上取整为 16 |
| D20 | 配置末检规则 | M3 | A5 | BDD-07 | AC-D20 | 需要/不适用必须显式保存并参与任务和放行判断 |
| D21 | 保存草稿校验 | M3 | A5 | BDD-07 | AC-D21 | 缺产品、路线、工序、项目或规则冲突时返回明确字段错误 |
| D22 | 检查检验规程完整性 | M3 | A5 | BDD-07 | AC-D22 | 按产品/路线版本/工序逐项列出缺失规则 |
| D23 | 发布或启用规程版本 | M3 | A5 | BDD-07 | AC-D23 | 发布版本不可改写，新任务用新版本，历史任务保留旧版本 |
| D24 | 选择活跃订单和路线工序 | M3 | A5 | BDD-09 | AC-D24 | 只能选统一活跃订单及其正式路线工序，终止/缺路线时阻塞 |
| D25 | 选择实际 PQC 人员 | M3 | A5 | BDD-09 | AC-D25 | 共享账号下保存实际检验人，不默认用登录人冒充 |
| D26 | 电子签名提交 | M3 | A5 | BDD-09 | AC-D26 | 签名人与实际检验人一致，保存签名 ID、时间和快照 |
| D27 | 逐件填写检验明细 | M3 | A5 | BDD-09 | AC-D27 | 计划数量为 30 时必须保存 30 件可还原明细，不只保存整批结果 |
| D28 | 填写不合格、损耗和失败原因 | M3 | A5 | BDD-09 | AC-D28 | 不合格/损耗数量、原因和说明参与最终判定并进入组长复核 |
| D29 | 生成工序池 PQC 事件 | M3 | A5 | BDD-09 | AC-D29 | 提交后生成可追溯 PQC 事件/提交并在组长待办中可见 |
| D30 | 处理 PQC 组长退回 | M3 | A5 | BDD-10 | AC-D30 | 退回原因、原始提交、补正内容和新签名形成修订链 |
| D31 | 缺失前置条件阻塞 | M3 | A5 | BDD-14 | AC-D31 | 缺订单、路线、规程、人员或签名逐项 fail fast，无默认成功 |
| D32 | 筛选待确认检验提交 | M3 | A5 | BDD-10 | AC-D32 | 订单、产品、工序、类型、轮次、人员、日期和状态筛选准确 |
| D33 | 查看 PQC 提交详情 | M3 | A5 | BDD-10 | AC-D33 | 详情展示逐件明细、原因、签名、规程版本和原始 payload |
| D34 | 确认或退回检验提交 | M3 | A5 | BDD-10 | AC-D34 | 确认/退回均留记录，退回不参与过程检验和放行完整性 |
| D35 | 确认人与实际检验人隔离 | M3 | A5 | BDD-10 | AC-D35 | 同一实际检验人和确认人组合被后端阻塞 |
| D36 | 跟进质量异常 | M4 | A6 | BDD-11 | AC-D36 | 不合格可形成质量异常并有独立状态，不进入生产异常处理流 |
| D37 | 确认过程检验记录汇集 | M4 | A6 | BDD-11 | AC-D37 | 组长确认后汇集可见，未确认/退回不算完成 |
| D38 | 过程检验闭环和日结提醒 | M5 | A6 | BDD-13 | AC-D38 | 未提交、未确认、退回未补正、不合格未处理和影响放行逐项提示 |
| D39 | 衍生配置不改写历史数据 | M5 | A6 | BDD-13 | AC-D39 | 修改人员、设备、原因、参数或规程后，历史记录仍显示原快照 |
<!-- TRACEABILITY_MATRIX_END -->

## Rollback or Stop Conditions

### Stop Conditions

- ERP/调拨、QA 规程、异常、返工、报废或库存正式来源未确认。
- 两套活跃订单数据无法确定性归并，或开放订单缺正式路线版本。
- 正式生产系数、正式批记录绑定或字段聚合策略缺失。
- 同一文件/分支出现无法区分的并行任务改动。
- 测试租户、角色权限、签名、数据库、Redis、浏览器或运行服务缺失。
- RED 只能通过缺测试类、缺脚本或 No tests 产生。
- 任一实现试图引入默认订单、默认工序、默认人员、默认数量、默认合格、双读、代表事件或 `formBindings` 替代正式来源。

### Rollback Strategy

- 数据库先采用增量 schema 和迁移预检，正式切换前不删除旧列/表；预检失败时停止切换，不启用双读 fallback。
- 活跃订单、PQC 和放行使用一次性服务切换；验证失败则回滚本次发布和对应 migration，恢复到发布前备份，禁止在运行时静默切回旧算法。
- 发布后若发现数据冲突，停止新状态转换并按 `docs/release-backup-restore.md` 的正式备份/恢复流程处理；不得直接改生产数据或猜填缺失字段。
- E2E 或并发失败时保留任务自有证据和数据标识，先修复根因再复跑；不得删除失败证据后标记通过。

## Definition of Done

- 62 个需求和 62 个验收项全部完成并可从 Excel 行追踪到实现、测试和证据。
- 62 个 AC 均按 BDD -> TEST_ADDED -> RED -> GREEN -> REFACTOR -> REGRESSION -> E2E -> ACCEPTED 完成。
- 62 个唯一 `TC-*` 的正向和失败/边界断言全部通过，需求覆盖率为 `62/62`。
- 生产、PQC、调拨、批记录和放行读取同一权威活跃订单。
- 生产报工可先记录工序事实，订单归属由组长分配。
- 目标数量按固定 ERP 数量乘正式生产系数，ERP 数量不变。
- QA 规程版本、PQC 任务、逐件明细、签名、复核和修订链完整。
- 正式批记录汇总包含全部员工、设备和多次报工，不使用代表事件。
- 开工检查、异常、日结、完整性和放行均有真实来源和可解释阻塞。
- 三类工艺路线配置互不替代。
- 后端定向回归、前端静态合同、类型检查、真实 Playwright E2E、权限、并发、迁移、审计和清理全部通过。
