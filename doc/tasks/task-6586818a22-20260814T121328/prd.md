# PRD：批记录表单导入时重建工艺路线候选版本

- Task ID: task-6586818a22-20260814T121328
- Created: 2026-08-14T12:13:28
- Workspace: E:\IntRuoyi
- User Request: 将批记录表单导入、勾选工艺流程后重建工艺路线、已有路线生成候选版本并保留旧绑定关系的业务需求，根据当前代码分析后写成 PRD、开发计划和测试计划。

## Goal

批记录表单 Word 导入时，只有用户明确勾选“工艺流程”，系统才按 Word 中识别出的工序顺序重建工艺路线。没有现有路线时创建新的工艺路线、工序、流程关系并绑定 DCC 项目代码；已有路线时不覆盖当前生效路线，而是生成或更新一个 DRAFT 路线候选版本，候选发布后才影响正式生产路线。

升版场景下，导入动作升级的是工序节点和顺序，不是清空重配整条路线。因此旧路线中已经正式配置的逐工序批记录表单绑定、表单槽位 formBindings 和工序开始节点配置，必须迁移到候选版本中可唯一映射的新工序节点。工序结束目前没有独立绑定关系，本需求不新增“工序结束绑定”概念。

## Scope

- 批记录表单 Word 导入弹窗、预检、提交参数和提示文案。
- 后端批记录表单导入识别、路线目标解析、DCC 项目代码校验、路线候选版本创建或更新。
- 已有路线升版时的候选快照生成逻辑，包括 flowGraph.nodes、普通/START/END 流程关系、batchUseConfigs.batchRecordReports、batchUseConfigs.formBindings、routeStartProductionLeaders 和 batchRecordAttachmentOwners。
- 候选版本发布投影，确保候选快照发布后正式路线仍保留正确绑定。
- 后端目标单元/数据库测试、前端静态合同测试、必要的真实页面 E2E 测试设计。

## Non-Goals

- 不在导入时直接覆盖 ACTIVE 生效路线。
- 不在导入时自动发布、提交审批或跳过候选版本流程。
- 不新增“工序结束绑定关系”，也不把 END 边界节点解释成业务绑定配置。
- 不用 formBindings、默认 MAIN、空值、当前登录人、工序开始上传人或前端文案推断正式批记录表单绑定。
- 不改变 DCC 项目代码作为路线绑定身份的正式来源。
- 不处理非 Word 导入、Excel 工艺路线导入或人工编辑路线的完整重构，除非这些链路复用相同的候选快照发布机制。

## Preconditions

- Word 导入请求必须携带唯一启用的 DCC 项目代码 ID，且其产品名称与批记录名称一致。
- 已有路线场景必须能按正式 DCC 路线绑定或允许的 DCC 项目代码到物料路线绑定规则定位到唯一目标路线。
- 已有路线必须存在当前 ACTIVE 路线版本；若存在未结束候选，只允许同源 DRAFT 经用户确认后更新。
- 前端预检返回的 expectedRouteId、expectedRouteVersionId、expectedRouteCandidateVersionId 必须在提交时回传并由后端最终加锁复核。
- 旧路线工序到新候选工序必须能形成可审计的唯一映射。建议以正式 processId 加出现序号建立映射，并保留 routeProcessId 快照用于发布投影。
- 测试环境需要可运行 Maven、Node/pnpm、必要时可启动本地 int_main 前后端并使用真实浏览器。

## Impacted Areas

- 后端导入入口：IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordReportServiceImpl.java
- 后端路线生成：IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordRouteGenerationServiceImpl.java
- 后端候选快照保存：IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteCandidateConfigServiceImpl.java
- 后端候选发布投影：IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/route/MesProRouteVersionPublishProjectionServiceImpl.java
- 后端流程配置与三类配置来源：MesProRouteFlowConfigServiceImpl 及 flowconfig VO。
- 前端导入页面：IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue
- 前端导入 API：IntRuoyiFronted/src/api/mes/pro/batchrecordreport/index.ts
- 现有相邻测试：batch-record-word-import-route-candidate-static、batch-record-word-import-production-upgrade-dedupe-static、batch-record-word-import-dcc-identity-static、MesProBatchRecordRouteGovernanceContractTest、MesProBatchRecordRouteCandidateGovernanceTest。

## Phase Plan

### P1: 明确导入入口和用户确认边界

- Objective: 让“工艺流程”勾选项成为是否重建路线的唯一用户动作边界。
- Owned paths: 前端批记录表单列表导入页、前端导入 API、批记录导入 Controller 请求参数。
- Dependencies: 当前预检已返回 routeUpgradeRequired、currentRouteId、currentRouteVersionId、currentRouteCandidateVersionId。
- Deliverables: 未勾选时只导入或升版批记录表单，不按 Word 工序顺序重建工序节点、流程边或 START/END 边界；勾选时才进入路线新建/候选升版提示和提交参数。若后续批记录表单绑定本身需要候选承载，必须保持原 ACTIVE 的 flowGraph 节点和关系不变，并与“工艺流程重建”分开表达。

### P2: 固化后端路线目标和候选版本治理

- Objective: 后端以 DCC 项目代码和预检冻结 ID 精确定位路线，已有路线只创建或更新 DRAFT 候选。
- Owned paths: MesProBatchRecordReportServiceImpl、MesProBatchRecordRouteGenerationServiceImpl、路线候选状态相关错误码和测试。
- Dependencies: DCC 项目代码正式绑定、ACTIVE 版本、未结束候选状态。
- Deliverables: 新路线创建 ACTIVE 初始版本；已有路线创建或更新 DRAFT 候选，不改 ACTIVE。

### P3: 升版候选保留旧工序配置

- Objective: 在候选快照中把旧路线可唯一映射的逐工序配置迁移到新工序节点。
- Owned paths: MesProBatchRecordRouteGenerationServiceImpl、MesProRouteFlowConfigServiceImpl、候选快照结构测试。
- Dependencies: 旧路线当前配置可读、旧工序到新工序可唯一映射、配置类型边界清楚。
- Deliverables: 候选版本节点按 Word 顺序生成，同时保留正式批记录表单绑定、formBindings、工序开始配置；不生成工序结束绑定。

### P4: 发布投影与运行态回归

- Objective: 候选发布后，正式 ACTIVE 路线按候选快照投影，且三类配置仍在正确链路上。
- Owned paths: MesProRouteVersionPublishProjectionServiceImpl、发布审批/投影测试、批次执行相关只读验证。
- Dependencies: 候选快照完整并能被发布投影读取。
- Deliverables: 发布后正式路线节点、关系和绑定一致；发布前 ACTIVE 不变。

## Phase Acceptance Criteria

### P1

- P1-AC1: Given 用户上传 Word 但未勾选“工艺流程” / When 提交导入 / Then 系统不得按 Word 工序顺序重建工艺路线的工序节点、普通流程边或 START/END 边界；如仅因批记录表单绑定升版需要候选承载，该候选必须沿用原 ACTIVE 的 flowGraph，不得重排或重建工艺流程。
- P1-AC2: Given 用户勾选“工艺流程”且预检发现已有路线 / When 提交前确认 / Then 页面明确提示“生成或更新候选版本，发布后生效”，并回传预检冻结的路线和候选 ID。
- P1-AC3: Given 预检发现候选状态为 PENDING_APPROVAL 或 READY_TO_PUBLISH / When 用户尝试继续导入 / Then 前端阻断并提示先撤回、取消或完成发布。
- Evidence expectation: 前端静态合同测试和 API 参数断言。

### P2

- P2-AC1: Given 请求勾选工艺流程但缺少 dccProjectCodeId、expectedRouteId 或 expectedRouteVersionId / When 后端处理 / Then 后端 fail fast，不按名称猜测路线或 DCC 身份。
- P2-AC2: Given 所选 DCC 项目没有现有路线 / When 导入 / Then 系统新建路线、工序、流程关系、DCC 项目代码绑定和初始 ACTIVE 版本。
- P2-AC3: Given 所选 DCC 项目已有唯一 ACTIVE 路线 / When 用户确认升版导入 / Then 系统创建或更新同源 DRAFT 候选版本，ACTIVE 路线工序、流程和配置保持不变。
- P2-AC4: Given 预检后 ACTIVE 版本或候选版本发生变化 / When 提交导入 / Then 后端最终校验失败，不继续写入。
- Evidence expectation: 后端数据库测试和现有候选治理合同测试。

### P3

- P3-AC1: Given Word 中识别出新的工序顺序 / When 已有路线升版生成候选 / Then 候选 flowGraph.nodes 按 Word 顺序生成，节点包含正式 processId 和可发布投影的 routeProcess 身份。
- P3-AC2: Given 旧路线某工序存在正式批记录表单绑定 / When 该旧工序唯一映射到候选新工序 / Then 候选 batchUseConfigs 中该新工序保留正式批记录表单绑定，且不得由 formBindings 或默认 MAIN 推断。
- P3-AC3: Given 旧路线存在 formBindings 表单槽位配置 / When 升版候选生成 / Then 该配置只作为表单槽位迁移到对应新工序，不改变“批记录表单”字段来源。
- P3-AC4: Given 旧路线存在工序开始配置 / When 升版候选生成 / Then routeStartProductionLeaders、batchRecordAttachmentOwners 等 START 相关上传人、附件负责人或同类开始节点配置保留到候选；END 只作为流程边界节点保留，不生成结束绑定。
- P3-AC5: Given 旧配置所在工序在 Word 中缺失或重复导致无法唯一映射 / When 导入 / Then 后端阻断并提示具体工序，不静默丢配置。
- Evidence expectation: 后端 RED/GREEN 测试覆盖配置迁移和失败分支。

### P4

- P4-AC1: Given 候选版本发布 / When 发布投影执行 / Then 正式路线按候选节点和关系投影，同时保留候选中的批记录表单绑定、formBindings 和工序开始配置。
- P4-AC2: Given 候选尚未发布 / When 批次执行或路线配置读取当前生效路线 / Then 仍读取原 ACTIVE 路线配置，不提前使用候选。
- P4-AC3: Given 发布投影缺少必要配置快照 / When 发布 / Then fail fast，不使用空配置、旧 ACTIVE 或默认值冒充成功。
- Evidence expectation: 发布投影单测/数据库测试、真实页面只读或写入 E2E 证据。

## Done Definition

- PRD、开发计划、测试计划均落盘在本任务目录。
- 开发前 RED 测试覆盖当前缺口：已有路线升版候选未保留旧绑定关系。
- 实现后所有 P1 到 P4 acceptance id 都有 execution-log.md 或 test-report.md 证据。
- 后端目标测试、前端静态合同、必要的真实浏览器 E2E 均通过或因缺少明确前置而 fail fast 记录。
- 不引入 fallback、静默降级、mock 成功或直接 SQL 修补。

## Blocking Conditions

- DCC 项目代码缺失、不唯一、停用或与批记录名称不一致。
- 当前路线缺 ACTIVE 版本，或路线正式 DCC 绑定在预检和提交之间漂移。
- 存在 PENDING_APPROVAL 或 READY_TO_PUBLISH 候选版本。
- 旧工序配置无法唯一映射到 Word 新工序节点。
- 批记录表单正式绑定来源缺失，只有 formBindings、工序开始配置或旧字段可用。
- 发布投影快照缺少 flowGraph.nodes、batchUseConfigs.batchRecordReports、batchUseConfigs.formBindings、routeStartProductionLeaders 或 batchRecordAttachmentOwners 所需结构。
