# IntRuoyi Backend Development Rules

## 触发场景

- 修改 `IntRuoyiBackend` 下的 Java、Spring Boot、Maven、接口、服务、Mapper、DO、配置、脚本或后端测试前，必须先读取本文件。
- 涉及 SQL、schema、菜单、权限、租户绑定或数据修复时，还必须读取 `docs/database-rules.md`。
- 涉及本机服务启动、端口或运行态验证时，还必须读取 `docs/local-runtime.md`。

## 项目边界

- 后端根目录：`E:\IntRuoyi\IntRuoyiBackend`。
- 使用 Java 17、Spring Boot、Maven 多模块结构。
- 主应用模块：`yudao-server`。
- 业务逻辑必须保留在所属模块内；跨模块移动或耦合必须有明确的设计理由和验证。
- 不得根据前端页面或历史实现猜测后端 schema、权限、接口或租户行为。

## 实施规则

- 先确认变更所属模块、现有 Controller/Service/Mapper/DO 边界和已有测试。
- 对功能、修复、重构和行为变更，先记录 BDD，再执行 RED -> GREEN -> REGRESSION。
- 缺少数据库、Redis、依赖、测试数据或运行配置时，必须 fail fast；不得切换数据源、返回 mock 成功或吞掉错误。
- 接口和服务错误必须通过真实响应、日志或测试暴露；不得用默认成功值掩盖失败。

## 验证方式

- 优先运行受影响模块的定向 Maven 测试，例如：
  - `mvn -pl yudao-module-mes -am test`
  - `mvn -pl yudao-server -am test`
- 如果指定测试类，记录 `-Dtest` 范围和 `surefire.failIfNoSpecifiedTests` 处理依据。
- 涉及 API 行为时，验证成功路径和失败路径。
- 涉及前端调用时，最后通过真实前端路径或已批准的 E2E 核对接口结果。

## MES 一线设备账号权限门禁

### 权限角色授权必须走登录用户标准权限解析

- Trigger: 一线生产填写、设备账号切换工序、压力泵全工序授权、`post workstation binding loginUserId=... postIds=...`、`hasAnyPermissionsInRoles`、权限角色已配置但仍落到岗位/工作站绑定。
- Preflight check: 修改设备账号、岗位、工作站或特殊全工序授权前，先区分“系统标准权限”与“岗位/工作站绑定”两条链路；凡需求口径是“拥有权限角色/权限即可授权”，后端判定必须从 `loginUserId` 调用标准 `PermissionApi.hasAnyPermissions(userId, permission)`，不得先取角色 ID 再做显式角色权限判断。
- Blocker: 拥有目标权限的登录用户仍进入岗位/工作站绑定链路、超级管理员或动态授权语义被绕过、无权限普通用户被扩大到全工序、或错误信息只能看到岗位 ID 无法说明权限链路是否命中时，必须停止并补齐回归测试。
- Verification: 后端回归必须同时覆盖“有权限用户获得压力泵全部工序”“无权限用户仍按岗位/工作站绑定”“旧显式角色检查会复现岗位绑定缺失错误”，并复跑一线员工切换和工作站岗位绑定相邻测试。
- Forbidden action: 禁止硬编码账号 ID、岗位 ID、角色 ID；禁止把岗位/工作站绑定失败当作权限角色授权的 fallback；禁止用前端放行、空列表成功或默认路线掩盖权限链路未命中。
- Evidence: `doc/tasks/20260803-pressure-pump-role-process-switch/verification-report.md`，运行时错误 `设备账号上下文不完整或不一致：post workstation binding loginUserId=1, postIds=[14]`。

### 生产组长工序配置必须按正式负责路线限定

- Trigger: 生产组长工作台、工序配置、损耗原因、设备映射、设备参数标准、`process-config/list`、`routeStartProductionLeaders`、`mes:pro-process-pool-team-leader:maintain`、admin 工序配置看到其它工艺路线工序。
- Preflight check: 修改生产组长配置页候选工序、损耗/设备/参数维护授权前，先区分“维护入口权限”和“正式负责路线范围”：`mes:pro-process-pool-team-leader:maintain` 只能说明用户可进入维护入口，不能扩大路线工序范围；后端候选列表和直接维护断言必须只读取当前 active 路线版本 `routeStartProductionLeaders` 中命中的 `USER/USERS/ROLE` 配置。验证职责范围必须读取各路线 active `routeStartProductionLeaders`，并同时计算直接用户配置与当前账号角色命中的 `ROLE` 配置。若通过 SQL 修复 active 路线快照，写入前还必须核对目标路线是否存在 DRAFT/candidate version；已有草稿缺少同一配置时，后续发布会覆盖本次 active 修复，必须阻塞并改走正式草稿保存/发布或取得明确的数据修复范围授权。写入后必须重新只读解析当前 active version；路线发布可能把原目标 version 置为 `SUPERSEDED` 并生成新的 active version，最终验证不得继续把旧 draft/version ID 当作非目标失败条件。
- Blocker: `process-config/list` 返回未在正式负责路线内的路线工序、拥有维护权限的 admin 可直接维护非负责路线工序、无负责路线时返回全部 active 路线、或用维护权限/admin 身份替代 `routeStartProductionLeaders` 命中结果时必须停止并补后端 RED/GREEN。
- Verification: 后端回归必须覆盖“拥有维护权限也只能列出正式负责路线工序”“拥有维护权限但不在工序开始快照中直接维护 routeProcess 会被拒绝”“无维护权限仍走 USER/ROLE 快照授权”，并复跑工序配置相邻服务测试和前端新增入口静态合同；真实登录态验证工序配置时必须调用生产组长工序配置数据源 `/mes/pro/process-pool/team-leader/process-config/list`，并断言其路线名称集合等于 `/mes/pro/process-pool/team-leader/responsible-routes` 返回的正式负责路线集合。验证“账号实际配置了哪些路线的生产组长”时必须逐路读取 `/mes/pro/route/flow-config/route-start-production-leaders` 或当前 active JSON 快照，不能用维护入口列表代替。数据修复复验必须以 `tenant_id + route_id + active=1 + lifecycle_status=ACTIVE` 当前命中行为准，同时记录原写入 version 与当前 active version 的差异，并确认目标路线没有会在下一次发布时丢失配置的旧草稿。
- Forbidden action: 禁止用维护权限、前端新增弹窗默认候选、空列表成功、admin 硬编码、直接放宽所有账号、菜单文案或 API-only 说明替代正式后端授权；禁止把 admin 因入口权限能打开页面解释成其工序配置职责覆盖全部路线；禁止只改当前 active 而忽略已经存在且缺配置的待发布草稿；禁止把 `formBindings`、批记录表单或其它路线配置链路当作工序开始生产组长来源。
- Evidence: `doc/tasks/20260806-process-config-refresh-to-add-button/verification-report.md`，用户以 `芋道源码 / admin` 点击新增仍报“当前账号没有可新增的路线工序”；`doc/tasks/20260806-admin-pressure-pump-route-start-leader/verification-report.md`，路线发布后 route `922119` 的 active version 从原写入 `448` 变为 `490`，最终按当前 active `490` 与 `622` 复验通过；`doc/tasks/20260807-admin-pressure-pump-only-route-start-leader/verification-report.md`，admin 维护权限与正式职责范围分开验证，并对两条目标及四条非目标 active 路线逐路读取配置；`doc/tasks/20260807-team-leader-process-config-responsible-routes/verification-report.md`，admin 工序配置列表最终只返回两条正式负责路线下的 28 个工序。

### 一线运行态 route-start 生产组长来源必须独立于班组设备绑定

- Trigger: 一线生产 runtime-config、`frontline runtime deviceId=...`、`routeStartProductionLeaders`、工作站正式设备、班组设备映射、`MesFrontlineRouteProcessCandidate.contextSource`、班组长工作台缺少负责范围上下文。
- Preflight check: 修改一线运行态候选或员工/设备配置读取前，先区分候选来源：`ROUTE_START_PRODUCTION_LEADER` 候选的负责范围来自 active 路线 `routeStartProductionLeaders` 与当前负责组长；`POST_BINDING` 设备账号候选才按设备账号岗位/工作站绑定和班组设备映射解析。工作站正式设备 ID 只说明 route-start 候选来自该工位设备，不等于班组维护设备已经映射；正式提交授权只校验工序身份、人员和模板，请求仍携带设备/工作站上下文用于提交追踪，但不得把 submittedDeviceId/submittedWorkstationId 与 route-start 或 post-binding 候选的 expectedDeviceId/expectedWorkstationId 互相比对来阻断提交，提交阶段不执行设备参数校验。
- Blocker: route-start 生产组长候选带 `deviceId` 但缺班组设备映射时直接报“班组长工作台缺少负责范围上下文”、或为消除错误把设备账号候选也改成当前登录人/空成功时必须停止。
- Verification: 后端回归必须覆盖“route-start 候选带正式设备 ID 且无班组设备映射时仍返回当前负责组长人员上下文”“设备账号 post-binding 候选仍需按设备绑定解析”“生产员工继承唯一负责组长工序不回退设备账号来源”“一线生产正式提交授权在工序合法但提交设备/工作站与授权候选设备/工作站不一致时放行”和“正式提交服务不调用设备参数校验器”。
- Forbidden action: 禁止把工作站正式设备 ID 当作班组设备维护绑定；禁止用设备账号岗位/工作站绑定、当前登录人 fallback、空设备成功或前端隐藏错误替代正式 route-start 生产组长负责范围。
- Evidence: `doc/tasks/20260807-team-leader-workbench-frontline-device-context/verification-report.md`。

### 候选流程图正式工作站与展示工作站必须分字段

- Trigger: 工艺路线候选保存/读取/发布、`routeProcessWorkstationId`、流程图节点 `workstationId`、`mes_pro_route_process.workstation_id`、一线生产提示“工艺路线工序缺少正式工作站绑定”。
- Preflight check: 修改候选流程图、版本投影或流程配置解析前，必须确认节点字段职责：`routeProcessWorkstationId` 是路线工序正式绑定，`workstationId` 仅用于可用工作站展示；候选保存、候选读取、流程配置解析和发布投影必须逐段核对正式字段是否原样传递。
- Blocker: 正式字段缺失、展示字段被写入或读取为正式字段、发布后当前路线工序工作站为空、或正式工作站不存在/禁用/与工序不一致时必须停止；不得继续发布或让一线生产静默过滤该工序。
- Verification: 后端回归必须让两个字段取不同值，并分别覆盖候选保存、候选读取、流程配置解析和发布投影；发布后只读核验当前 ACTIVE 版本、全部路线工序非空绑定，以及工作站存在、启用且 `workstation.process_id == route_process.process_id`；最终通过 `/mes/pro/feedback/frontline/device-account/processes` 和真实“一线生产”点击验证。
- Forbidden action: 禁止用展示 `workstationId`、默认工作站、相邻工序工作站、`formBindings`、批记录表单、工序开始配置、前端隐藏错误或 API-only 成功补齐正式绑定。
- Evidence: `doc/tasks/20260807-frontline-route-process-workstation-binding-fix/verification-report.md`。

## eDHR 详情回填门禁

### 路线配置有值但详情接口为空

- Trigger: eDHR、批次详情、动态表单、损耗单、工艺路线绑定、填写人、`fillableUsers`、`routeBindingId`、配置页有值但详情接口为空。
- Preflight check: 先同时核对配置接口/表中的来源字段、执行任务快照字段、详情接口组装链路和既有优先级，不得只改前端显示文案。
- Blocker: 若详情任务没有可追溯的绑定 ID、快照字段或正式规则来源，必须阻塞并补齐后端数据链路；不得从当前登录人、创建人、更新人或角色 ID 推断填写人。
- Verification: 新增后端回归测试覆盖“仅路线绑定配置填写人”场景，并同时跑相邻优先级测试，确认有效工作任务和工序规则仍优先。
- Forbidden action: 禁止前端把 `未配置` 改成配置页名称、禁止把角色/部门 ID 当用户 ID、禁止用空列表兜底掩盖缺失来源。
- Evidence: 任务 `doc/tasks/20260724-edhr-route-form-filler-backfill/`，目标测试 `MesProEdhrBatchExecutionServiceTest#detailTask_includesFillableUsersFromRouteFormBindingWhenWorkTaskNotCreated`。

### 批记录表单角色填写人名称回显边界

- Trigger: eDHR、批记录表单、填写人配置、小弹窗默认填写人、`candidateSourceType=ROLE`、`candidateSourceNames` 为空、候选用户已展开但角色名不显示。
- Preflight check: 修改填写规则响应前，同时核对 form-level `FILL` 规则和 cell-level 填写分配响应；角色来源必须既展开启用候选用户，也展开角色来源名称，不得只验证 `candidateUsers`。
- Blocker: `get-by-report` 对 form-level ROLE 只返回成员用户、不返回角色名，或 API 只能靠前端从用户列表反推角色名时必须停止并补齐后端响应。
- Verification: 后端回归覆盖 form-level ROLE 的 `candidateSourceNames` 与 `candidateUsers`，并用本机登录态 API 核对目标批记录表单返回业务码 `0`、角色名和候选用户数。
- Forbidden action: 禁止用前端硬编码角色名、当前登录人、创建人、角色 ID 文案拼接或空列表兜底掩盖后端响应字段缺失。
- Evidence: 任务 `doc/tasks/20260728-pressure-pump-batch-record-role-fillers/verification-report.md`，目标测试 `MesProEdhrProcessFormPermissionRuleServiceImplTest#getRuleByReport_returnsRoleSourceNamesForFormLevelFillRule`。

### 切换填写人快照读取边界

- Trigger: eDHR 批次执行填写页、“切换填写人”、协助填写人、`assistSwitchTasks`、`candidateUserSnapshot`、`getEdhrBatchExecution`、同工序 `MAIN` + 非 `MAIN` 附加表单候选、动态路线表单候选、`formBindingKey`、`formCenterInstanceId`、`activeWorkTaskId`、`workTaskId`、`assistUserId`、损耗单表单槽位、弹窗打开耗时过长。
- Preflight check: 先确认业务口径是否为批次执行创建后填写人固定；若固定，切换填写人候选必须来自执行详情返回的任务/填写人快照，而不是弹窗打开时重新拉取或重算全量批次详情；同工序存在 `MAIN` 批记录表单和非 `MAIN` 附加表单/表单槽位时，`assistSwitchTasks` 必须覆盖所有有效候选，候选来源按 active workTask `candidateUserSnapshot`、过程表单规则、工序规则、路线绑定候选源逐级核对；`available/allowedActions/activeWorkTaskId` 只能来自真实 active workTask，缺少同工序附加表单 workTask 时必须由后端正式 companion workTask 生成或详情回填链路补齐。eDHR 批次任务打开传统批记录时，必须把当前批次任务 ID 写入 `MesProBatchRecordExecutionOpenOrCreateByContextReqVO.taskId`，active 查询、active key 和 `mes_pro_batch_record_execution.task_id` 必须按 `batchExecutionId + taskId + workOrderId + routeProcessId + batchRecordReportId + batchCode` 隔离，并继续通过 `mes_pro_edhr_batch_execution_task.execution_id` 维护批次任务与 execution 的关联，避免新批次复用旧执行详情；若候选是动态路线表单任务（无 `batchRecordReportId` 且有 `formBindingKey/formTemplateId/formTemplateVersionId/formCenterInstanceId`），辅助填写行必须从任务冻结的 `FormTemplateVersionDO.jimuSchemaJson` / `edhrAssistRows` 解析，并按所选填写人的 `responsibilityScopeJson` 过滤，不得读取传统批记录 execution snapshot，且 `openTask` 响应必须携带前端运行态渲染所需的模板快照字段（如 `formTemplateJimuSchemaJson`、`formTemplateRecognizedFields`、模板元数据和实例草稿），不能要求填写人再具备模板管理查询权限。`openTask` 对代开目标附加表单的授权必须同时满足：当前用户在同批次同 `routeProcessId` 有 active FILL/REWORK 锚点任务，且请求的 `assistUserId` 是目标 workTask 的正式分配人或候选人。
- Blocker: 执行详情缺少可追溯任务快照、活动工作任务缺少 `candidateUserSnapshot`、附加表单只有候选展示但无真实 `activeWorkTaskId/workTaskId`、或无法证明候选人来自创建时快照时，必须补齐后端详情和工作任务链路；若 active 执行记录查询没有按批次和传统批记录上下文隔离，也必须阻塞；若动态路线表单任务缺完整 FormCenter 上下文、模板版本不存在、模板 ID 不匹配、`openTask` 成功后仍必须调用 `/form-center/templates/{id}/versions/{versionNo}` 才能渲染，或辅助行解析仍触发 `eDHR 批次缺少唯一批记录路线`，必须阻塞；若 `openTask` 允许无同工序锚点用户、错误 `assistUserId`、当前登录人 fallback 或外部用户打开目标附加表单，也必须阻塞。
- Verification: 运行 `node yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs`，并配合前端 ESLint/`pnpm ts:check` 与 `mvn -pl yudao-module-mes -am "-DskipTests" compile`；涉及动态路线表单候选时，新增或复跑后端回归，断言打开响应保留 `formCenterInstanceId`、所选 `assistUserId`、过滤后的 `assistRows` 和 FormCenter 模板渲染快照，且不调用传统批记录 `openOrCreateByContext`；涉及同工序附加表单候选时，后端回归必须同时覆盖候选快照、companion workTask 创建/回填、`openTask` 同工序锚点授权成功和无锚点拒绝。
- Forbidden action: 禁止在切换填写人弹窗打开时调用全量 `getEdhrBatchExecution` 作为性能问题的替代方案；禁止在 eDHR 批次任务打开传统批记录时省略、置空或改写 `MesProBatchRecordExecutionOpenOrCreateByContextReqVO.taskId` / `mes_pro_batch_record_execution.task_id`，导致不同批次任务复用旧执行详情；禁止让动态路线表单候选回落到批记录 execution snapshot、批记录路线唯一性解析、前端缓存、空列表兜底或吞异常；禁止只展示候选但不生成真实 workTask，或用当前登录人/任务负责人兜底替代“所有有效候选人”。
- Evidence: 任务 `doc/tasks/20260727-switch-filler-snapshot-loading/verification-report.md`；任务 `doc/tasks/20260728-loss-form-switch-route-fix/verification-report.md`；任务 `doc/tasks/20260728-switch-filler-extra-form-candidates/verification-report.md`。


## eDHR 批次任务配置来源门禁

### 当前配置与发布快照边界

- Trigger: eDHR 批次执行、路线发布快照、`routeSnapshotJson`、`batchUseConfigs`、记录本/批记录融合、当前路线配置缺失或陈旧绑定、任务门禁 `available`、开始节点并行第一组、多前置汇合工序。
- Preflight check: 新建/返工批次前先同时检查当前 BATCH 工序配置是否存在、绑定是否归属启用工序配置、发布版本快照是否包含完整 `flowGraph.nodes` 与 `batchUseConfigs`。读取已有批次任务门禁时，还要核对任务 `routeProcessId` 是否被冻结快照 `flowGraph.nodes` 完整覆盖；若批次任务由当前 BATCH 工序配置生成且当前配置完整覆盖任务工序，任务门禁必须按当前路线关系图读取完整直接前置集合。
- Blocker: 只要当前 BATCH 工序配置存在，就必须使用当前配置并严格校验绑定归属；不得因为当前绑定陈旧而静默回退到发布快照。批次任务 `routeProcessId` 既不能被冻结快照完整覆盖，也不能被当前 BATCH 配置完整覆盖，或当前/冻结关系图存在孤立、成环、不可达节点时必须停止；不得用单值 `predecessorRouteProcessId`、排序前一工序、默认首个 WAITING 工序或空前置集合掩盖多前置关系。
- Verification: 同时覆盖“当前配置存在优先当前绑定”“当前配置整体缺失时使用已发布快照”“陈旧绑定必须 fail fast”“legacy flat batchRecordReportId 快照可投影”“多起点第一组均 available=true”“多前置汇合工序前置未完成时 available=false”“旧冻结快照但当前配置覆盖任务工序时按当前关系图计算”的后端测试。
- Forbidden action: 禁止把发布快照作为通用 fallback；禁止用空绑定、默认 MAIN 或默认成功掩盖当前配置损坏。
- Evidence: `doc/tasks/merge-jiluben-worktree-20260724/verification-report.md`；`doc/tasks/20260729-edhr-parallel-start-process-highlight/verification-report.md`。

### 历史批记录只读页与活动流转门禁边界

- Trigger: 历史批记录页签、`review-timeline`、已归档批次、`BATCH_STATUS_ARCHIVED`、`routeSnapshotJson` 缺 `flowGraph.nodes` 或 `batchUseConfigs`、当前路线 BATCH 配置已删除、只读批记录预览反查当前 Jimu 报表。
- Preflight check: 先区分“终态只读历史展示”和“活动批次流转/切换工序门禁”；历史页签应读取已持久化的批次事件、任务事件、执行快照、签名、审批、附件和归档目录，不应为了展示历史而调用活动任务门禁 `buildTaskGateMap` 或重新解析当前 BATCH 流程配置。
- Blocker: 已归档历史批次因当前/冻结 BATCH 门禁配置缺失导致全部历史信息打不开，或历史执行预览因当前 Jimu 报表/当前报表绑定缺失阻断已保存执行快照展示时，必须修复历史读取边界；不得用清空全部历史、前端隐藏错误或吞异常掩盖。
- Verification: 后端回归必须同时覆盖“缺失 BATCH 门禁配置仍返回已持久化执行快照并标记只读”和“正常历史批记录仍返回任务、签名、审批、归档内容”；静态契约需防止 `review-timeline` 重新直接调用活动门禁。
- Forbidden action: 禁止把终态历史只读页签改成当前配置重算结果；禁止历史执行预览在已有 `executionSnapshotJson` / `sheetLayoutJson` 时再强制依赖当前 Jimu 报表；禁止把活动批次缺配置 fail-fast 放宽到默认成功。
- Evidence: `doc/tasks/20260803-edhr-history-missing-batch-config/verification-report.md`。

### 草稿 BATCH 快照读写对称边界

- Trigger: 路线草稿/候选版本、`routeSnapshotJson`、`batchUseConfigs`、`formBindings`、表单槽位、`flow-config/batch-record/save`、草稿保存后读回为空或仍报“系统异常”。
- Preflight check: 同时核对保存链路写入的候选快照字段、读取策略、版本生命周期状态和当前工序设置；一旦 DRAFT 草稿显式保存过 BATCH 绑定快照，DRAFT 读取必须优先返回该草稿快照，待审批/待发布版本仍按既有规则读取当前工序设置。
- Blocker: 显式保存后的 DRAFT `batchUseConfigs.formBindings` 读回被当前工序设置覆盖、读回为空、或无法区分 legacy 候选快照与本次草稿显式保存快照时，不得宣称草稿保存完成。
- Verification: 新增后端回归测试覆盖“显式保存后的 DRAFT 快照优先于当前绑定”，并同时跑完整相邻测试类，确认 PENDING_APPROVAL / READY_TO_PUBLISH 仍读取当前工序设置。
- Forbidden action: 禁止用当前工序设置作为显式保存草稿快照的 fallback；禁止用空绑定、默认 MAIN、前端隐藏或吞异常掩盖草稿快照读写不对称。
- Evidence: `doc/tasks/20260726-route-flow-v15-save-system-exception/verification-report.md`，`MesProRouteFlowConfigServiceImplTest#getRouteFlowProcessConfigList_shouldReadSavedDraftBatchSnapshotBeforeCurrentBindings`。

### 历史关闭候选版本只读快照边界

- Trigger: 工艺路线版本工作区“查看”、老版本工艺流程、`routeVersionId`、`CANCELLED`、`REJECTED`、`SUPERSEDED`、`PRO_ROUTE_VERSION_CANDIDATE_NOT_PUBLISHABLE`、历史关系图或流程配置打不开。
- Preflight check: 先区分读取与写入状态集合；只读读取应按版本生命周期从该版本 `routeSnapshotJson.configSnapshots` 读取关系图、批记录/排产/附件负责人等冻结快照，写入仍只允许 `DRAFT` 候选版本。
- Blocker: 只读查看 `CANCELLED` / `REJECTED` / `SUPERSEDED` 被候选发布条件拦截、返回当前 ACTIVE 配置、返回空图/空配置、或写入校验因扩展读取状态而放宽时必须停止。
- Verification: 后端回归必须同时覆盖关闭历史版本读取冻结快照、候选排产快照读取、以及 `CANCELLED` 保存关系图/流程配置/排产配置仍 fail-fast；前端静态或真实路径需证明查看动作传递历史 `routeVersionId` 且禁用写控件。
- Forbidden action: 禁止把关闭候选版本当成待发布候选要求、禁止回退到当前工序设置或 ACTIVE 版本、禁止用空快照默认成功、禁止为了只读查看放宽提交/保存/发布写入守卫。
- Evidence: `doc/tasks/20260727-route-history-cancelled-version-view/verification-report.md`。

### 冻结快照附件负责人 JSON 类型边界

- Trigger: `batchRecordAttachmentOwners`、`PRO_ROUTE_FLOW_CONFIG_BATCH_ATTACHMENT_OWNER_INVALID`、`批记录附件负责人配置无效`、已有批次冻结 `route_snapshot_json` 缺配置、路线版本发布后旧批次仍打不开。
- Preflight check: 先分别核对当前 ACTIVE 路线版本快照和目标批次冻结快照的 `$.configSnapshots.batchRecordAttachmentOwners`，同时检查 `JSON_TYPE` 必须是 `ARRAY`、`JSON_LENGTH` 必须等于业务要求数量；只看到配置接口返回列表不代表冻结快照可用。
- Blocker: ACTIVE 版本缺配置、批次冻结快照缺配置、JSON 被写成 `STRING` 而不是 `ARRAY`、影响行数不是精确目标行数、或缺少原始快照备份时必须停止，不得放宽打开已有批次的校验。
- Verification: 授权数据修复必须记录原始快照备份、回滚路径、`restoreRows/repairRows`、修复后 `JSON_TYPE=ARRAY` 与 `JSON_LENGTH`，再用真实页面 `打开/创建 -> 确认` 验证不再出现负责人配置错误。
- Forbidden action: 禁止把缺失负责人配置默认成功、禁止把 JSON 数组通过用户变量/字符串写成 JSON 字符串、禁止 API-only 或直接详情 URL 替代确认按钮 E2E。
- Evidence: `doc/tasks/20260727-batch-record-attachment-owner-config/verification-report.md`。

### 批次任务产品信息成员表单部分缺失边界

- Trigger: eDHR 批次执行、批记录表单“产品信息”缺失、已有批次存在 `ROUTE_FORM` 任务但同版产品信息成员表单未生成、`batchRecordDefinitionId + batchRecordVersionId`、`batch_record_sort` 唯一键冲突、产品信息未固定排在 `80`。
- Preflight check: 读取批次详情或修复任务恢复逻辑时，必须比较目标批次任务集合与已有任务集合；不能只用“已有任一 `ROUTE_FORM` 任务”判断批记录任务完整。产品信息成员表单只能从已有正式 `MAIN + BATCH_RECORD` 任务的批记录定义/版本解析，生成和恢复时 `batchRecordSort/reportSort` 必须统一为 `80`，不得从 `formBindings`、工序开始配置、当前登录人或默认槽位推断。
- Blocker: 同版产品信息成员报表存在但活跃批次详情仍不展示、恢复逻辑会重复插入同一 `batchRecordReportId`、产品信息不是 `batch_record_sort=80`、产品信息排在正式批记录表单之前，或产品信息与源表单使用相同 `batch_record_sort` 触发唯一键冲突时，必须停止并补齐后端任务恢复链路。
- Verification: 后端回归必须同时覆盖“新建批次包含产品信息成员表单”“已有工序任务但缺产品信息时详情读取补齐”“完全缺工序任务的历史恢复仍可用”，并断言产品信息固定排序 `80` 且在前序正式批记录未完成前被同工序顺序门禁阻塞。
- Forbidden action: 禁止用前端硬编码展示“产品信息”、禁止把 `formBindings` 当批记录表单来源、禁止按源表单排序 `-1` 推算产品信息位置、禁止只调整页面排序或隐藏错误来掩盖任务未持久化。
- Evidence: `doc/tasks/20260728-batch-execution-product-info-form-missing/verification-report.md`。
## eDHR 批记录版本治理规则运行态门禁

### 已发布版本治理证据与 Jimu 当前 JSON 边界

- Trigger: eDHR 打开填写、`openOrCreateByContext`、`1040750243`、批记录模板未确认填写规则、`CELL_RULE_RECONCILED`、已发布批记录版本、Jimu 报表 JSON。
- Preflight check: 先核对报表 `batchRecordVersionId`、版本 `APPROVED` 状态、migration item 中 `CELL_RULE_RECONCILED` 证据、blocking item 数量，以及当前 Jimu JSON 未确认单元格数量。
- Blocker: 版本未发布、缺少 `CELL_RULE_RECONCILED` 治理证据、存在 `BLOCKER` 或未确认 `CONFIRM_REQUIRED` 时，运行态必须继续 fail-fast，不得把当前 Jimu JSON 自动标记为已确认。
- Verification: 后端测试同时覆盖“已发布且治理通过时物化运行态规则”和“无治理证据的 legacy checkbox 仍阻塞”；真实 E2E 需打开当前填写任务并核验 execution snapshot 无未确认规则字段。
- Forbidden action: 禁止直接 SQL 修改 `jimu_report.json_str`、禁止跳过 `validateConfirmedCellRules`、禁止把 API-only 或历史 execution 直连当作打开填写成功。
- Evidence: `doc/tasks/20260724-batch-execution-published-route-runtime-update/verification-report.md`。

### 批记录单元格链接预填落库边界

- Trigger: eDHR 批记录单元格链接、`PRODUCTION_WORK_ORDER.batchCode`、生产批号目标格为空、`/batch-record-cell-link/prefill`、`cell_values_json=[]`、只读预览缺少已配置链接值。
- Preflight check: 先区分“来源字段不存在”和“链接值未落库”：同时核对来源业务表字段值、启用链接规则、目标 execution 的 `cell_values_json`、创建/打开执行记录写边界和字段审计链，不得只看前端 draft hydrate。
- Batch code source boundary: `PRODUCTION_WORK_ORDER.batchCode` 在批记录执行运行态必须读取创建/打开执行记录时已解析并写入 `mes_pro_batch_record_execution.batch_code` 的正式执行上下文批号；生产工单主表 `batch_code` 只可作为创建执行记录时的输入来源之一，不得在单元格链接落库阶段绕过执行上下文直接作为唯一来源。
- Source ownership boundary: `PROCESS_POOL_REPORT` 等来自生产组长报工确认、订单工序完成或其它专用业务写链路的来源字段，不应由通用 `/batch-record-cell-link/prefill` 自动预填接管；通用预填应跳过该来源，由对应专用服务负责读取正式业务事件、分配记录、字段映射和字段审计写入。
- Blocker: 来源值存在且链接规则启用，但目标 execution 未保存到 `cell_values_json` 时，必须把修复收敛到创建/打开执行记录的后端落库链路；若字段审计系统写入证据缺失，也必须阻塞，不能直接 update 主表。
- Idempotency schema check: 自动落库写入字段审计前必须核对幂等键列长度；语义组合键可能超过 `varchar(64)` 时，使用稳定原始组合键的 SHA-256 作为保存和查询共用键，并同时测试写入路径与重复打开查询路径恰好生成 64 位小写十六进制。
- Verification: 后端回归需覆盖创建执行记录、打开历史空 DRAFT、重复打开幂等、目标已有人工值不覆盖、来源批号缺失 fail-fast、专用来源被通用预填跳过且由专用服务回填，并复验字段审计 hash/head revision、审计批次数量和幂等键长度；真实 E2E 需同时断言打开任务响应、执行详情 `cellValuesJson`、页面目标输入值和重复打开不追加审计批次。
- Forbidden action: 禁止把 `/prefill` 返回值或前端 `hydrateDraftState` 当作已保存结果；禁止前端写空值兜底、查询接口隐式写库、直接 SQL 回填、把专用业务来源当成通用不支持字段抛错，或绕过字段审计链。
- Evidence: `doc/tasks/20260727-edhr-cell-link-auto-persist-design/verification-report.md`；`doc/tasks/20260727-edhr-cell-link-auto-persist-implementation/verification-report.md`；`doc/tasks/20260731-team-leader-workbench-prd-plan/execution-log.md`。

## eDHR 批记录 Word 表格解析门禁

### 全局行形态优先于模板特例

- Trigger: 批记录 Word 导入、Route B/Route D 表格识别、packed 物料矩阵、操作明细区域、`生产自检`/合格标准/检验方法说明块、截图位置错位。
- Preflight check: 先用真实源 DOC 与最小合成表格复现结构偏差，定位到共享 parser/calibrator/row-type 规则；对 packed 宽单元格必须按视觉 token 处理续行，对短标题 + 长说明行必须按说明区行形态判断。
- Blocker: 缺少真实源 DOC、测试类硬编码本地 fixture 不存在、或 RED 不能稳定复现时，不得宣称修复完成；先记录缺失 fixture 和影响范围。
- Verification: 回归必须同时包含合成 RED/GREEN 和用户指定真实 DOC 样本；至少断言 packed 括号续行不新增物料项、后续物料不整体错位、操作明细区域不吞入后续说明块。
- Forbidden action: 禁止用表单名、工序名、文件名、压力泵模板名硬编码特例；禁止把缺 fixture 的结构测试当成业务逻辑失败；禁止只靠截图人工判断完成。
- Evidence: `doc/tasks/20260725-batch-record-global-table-position-fix/verification-report.md`。

### 批记录/路线导入真实 fixture 覆盖范围变更边界

- Trigger: 批记录 Word、Sheet1 Excel、路线导入、真实 fixture、`NoSuchFileException`、用户明确说“不需要覆盖这个”或取消真实样本覆盖。
- Preflight check: 先区分“业务仍要求真实样本覆盖但 fixture 缺失”和“用户明确变更验收范围取消该真实样本覆盖”；前者必须阻塞并取得权威原件，后者必须删除依赖缺失真实 fixture 的测试入口，同时保留不依赖真实文件的合成 fail-fast/契约测试。
- Blocker: 缺少用户明确范围变更、无法证明删除的测试只覆盖被取消的真实样本链路、或删除后完整目标套件仍有 failure/error 时，必须停止，不得宣称完成。
- Verification: 记录用户范围变更、删除/保留的测试清单，运行目标 parser/contract 定向测试和完整模块回归；完整回归必须 `BUILD SUCCESS` 且 0 failures/0 errors。
- Forbidden action: 禁止用 `@Disabled`、Maven excludes、assumptions、空夹具、合成 workbook 或桌面候选文件冒充权威真实 fixture；禁止把真实样本覆盖取消解释成业务 fallback。
- Evidence: `doc/tasks/20260727-edhr-notify-all-valid-candidates/verification-report.md`，用户明确取消 Sheet1 Excel 真实样本覆盖后，保留 `Sheet1RouteExcelParserTest` 合成 fail-fast 测试并通过完整 `mvn -pl yudao-module-mes test`。

### 旧版本 JSON 的 fillForm/edhrCellRule 读时刷新门禁

- Trigger: 批记录截图或只读预览仍显示已修复过的错位 checkbox、V14/V14.0 等既有版本复验、`sheetLayoutJson` 的 `text` 坐标正确但页面仍渲染旧控件。
- Preflight check: 同时审计 `text/value`、`fillForm.labelText/componentFlag/valueType` 和 `edhrCellRule.label/componentFlag/valueType`；不得只检查静态文本坐标。
- Blocker: 若业务列仍残留未确认 AUTO 规则的旧 checkbox / BOOLEAN / 串列 label，必须在共享单元格规则刷新链路中修复并持久化，不得用截图裁剪、前端隐藏或表单名特例绕过。
- Verification: 回归测试必须覆盖 stale `fillForm` 被刷新、已确认 MANUAL 规则不被覆盖、密集表格业务列优先使用上方列头；真实页面验证需同时断言目标业务列 offender 为 0 并保留截图。
- Forbidden action: 禁止只重新导入新版本就宣称既有版本已修复；禁止按产品名、工序名、文件名、压力表文本写清理逻辑；禁止把 API-only 审计替代真实前端截图验收。
- Evidence: `doc/tasks/20260726-batch-record-v14-layout-regression/verification-report.md`。

### Jimu fillForm 组件类型语义优先边界

- Trigger: Jimu 编辑页右侧“当前组件”与批记录单元格语义不一致、日期/签名日期单元格显示为“多行文本”或普通文本、`fillForm.componentFlag=input-textarea` / `input-text`、`记录人/日期` / `操作人/日期` / `复核人/日期` 等签名日期宽空白格。
- Preflight check: 先审计后端 `MesProBatchRecordReportJsonBuilder` 生成的 `fillForm.componentFlag`、`edhrSignature` 与相邻/同一行标签语义；Jimu 右侧当前组件以 `fillForm.componentFlag` 为准，只有 `edhrSignature` 元数据不足以显示电子签名控件；宽合并空白格不得在语义判断前被 `isWideBlankNarrativeArea` 直接归类为 textarea。
- Blocker: 如果无法用最小合成表格稳定复现组件类型误判，或无法证明普通叙述型宽空白格仍保持 textarea，不得宣称修复完成。
- Verification: 必须同时覆盖“签名日期宽空白格生成 `componentFlag=signature` 并保留 `edhrSignature`”和“普通高/合并叙述空白格仍生成 `input-textarea`”两个回归断言。
- Forbidden action: 禁止只改前端“当前组件”显示文案、禁止直接手工改 Jimu JSON、禁止按模板/产品/文件名硬编码日期格、禁止只把签名日期格退化成 `input-text` 或普通日期展示而丢失电子签名组件语义。
- Evidence: `doc/tasks/20260727-jimu-signature-date-cell-type/verification-report.md`。

## 统一审批中心 BPM 已办历史状态门禁

- Trigger: 审批中心“已办”、`/approval-center/tasks/page?viewType=DONE`、`BpmNativeApprovalTaskProvider`、`BPM_TASK_DONE`、历史 `HistoricTaskInstance` 缺少 `TASK_STATUS`、页面显示“系统异常”。
- Preflight check: 先对照标准 BPM `done-page` 行为确认历史任务状态字段是否可为空；统一审批中心 DONE 映射必须保留正式历史任务行，`approvalResult` 可为空表示历史记录未保存审批结果，不得把缺失状态当作整页异常。
- Blocker: DONE 映射因 `TASK_STATUS=null` 抛 `APPROVAL_RESULT_UNSUPPORTED`、删除历史任务行、返回默认“通过/驳回”、或对非空未知状态吞异常时必须停止。
- Verification: 新增后端回归覆盖缺少 `TASK_STATUS` 的 `HistoricTaskInstance` 仍返回 `BPM_TASK_DONE` 摘要且 `approvalResult/approvalRemark` 为空；同时复跑 `BpmNativeApprovalTaskProviderTest` 和 `ApprovalCenterServiceImplTest`。
- Forbidden action: 禁止用前端隐藏错误、空列表成功、过滤掉 legacy 已办任务、默认审批结果、或放宽所有未知状态来掩盖历史状态缺失。
- Evidence: `doc/tasks/20260804-approval-center-done-system-exception/verification-report.md`。

## 统一审批中心 DCC 已办历史快照展示门禁

- Trigger: 审批中心“已办”、`viewType=DONE`、`DccApprovalTaskAdapter`、DCC 历史审批行、受控文件历史归档或软删除数据缺少 `versionNo`、`categoryId`、分类记录或其它只用于展示的历史元数据，页面显示“系统异常”。
- Preflight check: 先区分当前 TODO/处理态必填业务数据和 DONE 历史展示快照。当前待办或可处理审批必须继续要求正式版本号、分类和业务元数据；历史 DONE 行如果正式业务已完成但展示元数据缺失，只能在上下文标签中用 `-` 表示未知展示值，并保留文件编号、节点、盖章、分发等仍可追溯字段。
- Blocker: DCC DONE 历史行因缺少纯展示字段抛 `APPROVAL_BUSINESS_VERSION_REQUIRED` / `APPROVAL_BUSINESS_CATEGORY_REQUIRED` 导致整页失败，或为了修复 DONE 而放宽 TODO/当前审批必填校验时必须停止。
- Verification: 后端回归覆盖历史 `DccControlledFileDO` 缺 `versionNo/categoryId` 时 DONE 摘要返回 `版本：-` 和 `分类：-`；同时复跑 `DccApprovalTaskAdapterTest` 与 `DccApprovalTaskTimelineAdapterTest`，再用真实 `/approval-center/done` E2E 验证 DONE API 成功且页面无“系统异常”。
- Forbidden action: 禁止用 `formBindings`、默认分类、当前登录人、前端隐藏错误、空列表成功或 catch 吞异常替代正式 DCC 历史快照展示；禁止把历史展示占位扩散到当前待办必填字段。
- Evidence: `doc/tasks/20260804-approval-center-done-system-exception/verification-report.md`。

## 统一审批中心待办聚合一致性门禁

- Trigger: 审批中心“待办”、`/approval-center/tasks/page?viewType=TODO`、左侧徽标有数量但列表为空、`ApprovalCenterServiceImpl`、provider `total > 0` 但首屏 `list` 为空、页面显示“暂无审批任务”或“0 个模块”。
- Preflight check: 同时核对模块徽标统计、统一审批中心 provider 分页响应、全局聚合窗口和前端 route query；首屏 `pageNo=1` 时 provider 返回 `total > 0` 但 `list` 为空必须视为 adapter/query 一致性错误，而不是合法空态。
- Blocker: 后端把 total/list 不一致返回给前端、前端隐藏查询条件导致用户看不到过滤状态、模块列表接口失败被后续请求覆盖为有效 0、或测试只断言空态文案不核对 provider 总数时必须停止。
- Verification: 后端回归覆盖 inconsistent provider 首屏 fail-fast，并复跑 `ApprovalCenterServiceImplTest`；前端静态合同覆盖 route `moduleCode` / `keyword` 同步到可见筛选控件，并复跑审批中心分页/列表相邻合同。
- Forbidden action: 禁止用前端空列表兜底、默认清空筛选、吞模块接口异常、过滤掉 provider 行、或只改徽标数量来掩盖正式待办数据链路不一致。
- Evidence: `doc/tasks/20260804-approval-center-todo-empty-list/verification-report.md`。

## 业务审批策略按配置执行门禁

### 表单模板升版/作废审批模式以 published 策略为准

- Trigger: 表单模板导入升版、作废审批、`FORM_TEMPLATE_UPGRADE`、`FORM_TEMPLATE_OBSOLETE`、`form-template-upgrade-v1`、`Form template upgrade requires BPM approval`、业务审批策略切换 DIRECT/SIGNATURE_REQUIRED。
- Preflight check: 先核对 `bpm_business_approval_policy` 中目标 executor 的 published 策略模式；`DIRECT` 必须直接执行 executor 的直接生效逻辑，`BPM_REQUIRED` 必须有对应流程 key（升版 `form-template-upgrade-v1`、作废 `form-template-obsolete-v1`）并启动 BPM。
- Blocker: DIRECT 仍启动 BPM、BPM_REQUIRED 未启动 BPM、BPM_REQUIRED 流程 key 为空或错误、seed 强行改写已发布 DIRECT 策略、或回归只能靠手工改库时必须阻塞。
- Verification: 运行 `mvn -pl yudao-module-bpm "-Dtest=BusinessApprovalPolicyAdministrationServiceTest,FormTemplateUpgradeBusinessApprovalEffectExecutorTest,FormTemplateObsoleteBusinessApprovalEffectExecutorTest" test`、`python -X utf8 -m pytest script/tests/test_form_template_upgrade_bpm_seed.py script/tests/test_form_template_obsolete_bpm_policy_seed.py`，并复验 BPM_REQUIRED orchestrator 相邻测试。
- Forbidden action: 禁止把 DIRECT 当成降级或绕过强行拦截；禁止把 BPM_REQUIRED 静默直通、默认成功、前端隐藏错误、手工 update 单条数据或 seed 覆盖用户显式策略。
- Evidence: `doc/tasks/20260727-form-template-approval-mode-respects-policy/verification-report.md`。

### 业务审批策略默认视图必须用顶层开关白名单

- Trigger: 业务审批策略列表、`approvalSwitchScope`、默认显示 102 条、可开关审批业务、文控/表单/批记录审批开关、`policyMode=BPM_REQUIRED` 过滤过窄。
- Preflight check: 默认“可开关审批业务”视图必须按顶层业务 effect executor 正向白名单过滤，例如 DCC 上传/发布/作废、表单模板升版/作废、工艺路线版本发布、批记录版本发布、eDHR 批次提交审核和批次作废；页面的 `policyMode`、对象类型等筛选只能在该范围内继续过滤。
- Blocker: 默认视图用 `policyMode=BPM_REQUIRED` 导致关闭审批的 `DIRECT` 策略不可见，或只排除 `EDHR_ROUTE_FORM` 等少量明细导致表单实例、路线附件、路线表单填写等明细策略仍大量出现时必须停止。
- Verification: 后端 Mapper 回归必须同时插入顶层策略和同对象类型明细策略，断言 `approvalSwitchScope=true` 只返回白名单执行器且保留 `DIRECT`；前端静态契约必须断言默认传 `approvalSwitchScope: true` 且 `policyMode` 不默认等于 `BPM_REQUIRED`。
- Forbidden action: 禁止用对象类型泛匹配或“排除几个噪声类型”替代顶层执行器白名单；禁止把关闭审批的 DIRECT 策略隐藏；禁止把业务策略列表误当 BPM 流程定义列表。
- Evidence: `doc/tasks/20260804-bpm-policy-default-bpm-required/verification-report.md`。

## eDHR 放行负责人来源门禁

### 工序结束放行负责人必须来自 RELEASE_APPROVE

- Trigger: eDHR 放行负责人、放行预检、放行审批、电子签名放行、`releaseOwnerLabel`、`RELEASE_APPROVE`、`CLOSE`、工艺路线“工序结束 > 放行责任人”。
- Preflight check: 同时核对路线级 `RELEASE_APPROVE` 规则、候选人解析结果、工作台 `releaseSummary` 和正式放行授权；展示与授权必须共用 `RELEASE_APPROVE`，不能只看 `stageOwnerRole` 或关闭负责人。
- Blocker: 只配置 `CLOSE` 未配置 `RELEASE_APPROVE`、`RELEASE_APPROVE` 候选池为空、用户/角色无效或运行态仍显示“执行人”时必须停止，不得把关闭负责人、当前登录人、静态阶段角色或 `stageOwnerRole` 当作放行负责人。
- Verification: 后端回归覆盖 USER、ROLE_GROUP、角色成员可放行、关闭负责人不能越权和缺失配置 fail-fast；前端静态契约覆盖放行预检/审批阶段读取 `releaseSummary.releaseOwnerLabel` 且不兜底 `stageOwnerRole`。
- Forbidden action: 禁止新增数据库迁移修历史数据、禁止把 `CLOSE` 规则复用为放行授权、禁止前端用“执行人/QA/放行员”掩盖未配置、禁止吞掉候选人解析异常。
- Evidence: `doc/tasks/20260727-edhr-release-owner-from-end-config/verification-report.md`。

### 活跃订单申请放行资料必须只使用正式来源

- Trigger: 生产组长活跃订单“申请放行”、`active-order/release/apply`、`MesTeamLeaderActiveOrderReleaseApplicationService`、正式批记录数据、正式过程检验单、正式损耗单、生产负责人放行待办。
- Preflight check: 后端必须作为权威门禁核对当前用户生产组长负责范围、活跃订单生产进度和检验进度均为 100%、发布态路线快照、逐工序正式 BATCH 批记录绑定、过程检验汇集确认明细、损耗单正式承载映射、`RELEASE_APPROVE` 放行负责人和申请幂等键；批记录来源只能来自工序设置逐工序 BATCH 绑定和 `RECORD_CATEGORY_BATCH_RECORD`，过程检验来源只能来自已确认的 PQC 汇集明细，放行只能提交到负责人待审批。对要求同一目标工序生成批记录、过程检验和损耗三类资料的 fixture，执行写入前必须按租户、路线版本和路线工序证明三类传统 `batchRecordReportId` 均非空并可解析到有效 report/definition/version；只有 `formSlotType/formTemplateId` 的动态表单槽位不构成该证明。
- Blocker: 进度不足、非当前组长负责范围、缺正式批记录绑定、过程检验/损耗槽位只有动态表单模板而无传统 `batchRecordReportId`、PQC 汇集未确认或无结构化明细、路线存在 `LOSS_REPORT` 但正式损耗单映射未证明、缺放行负责人、幂等冲突、eDHR 批次或放行事务无法持久化时必须返回 blocker 或 fail fast，不得创建不完整资料。
- Verification: 后端静态/单元回归至少覆盖成功、进度不足、非当前组长、正式来源缺失、重复申请幂等和负责人缺失；schema 测试锁定申请表唯一键、状态字段、来源快照和权限码；前端静态合同覆盖双 100% 按钮、确认文案、刷新和 blocker 展示；真实 E2E 必须使用任务自有双 100% 活跃订单、测试账号、签名、正式模板和可清理数据。
- Forbidden action: 禁止用 `formBindings`、默认 `MAIN`、工序开始配置、当前登录人、空资料、mock、直接 SQL、API-only、默认成功、吞异常或直接调用负责人电子签名放行来替代正式资料生成与待审批链路。
- Evidence: `doc/tasks/20260808-active-order-release-dossier-implementation/verification-report.md`。

## 第三方报工直报正式链路门禁

### 导入成功必须落到正式报工而不是直接进度

- Trigger: 第三方报工、李萍报工单、直接报工 Excel、`importDirectWorkReportWorkbook`、`DIRECT_WORK_REPORT`、导入结果弹框显示成功但正式报工列表无新增、排产进度疑似未增长、排产员工作台工序列表班次产能为 0、点击重排提示“排产资源缺少班次小时配置”。
- Preflight check: 先确认导入成功路径是否创建 `MesProFeedbackDO`、设置 `sourceImportRecordId`、回写导入记录 `feedbackId`、调用正式提交服务，并由正式报工状态参与排产进度汇总；第三方报工或已完成任务在手动重排里只用于计算已完成量和剩余量，不能用旧任务 `mes_pro_task.workstation_id`、旧产线或历史排产快照决定剩余工序怎么排；若工作台班次产能为 0，必须按当前路线工序 `process_id` 核对启用未删除工作站、工作站设备绑定和 `mes_dv_machinery_process` 小时产能，而不是只按工序编码找旧工作站；工作站 `shift_hours` 为空、非正数或多个当前可用工作站班次小时不一致时，手动重排必须按 `scheduleDefaultCompatibilityPolicy.defaultShiftHoursWhenMissing()` 的默认 `10.5` 小时计算，不得把缺班次小时误判为旧任务资源 blocker；手动重排后验证资源落库时，必须通过 `mes_pro_task_schedule_ext.schedule_order_id -> mes_pro_task.workstation_id` 核对新生成任务的实际资源，不能只看可能未回写的历史 `mes_pro_schedule_order_process.workstation_id` 快照。
- Blocker: 若缺少报工人、审批人、唯一未完成任务、排产工序剩余数量、正式路线工序快照、当前 `process_id` 工作站、产线或设备工序产能，不得写 `progressSourceType=DIRECT_WORK_REPORT` 或直接改进度/班次产能伪造成功；`FEEDBACK`/`FINISHED` 进度事实任务缺旧工作站或旧产线不是手动重排 blocker，真正需要阻断的是当前工艺路线剩余工序缺少可用工作站、产线或产能；工作站班次小时缺失不是 blocker，应按默认 `10.5` 计算；`IN_PROGRESS`、`LOCKED`、`MANUAL` 等仍需要作为真实受保护任务校验其现有资源。必须返回结构化跳过原因或 fail fast。
- Verification: 后端回归必须同时覆盖匹配行创建/提交正式报工、缺用户跳过、重复导入再次正式报工、超剩余跳过、导入后反馈/已完成任务只扣减剩余量且剩余任务按当前工艺路线资源生成、缺班次小时默认 `10.5` 且不掩盖缺工作站/缺产能；前端静态合同需确认导入确认后刷新正式报工列表并广播受影响排产工单刷新 payload，真实 E2E 需至少覆盖一次第三方报工导入后的手动重排预览或应用；跨环境补工作站数据后必须复验工作台目标工序 `shiftCapacityTotal` 为非 0 且资源链路行数可追溯；重排应用后必须记录排产工单计划时间、`mes_pro_task_schedule_ext` 任务数、空/失效工作站数、覆盖工作站数和最近一次重排快照。
- Forbidden action: 禁止用导入记录直接进度、前端假新增、默认成功、空列表刷新或 API-only 结果替代正式报工持久化链路。
- Evidence: `doc/tasks/20260801-third-party-feedback-import-list-progress/verification-report.md`；`doc/tasks/20260802-test-server-replan-protected-task-workstation/verification-report.md`；`doc/tasks/20260802-test-server-replan-shift-hours-duration/verification-report.md`；`doc/tasks/20260806-replan-current-route-after-feedback/verification-report.md`；`doc/tasks/20260806-replan-shift-hours-default-regression/verification-report.md`。

### 生产组长报工管理造数必须补齐工序池时间线

- Trigger: 生产组长报工管理随机数据、`team-leader/submission/page`、`MesTeamLeaderWorkbenchService.getSubmissionPage`、`MesProProcessPoolTimelineReadMapper`、`actualEmployeeUserName` 为空、员工列显示用户编号或 `964`、只写 `mes_pro_feedback` 后组长页面无新增。
- Preflight check: 先确认页面读模型按 `mes_pro_process_pool_event.server_submit_time`、`actual_employee_id` 和生产组长责任员工集合筛选；时间线 mapper 必须按 `pool_event.actual_employee_id`、`tenant_id`、`deleted` 关联 `system_users` 并返回 `nickname AS actualEmployeeUserName`；造数必须同时补齐正式报工、记录本 entry/event、工序池 `PRODUCTION_SUBMIT` 事件、数量片段和 `mes_pro_process_pool` 汇总，并核对员工在目标生产组长的 `EMPLOYEE` scope 内。
- Blocker: 只有 `mes_pro_feedback` 而缺工序池事件、记录本或数量片段，`actual_employee_id` 不在当前生产组长责任范围，缺 `route_process_id/process_id/work_order_id/task_id` 正式链路，mapper 返回 `NULL AS actualEmployeeUserName`，前端把 `actualEmployeeUserId` 当员工显示文案，或只能用 admin 登录态看到数据时必须停止，不得宣称生产组长报工管理可见。
- Verification: 用数据库只读 SQL 同时断言报工、工序池事件、记录本 entry/event、数量片段计数和 `actual_employee_id -> system_users.nickname` 可解析；再使用生产组长本人登录态请求 `/admin-api/mes/pro/process-pool/team-leader/submission/page?leaderType=PRODUCTION&submitDate=<date>`，按事件 ID 或任务标识断言命中新增数据且 `actualEmployeeUserName` 非空；静态合同锁定 mapper 不得返回空姓名、前端不得退回显示员工 ID。
- Forbidden action: 禁止用 admin 页面、API-only 非组长账号、前端假行、空列表刷新、直接改工序池汇总、只改报工主表、前端硬编码姓名或显示用户 ID 替代生产组长真实时间线可见性。
- Evidence: `doc/tasks/20260806-production-leader-feedback-random-data/verification-report.md`；`doc/tasks/20260806-team-leader-employee-name/verification-report.md`。

### 一线生产正式提交必须单事务落链并按唯一组长归属可见

- Trigger: 一线生产填写页“提交”改为正式提交、重复点击、提交前预校验、无设备工序提示“当前工序缺少正式设备配置”、设备参数缺失、员工无生产组长归属或多组长归属、电子签名提示“当前登录账号必须是实际填写员工”、提交成功但对应生产组长报工列表无记录。
- Preflight check: 前端只做数量、损耗和设备参数的本地提前提示及不可逆确认，确认后只调用一次正式提交接口；后端运行态必须返回服务端解析的 `productionSubmitContext`，其中路线、路线工序、MES 工序、工作站、设备账号、实际员工和生产组长审批人来自正式运行态候选与启用生产人员档案。一线生产不需要匹配任何工单，`workOrderId`、`taskId`、`itemId`、`recordbookId`、`scheduleOrderId` 和 `scheduleOrderProcessId` 可以为空且不得作为运行态或正式提交前置条件；当前工序没有正式记录本上下文时不写 `recordbookPayload`，也不得用默认工单、默认任务、默认物料或默认记录本补齐。PQC 和其它订单级流程仍按各自门禁要求订单上下文。后端必须在写入前按启用生产人员档案确认实际员工只属于一个生产组长；正式提交授权只校验路线、路线工序、MES 工序、实际员工、签名员工和模板，请求仍携带工作站用于提交追踪，但不再用 route-start/post-binding 候选的 `deviceId` 或 `workstationId` 拦截提交；正式提交阶段不执行设备参数校验，不因 `selectedDevice` 缺失、`processPoolContext.deviceId` 与 `selectedDevice.deviceId` 不一致、设备参数缺失/重复/异常或设备参数规则不匹配而阻断。设备端登录账号只代表入口账号，正式签名主体必须是页面选择的实际填写员工：`signatureEmployeeId` 必须等于 `actualEmployeeId`，但不得要求其等于 `loginUserId`；生产提交签名服务必须显式传入该选择员工作为 actor 并验证其电子签名密码。报工、可选记录本原始条目、生产提交签名、工序池 `PRODUCTION_SUBMIT` 事件和正式响应 ID 必须处于同一事务。前端设备卡片必须使用运行态 `devices` 的全量集合，不得通过 `slice(0, 3)` 或同类展示层截断隐藏工序设备。
- Preflight detail: 一线生产运行态和正式提交不得解析、匹配或要求 `productionSubmitContext.activeOrder`、生产工单、生产任务、产品物料或开启记录本；同一路线存在多个 ACTIVE 活跃订单、没有 ACTIVE 活跃订单或没有生产任务时，一线生产仍按当前候选的路线/工序/工作站/员工上下文提交。若未来需要订单级分配、PQC 或工单追溯，必须建独立订单级链路，不得恢复一线生产提交的隐式工单匹配。
- Device parameter default detail: 一线运行态设备数值参数必须优先使用正式显式默认值；显式默认值为空且上下限同时存在时，统一以 `(lowerLimit + upperLimit) / 2` 解析运行态默认值。文本标准或任一边界缺失时保持空值，前端在执行 `Number(...)` 前必须显式排除 `null`、`undefined` 和空字符串，不得把空默认值转换成 `0`。
- Signature authorization detail: 生产组长人员管理中的电子签名授权必须按人员身份分流。正式员工档案有 `system_user_id`，签名前必须命中同租户、未删除、`electronic_signature_enabled=1`、`authorization_state=ENABLED` 且未锁定的 `dcc_electronic_signature_authorization`；临时工档案没有 `system_user_id`，只能使用该启用人员档案自己的非空 `signature_password_hash`。批量开通权限前必须分别统计正式员工和临时工，核对正式员工系统用户启用且同租户、临时工签名密码已设置，并为每条系统用户授权写 `dcc_electronic_signature_authorization_audit`；不能把人员档案 ID 当系统用户 ID 写入 DCC 授权表。
- Blocker: 员工无启用组长归属、同时属于多个启用组长、签名员工与实际填写员工不一致、路线/路线工序/MES 工序/工作站/审批人/签名等正式必需上下文缺失、一线生产仍要求或匹配活跃订单/生产工单/生产任务/产品物料/记录本、前端把运行态设备集合截断为前三台或其它固定数量、客户端审批人、URL query 或预传 `signatureId` 被当作权威、当前运行 Jar 未加载本次正式链路时必须停止；前端失败后保留输入，不得显示成功或锁定状态。
- Verification: 后端回归覆盖运行态 `productionSubmitContext` 无活跃订单/无工单/无任务/无记录本仍返回生产提交上下文、正式提交可写入空 `workOrderId/taskId/itemId/recordbookId`、选择员工与登录账号不同但签名密码匹配时生成选择员工签名、无归属、多归属、唯一归属、授权工序合法但提交设备/工作站与候选设备/工作站不一致时放行、正式提交服务不调用设备参数校验器、幂等和任一步骤异常整事务回滚；前端静态合同覆盖无设备提交不再被缺设备文案阻断、有设备确认弹窗仍展示并提交设备参数、设备卡片直接展示全部 `configuredDeviceCards`、正式上下文来自运行态且提交只传 `signaturePassword` 不传前端 `signatureId`，并禁止 `signatureEmployeeId === currentLoginUserId`、订单/任务/记录本必填、固定前三台设备截断和 URL query 幂等键这类拦截；同一设备连续报工还必须证明每次确认只发一次正式请求、明确成功后清空本次业务输入并轮换新幂等键、失败或响应不确定时保留原输入和原幂等键、成功结束后可切换另一实际员工和另一工序。真实 Playwright 从生产填写页确认提交时，断言正式接口只发一次、返回报工/可选记录本/签名/工序池 ID、本次正式事实不可修改且页面进入新的独立填写会话，再由唯一对应生产组长本人登录报工列表按事件或任务标识确认可见且其他组长不可见。
- Verification detail: 运行态回归必须覆盖“当前组长没有 activeOrder/workOrder/task/recordbook 时仍返回生产提交上下文”，并复跑员工切换相邻测试，防止选择员工触发运行态刷新后误报 `productionSubmitContext.activeOrder routeId=...`。
- Device parameter default verification: 回归必须同时覆盖显式默认值优先、完整数值范围生成精确中点、文本标准保持空值、单边范围保持空值，以及前端空值不进入数值归一化；不能只验证某一条清洗功率样本。
- Signature authorization verification: 数据授权任务必须独立证明目标启用人员总数、正式员工系统用户数、临时工人数、临时工签名密码就绪数、正式员工有效 DCC 授权数和剩余缺口；授权事务需断言授权变更数与审计新增数一致，并复跑验证幂等且不产生重复授权或重复审计。
- Forbidden action: 禁止用第二个预校验请求替代事务内权威校验，禁止默认组长、默认工单、默认任务、默认产品物料、默认记录本、公共待认领列表、前端 `approveUserId`、当前登录人替代 `signatureEmployeeId`、URL query 拼接 `taskId/recordbookId/signatureId`、旧运行 Jar、API-only、直接 SQL、恢复 activeOrder 匹配、只写 `mes_pro_feedback` 冒充正式提交闭环，或为了放宽设备/参数校验同时放宽工序、工位、人员、签名和事务校验。
- Device parameter default forbidden action: 禁止由前端重复计算范围中点、用下限/上限单边猜测默认值、让 `Number(null)` 生成 `0`，或用固定常量覆盖显式目标值。
- Signature authorization forbidden action: 禁止给临时工伪造系统用户或把 `employee_profile.id` 插入 `dcc_electronic_signature_authorization.user_id`；禁止用电子签名菜单、角色、默认授权、空密码或前端可见状态代替正式员工 DCC 授权或临时工人员档案签名密码；禁止只写授权不写授权审计。
- Evidence: `doc/tasks/20260807-formal-frontline-production-submit/verification-report.md`；`doc/tasks/20260807-frontline-submit-optional-equipment/verification-report.md`；`doc/tasks/fix-electronic-signature-selected-employee/verification-report.md`；`doc/tasks/fix-frontline-active-order-route-id-context/verification-report.md`；`doc/tasks/fix-frontline-production-no-work-order-context/verification-report.md`；`doc/tasks/20260808-frontline-submit-relax-device-param-validation/verification-report.md`；`doc/tasks/20260809-frontline-range-midpoint-default/verification-report.md`。

## MES PQC 项目级检验快照门禁

### PQC 检验项目事实必须来自发布规程和结构化 itemResults

- Trigger: PQC 填写、PQC 组长复核、QA 检验规程、检验设备、设备编号、无设备检验项目、`equipmentRequired=false`、接收标准、检验方法、参数上下限、`itemResults`、`rawPayload.pqcPieceValues`、`pqcItemDetails`、固定 `length/appearance/seal/pressure` 字段。
- Preflight check: 修改 PQC 链路前先核对发布 QA 规程项目、`equipmentRequired`、项目级设备表、设备台账编号归属、接收标准上下限、单位和精度字段；提交契约必须以结构化 `itemResults[]` 为业务事实，后端在提交时从发布规程冻结设备、编号、方法、标准、上下限、单位、精度、实测值和判定。设备是否必填必须按单个 QA 项目判断：`equipmentRequired=true` 才强制 `selectedEquipmentId/selectedEquipmentNumber` 并校验项目设备归属；`equipmentRequired=false` 且无设备选项是正式无设备项目，应允许设备字段为空并保存空设备快照。一线弹框、卡片摘要、组长列或提交快照若展示“接收标准/检验方法”，必须使用显式 QA 工序列字段或别名（例如 `acceptanceStandard/processInspectionMethod`），不能直接让默认首检摘要、判定值、上下限合成文案或旧兼容字段成为可见来源。
- Blocker: 客户端提交可改写接收标准或检验方法、后端仍把 `rawPayload.pqcPieceValues` 当权威、组长页仍按固定四项字段展示、设备编号未按项目设备归属校验、无设备项目仍被要求选择设备、缺发布规程项目或设备必填项目缺设备主数据时默认成功，必须停止。
- Verification: 后端回归需覆盖 schema、项目设备 mapper、`itemResults` 提交、设备编号归属校验、设备必填项目明细冻结和无设备项目空设备快照；前端静态或真实路径需覆盖填写页每项目设备/编号/标准/方法入口、无设备项目显示“无需设备”、组长页读取 `pqcItemDetails/itemResults`，并复跑相邻 eDHR/PQC 布局合同和 `pnpm ts:check`。
- Forbidden action: 禁止用整单设备替代项目级设备，禁止把所有 PQC 项目统一当作设备必填，禁止用固定四项字段、前端文案、默认上下限、默认首检规则、判定值、空标准、raw payload 或 API-only 展示替代正式项目级快照。
- Evidence: `doc/tasks/20260803-pqc-equipment-standard-method-implementation/verification-report.md`；`doc/tasks/20260808-pqc-optional-equipment-items/verification-report.md`；`doc/tasks/20260808-pqc-qa-process-standard-method-source/verification-report.md`。

### QA 多工序正式发布与退役夹具唯一键必须隔离

- Trigger: QA 规程页面一次维护多个工序组、按路线工序发布、`routeProcessId`、激活路线版本快照、`owner_module=MES_QA`、退役 `CODX_QA` 夹具仍占路线工序唯一键、正式接收标准或检验方法仍显示测试夹具文案。
- Preflight check: 发布前必须读取当前唯一 ACTIVE 路线版本中的路线工序身份，按项目的明确业务工序范围逐项解析并分组，每个 `routeProcessId + processId` 单独生成规程 payload；业务标签包含复合名称时必须维护显式映射，不能按包含关系把相邻工序猜入范围。QA 业务工序与批记录表单绑定是两个独立事实：业务方明确确认某 QA 工序即使尚未识别批记录绑定也要保留时，必须使用项目级显式允许清单，只把该项目归入页面已经唯一解析的正式 QA 质检工序身份，保留原业务工序显示，并让批记录绑定摘要为空；不得据此猜测它对应其它路线工序。修复本地测试数据前必须精确核对唯一键占用行的 owner、生命周期、deleted、版本、项目、任务状态和逐件明细；若退役 `CODX_QA` 占键，只有取得明确授权后才能软删除该退役 owner 行，再新建独立 `MES_QA/PUBLISHED` 规程，旧版本、旧项目和已取消任务继续保留审计。
- Blocker: ACTIVE 路线版本缺失或不唯一、项目无法唯一映射正式路线工序且未进入业务方确认的未绑定允许清单、允许未绑定但页面无法唯一解析正式 QA 质检工序身份、使用当前编辑态路线工序 ID、把未获允许的跨工序项目混入一个 payload、正式运行态仍引用非 `MES_QA/PUBLISHED` 规程、退役夹具占键但未获处理授权、目标任务已有提交或逐件明细时必须停止。
- Verification: 前端合同必须覆盖激活版本工序解析、逐工序 payload、复合工序显式映射、显式允许的未绑定业务工序继续显示且不产生批记录绑定摘要，以及任一请求失败时整体显式失败；后端回归必须锁定正式 owner/发布态准入和项目级快照；真实 Playwright 至少确认未绑定业务工序和 QA 来源列可见、目标写请求为 0，完整发布链路还必须满足任务数据清理门禁。数据修复后核对正式规程、版本、FIRST/PATROL/FINAL 项目、待检任务及零明细，并通过真实 Playwright 页面确认标准、方法和来源均为发布 QA 规程快照且不含夹具/默认规则文案。回滚脚本必须只处理未变化的任务自有新记录并恢复被软删除的退役 owner 行。
- Forbidden action: 禁止把全部 QA 项目默认发布到单一路线工序，禁止把未识别批记录绑定扩展成全局宽松规则，禁止用字符串模糊匹配猜测工序，禁止隐藏或删除业务方明确要求显示的未绑定 QA 工序，禁止为其伪造批记录绑定摘要，禁止把当前可变路线工序 ID 当发布快照身份，禁止把 `CODX_QA` 改 owner 或重新发布成正式规程，禁止硬删除旧版本、旧项目和已取消任务，禁止用默认首检规则或夹具文案补齐缺失正式配置。
- Evidence: `doc/tasks/20260809-pqc-formal-standard-method-source/verification-report.md`。

### QA 抽样方案与适用检验类型必须共用项目级正式来源

- Trigger: QA 规程“适用检验类型”、项目抽样方案、首件/首检数量、上午巡检、下午巡检、`AQL`、`patrolInspectionRatio`、末检开关、保存或发布规程。
- Preflight check: 修改 QA 检验类型展示或保存链路前，必须逐项目核对抽样方案解析、页面只读展示、完整性检查和保存载荷是否共用同一正式派生逻辑；不得保留可编辑 `applicableTypes` 数组或全局抽样比例作为第二数据源。涉及比例字段时必须同时核对上下游单位和最终计算公式：当前 `patrolInspectionRatio` 存储百分比原值，任务数量公式再执行 `plannedQuantity × ratio ÷ 100`，因此 `AQL=0.4` 应保存为 `0.4`，前端不得二次除以 100。若一线或发布版需要展示 QA 项目的“抽样方案”“检验器具及设备”等人类可读原文，必须把原文作为独立正式字段贯穿保存、发布和运行态响应；数量、比例和设备选项只承担结构化计算或选择职责，不能替代原文。运行态还必须区分“发现正式 QA 工序”和“使用检验详情”两个边界：工序列表只依赖发布规程的产品、路线、版本、工序和检验项身份，历史原文字段为空时原样返回 `null`；打开详情或正式提交时再按同名字段严格校验。
- Blocker: 抽样方案显式包含首检标记但缺合法正整数数量、缺合法 AQL、页面与 payload 派生结果不一致、前后端比例单位未核对、末检关闭后仍序列化 FINAL 项目，或打开检验详情、发起正式提交时历史发布项目缺展示所需正式原文字段，必须停止；不得补默认比例、默认首检数量、拼装历史原文或保留旧数组掩盖缺失。新增原文字段的迁移不得猜测回填历史数据，历史项目需通过正式 QA 保存/发布链路补齐。仅用于“选工序”的接口不得因这些非工序身份原文为空而整单失败。
- Verification: 纯函数合同覆盖无首检、有首检、非法首检数量、缺 AQL 和比例原值；静态合同锁定页面展示、完整性检查和保存载荷共用派生函数，并负向扫描旧 `applicableTypes`、全局数量和全局比例；后端公式核对或回归必须证明百分比只除以 100 一次；原文展示链路还必须逐层断言保存载荷、发布记录、运行态响应和页面直接读取同名正式字段，并覆盖“历史缺字段仍列出正式工序、详情不打开、提交不发送且后端提交边界 fail-fast”；真实页面覆盖末检开关两态且不产生保存/发布写请求。
- Forbidden action: 禁止把 AQL 同时当百分数和小数比例，禁止根据展示文案以外的旧字段猜测首检，禁止用 `firstInspectionQuantity`、`patrolInspectionRatio`、`equipmentOptions` 或前端默认值反推/拼装“抽样方案”“检验器具及设备”原文，禁止让上午/下午巡检分别形成相互冲突的后端 PATROL 比例，禁止用兼容分支或静默跳过无效方案维持假成功，也禁止把详情原文完整性校验提前到只负责发现工序身份的列表边界。
- Evidence: `doc/tasks/20260809-qa-applicable-types-derived/verification-report.md`。

### PQC 待检准入与工序选择必须分离

- Trigger: 一线 PQC 真实页面、`active-order/list`、`active-order/processes`、QA 检验项目列表“工序”列、活跃订单当前产品、生产工单产品路线绑定、订单产品代码不等于项目代码、路线产品绑定物料、DCC 项目代码 `productMasterId`、同一路线绑定多个产品、路线存在额外工序但 QA 项目未配置、只有一个工序存在 `PENDING` PQC 任务、`activeOrderId` 有值但 `routeProcessId/processId=null`。
- Preflight check: PQC 待检工单列表必须以正式 `PENDING` PQC 任务为准入条件，按最新 active order ID 过滤后再加载工单、路线和产品摘要；没有待执行任务时返回空列表，由前端显示业务空态。用户选择工单后，`active-order/processes` 必须依次校验活跃订单、生产工单及当前产品与路线的正式绑定；订单产品只用于定位当前路线，不是 DCC 项目代码。随后读取该路线全部正式 `mes_pro_route_product` 绑定及其物料代码，以这些路线绑定代码精确匹配唯一启用的 DCC 项目代码；把路线全部绑定物料 ID 与该 DCC 项目的 `productMasterId` 共同作为 QA 产品候选，按当前 `routeId + routeVersionId + MES_QA/PUBLISHED` 过滤正式规程，并要求最终只命中一个 QA 产品。候选工序只从该唯一 QA 产品实际存在检验项目的规程中提取，按 `routeProcessId + processId` 去重；当前路线工序只能补充名称、排序和工位，不得扩展候选集合。未绑定当前路线的其它 DCC 项目、活跃订单工序快照、路线全部工序和 `PENDING` 任务都不是候选工序来源。正式 `PENDING` 任务只为已有 QA 候选工序附着 `pqcTaskId`、规程快照和检验项；历史检验项的 `inspectionTool/samplingPlanText` 为空时，列表原样返回空值但仍保留正式工序，打开详情和正式提交再严格拦截。提交链路必须携带正式 `pqcTaskId` 并校验任务、QA 工序、MES 工序、活跃订单和状态一致。非 `CANCELLED` PQC 任务的 `routeProcessId/processId` 必须是正式任务身份。
- Blocker: PQC active order 列表返回的工单没有 `PENDING` 任务、生产工单当前产品未绑定所选路线、路线产品绑定为空、路线绑定物料缺失或物料代码为空、路线绑定代码无法唯一匹配启用 DCC 项目、DCC 项目缺 `productMasterId`、候选产品未命中或命中多个 QA 产品、目标路线版本没有正式 `MES_QA/PUBLISHED` 规程或规程缺检验项目、QA 规程的产品/路线/版本/工序身份不一致、MES 工序缺失或停用、非取消 PQC 任务缺正式工序身份、待检任务不属于 QA 候选工序、提交时所选工序没有正式 `PENDING` 任务时必须停止；不得返回默认成功、推断候选或伪造可提交上下文。`inspectionTool/samplingPlanText` 为空不是工序列表 blocker，但必须成为详情和提交 blocker。
- Verification: 后端回归必须覆盖“订单产品代码与路线项目代码不同时，由订单产品定位路线后使用路线绑定项目代码”“路线绑定物料 ID 与 DCC productMasterId 共同作为 QA 候选且当前路线版本只唯一命中一个 QA 产品”“未绑定当前路线的其它 DCC 项目不参与”“路线有额外工序但 QA 项目只覆盖部分工序时只返回 QA 工序”“同一工序含多个 QA 项目或重复规程时按正式工序身份去重”“有任务工序可附着首检/巡检任务选项”“历史展示原文为空仍返回工序且原样保留空值”“非 `MES_QA` owner 必须 fail fast”“QA 工序身份漂移或缺失 fail fast”“无 active order 返回空列表”“active order 仅有非 PENDING 任务被过滤”“产品路线绑定不匹配 fail fast”“非取消任务缺正式工序身份 fail fast”。前端真实路径应逐项比对 `active-order/processes` 与工序卡片，并确认未配置 QA 检验项目的路线工序不可见、无正式任务的 QA 工序未获得伪造提交上下文、历史展示原文为空时详情不打开且提交请求不发送。
- Forbidden action: 禁止把订单产品物料代码直接当成 DCC 项目代码；禁止用未绑定当前路线的 DCC 项目、活跃订单工序快照、路线全部工序、当前进行状态、待检任务集合、草稿路线、`formBindings`、默认 `MAIN` 或前端补齐逻辑替代或扩展路线项目代码下的 QA 检验项目工序集合；禁止为空任务工序伪造 `pqcTaskId`、规程、检验项或提交成功；禁止在附着正式任务上下文时接受 `CODX_QA`/其它测试 owner。
- Evidence: `doc/tasks/20260808-frontline-pqc-process-cards-qa-items/verification-report.md`；`doc/tasks/20260807-pqc-leader-management-five-records/verification-report.md`；`doc/tasks/20260807-frontline-pqc-pending-order-filter/verification-report.md`；`doc/tasks/20260809-frontline-pqc-qa-project-process-source/verification-report.md`。

### PQC 末检适用性按显式 true 要求 FINAL

- Trigger: AC-M15、PQC 末检、末检不适用、QA 规程发布、`finalInspectionApplicable`、`finalInspectionNotApplicableReason`、`FINAL` 检验项目、PQC 任务生成、放行完整性预检。
- Preflight check: 修改末检、QA 规程发布、PQC 任务生成或放行完整性前，必须核对发布版本表、保存/发布 VO、前端 payload、生成器和放行校验是否都读取同一份 `finalInspectionApplicable` 与 `finalInspectionNotApplicableReason`；放行完整性中只有发布版本明确 `finalInspectionApplicable=true` 才要求 FINAL 任务，历史发布版本 `null` 不再作为 blocker。
- Blocker: 末检不适用但缺依据、适用却缺 FINAL 项目、不适用却仍保存 FINAL 项目、生成器因明确适用却缺 FINAL 任务默认跳过末检、或放行预检无法追溯发布版本依据时必须停止。
- Verification: 后端回归必须覆盖适用生成/要求 FINAL、不适用且有依据跳过 FINAL、历史 `finalInspectionApplicable=null` 不阻塞放行、明确 false 但缺依据阻塞；前端静态或真实路径必须覆盖末检关闭时填写正式依据、payload 提交字段、禁用检验类型不序列化为项目；schema 测试需锁定版本表字段。
- Forbidden action: 禁止把缺少 FINAL 任务、空规则列表、前端开关、历史任务状态或 API-only 说明当作明确不适用依据；禁止在明确 `finalInspectionApplicable=true` 时默认放行或吞掉 FINAL 缺失。
- Evidence: `doc/tasks/20260805-pqc-regulation-task-generation-fix/verification-report.md`。

### PQC 过程检验汇集必须形成最终确认明细

- Trigger: AC-M21、过程检验记录汇集、PQC 组长复核通过、`aggregateApprovedPqcSubmission`、`processInspectionAggregationStatus`、`mes_pqc_process_inspection_aggregate_detail`、`mes_pqc_inspection_task.task_status`。
- Preflight check: 修改 PQC 汇集链路前先核对 `mes_pro_process_pool_event`、`mes_pro_process_pool_pqc_record`、`mes_pqc_inspection_task`、`mes_pqc_inspection_piece_detail` 和汇集明细表的租户、事件、任务、轮次、规程版本、逐件明细来源；汇集只能读取正式 `SUBMITTED` 任务和结构化逐件明细，并在同一事务中 CAS 标记记录已汇集、确认任务为 `CONFIRMED`、写入结构化汇集明细。
- Blocker: 只能证明状态标记而没有结构化明细、仍从 raw payload 汇集、未校验租户/事件/任务一致性、未排除旧修订/未确认任务/重复汇集、或任务确认与明细插入不在同一事务时必须停止。
- Verification: 后端回归必须覆盖成功汇集明细字段、重复汇集 CAS、跨租户拒绝、无逐件明细拒绝、任务确认 CAS 失败回滚，并配合 schema 测试验证唯一键 `tenant_id + event_id + source_piece_detail_id + deleted`。
- Forbidden action: 禁止用前端展示、状态字段、默认空明细、raw payload、API-only 截图或吞唯一键异常替代正式结构化汇集事实。
- Evidence: `doc/tasks/20260805-ac-m21-process-inspection-aggregation-fix/verification-report.md`。

### QA 规程配置状态必须来自产品级规程记录

- Trigger: QA 规程配置页、DCC 项目代码对应产品、`已配置 QA 规程`、`待配置 QA 规程`、产品级检验规则草稿、`qaInspectionTypeRules`、`qaProductRuleDrafts`、`project-statuses`、`mes_qa_inspection_regulation.product_id`、前端硬编码 `IDI` 或压力泵模板判断产品状态。
- Preflight check: 修改 QA 规程配置状态或检验规则前，先核对 DCC 项目代码的 `productMasterId` 与 QA 规程表 `product_id` 的正式关系；配置状态必须由后端按产品 ID 查询 QA 规程记录并返回。页面内尚未保存的规程字段、检验规则和检验项目也必须以 `productMasterId` 为唯一状态 key，切换产品前保存当前产品草稿、切换后恢复目标产品草稿；同一产品的不同 DCC 入口必须复用同一状态，缺产品绑定时清空并阻塞。
- Blocker: 页面把压力泵 `IDI`、产品名称、前端常量集合、空状态、模板初始化数据或查询失败当作配置状态来源，直接以项目代码选择当前规则，多个产品共享同一个可变规则数组，切换产品不重置/恢复规则，状态接口失败时静默把项目归入待配置，或只加载第一页/局部 DCC 候选后就执行已配置排序，必须停止并补齐正式产品状态链路和完整候选输入。
- Verification: 后端回归必须覆盖已配置与未配置产品按请求顺序返回；前端静态契约必须断言调用正式 `project-statuses` API、产品草稿 Map 以正式产品 ID 为 key、切换前保存和切换后恢复、同产品跨项目入口复用、缺产品绑定清空、默认下拉完整加载候选后再排序，并禁止项目代码直接选择当前规则；真实页面回归需覆盖目标已配置产品位于默认第一页之外时仍进入已配置优先组；同时运行相邻 QA 合同和 `pnpm ts:check`。
- Forbidden action: 禁止用前端文案、默认项目、产品名称、项目代码、压力泵样例模板、共享页面单例、API-only 展示或吞掉状态接口错误替代产品级 QA 规程和检验规则事实；样例规则如需保留，只能先通过正式 DCC `productMasterId` 登记产品归属。
- Evidence: `doc/tasks/20260804-qa-regulation-dcc-project-code/verification-report.md`；`doc/tasks/20260805-qa-regulation-product-specific-rules/verification-report.md`。

## MES 工艺路线产品绑定状态门禁

### 产品侧路线选择必须匹配后端可维护状态

- Trigger: MES 物料产品选择工艺路线、产品侧路线下拉、`getRouteSimpleList`、`item-binding-list`、`saveRouteProductByItem`、`validateRouteNotEnable`、已启用路线不可维护。
- Preflight check: 修改产品侧路线选择或 route-product 保存前，先核对下拉数据源返回的路线状态集合和后端维护校验是否一致；若后端禁止维护已启用路线，前端不能使用只返回已启用路线的精简列表作为可选项，必须禁用不可维护路线并调用 `saveRouteProductByItem` 后重新读取 `getRouteProductByItem`。
- Blocker: 产品侧下拉只提供已启用路线但保存接口会因 `PRO_ROUTE_IS_ENABLE` 失败、已启用当前绑定允许清空或改选、产品侧新增第二套路由字段、保存后未重读正式当前绑定、或用前端隐藏错误替代后端 fail-fast 时必须停止。
- Verification: 前端静态契约必须断言产品侧使用专用路线选择接口、禁用已启用路线选项、不调用只返回已启用路线的 `simple-list`；后端回归必须覆盖创建、迁移、解除绑定和旧路线产品 BOM 清理。
- Forbidden action: 禁止为了让产品维护页能选择路线而放宽 `validateRouteNotEnable`、禁用后端校验、使用 `MdItemApi.routeId` 第二关系源、默认成功、吞掉保存错误或混入表单槽位/批记录表单链路。
- Evidence: `doc/tasks/20260804-mes-item-route-selection/verification-report.md`。

### QA 规程手动绑定必须允许已发布路线

- Trigger: QA 规程适用范围手动绑定工艺路线、`data-qa-regulation-manual-route-bind`、`saveQaRegulationRouteProductByItem`、`save-qa-regulation-route-by-item`、已发布路线不能选择、`已启用，仅回显`。
- Preflight check: QA 规程只允许手动绑定“工艺路线”这一正式产品路线关系；路线版本、质检工序、SOP、生产系数和批记录绑定仍必须从已发布路线自动解析。QA 适用工序解析顺序必须显式、可追溯：唯一 `checkFlag=true`、单一正式工序、唯一启用 BATCH `batchRecordReports`、唯一发布投影 `batchRecordReportId/code/name`、唯一路线 `keyFlag=true`；任一候选出现多个都必须 fail-fast，不得猜测。QA 下拉可复用 `getRouteItemBindingList` 候选，但不得按 `CommonStatusEnum.ENABLE` 禁用已发布路线；选择 DCC 项目时必须用 `getRouteProductByItem` 读取到的正式 `routeProduct.routeId` 回填手动绑定下拉默认值；保存必须调用 QA 专用 `saveQaRegulationRouteProductByItem`，后端校验路线存在且有 ACTIVE 版本，不调用 `validateRouteNotEnable`，保存后必须重新读取 `getRouteProductByItem` 并以重读结果作为默认绑定。
- Blocker: QA 下拉把已发布/已启用路线置灰、选择 DCC 项目后不回显已有正式绑定、仍调用 `saveRouteProductByItem` 导致 `PRO_ROUTE_IS_ENABLE`、后端 QA 方法缺少 ACTIVE 版本 fail-fast、绑定后只用本地选择值展示、缺 `checkFlag` 时未按正式批记录或唯一 `keyFlag` 链路解析、多个关键工序仍继续猜测、或把黄框字段重新开放为手工输入时必须停止。
- Verification: 前端静态契约必须断言 QA 页面不再禁用 `CommonStatusEnum.ENABLE`、不显示“已启用，仅回显”、选择 DCC 项目会把正式 `routeProduct.routeId` 赋给 `manualQaRouteBinding.routeId`、调用 `saveQaRegulationRouteProductByItem` 并重读当前绑定、无 `checkFlag` 路线按正式批记录/发布投影/唯一 `keyFlag` 顺序解析且不使用 `formBindings`；后端回归必须覆盖 QA 新建绑定、修正既有绑定、缺 ACTIVE 版本失败、Controller QA endpoint 和不调用 `validateRouteNotEnable`。
- Forbidden action: 禁止放宽产品维护页 `validateRouteNotEnable` 来满足 QA；禁止用前端本地值、默认路线、`formBindings`、批记录表单、空成功或吞异常冒充 QA 绑定成功。
- Evidence: `doc/tasks/20260805-qa-regulation-publish-fix/verification-report.md`。

### 零排产活跃订单必须使用发布态正式路线

- Trigger: 生产组长活跃订单候选/新增、已确认生产工单没有有效排产工单、`MesTeamLeaderActiveOrderServiceImpl`、`mes_pro_route_product`、`mes_pro_route_version.route_snapshot_json`。
- Preflight check: 先按生产工单产品读取唯一未删除的 `mes_pro_route_product` 正式绑定，再读取该路线唯一 `active=1 AND lifecycle_status=ACTIVE` 版本；若未删除绑定指向已删除路线，必须先把它当作孤儿正式关系只读暴露并精确修复，不能让它参与“唯一绑定”判断。工单产品 ID 与 QA 产品 ID 不同时，必须读取该正式路线的全部产品绑定和 MES 物料编码，以物料编码与启用 DCC 项目代码做精确等值匹配；只接受 `productMasterId` 非空且唯一的 DCC 项目，再把路线产品 ID 与该 DCC `productMasterId` 组成 QA 查询范围，并只保留精确命中当前 ACTIVE 路线/版本、`PUBLISHED` 且有当前版本的规程。最终只允许一个 QA 产品上下文，取消工单必须在加载路线、DCC 和 QA 前先行阻断。运行工序、顺序和数量系数必须从发布快照 `configSnapshots.flowGraph.nodes` 与 `scheduleUseConfigs` 逐项匹配，ERP 数量必须来自生产工单正式字段；PQC 规程也必须绑定发布快照里的 `routeProcessId/processId`，不能用当前 `mes_pro_route_process` 重建后的新 ID 代替。零排产不得以 ERP 计划开工时间为空或 PQC 业务日期为由拒绝候选/新增；PQC 非空记录日期使用已落库活跃订单的 `joinedAt` 日期。有一条有效排产时优先使用工序 `planDate`，`planDate` 为空时不得阻断候选/新增，PQC 业务日期使用已落库活跃订单的 `joinedAt` 日期。候选资格和新增写入必须复用同一个路线来源解析契约。宽关键词可能命中大量工单时，候选数量上限只能在全部匹配项完成正式路线、唯一 ACTIVE 版本、DCC 项目和已发布 QA 资格解析，并按资格优先排序后应用；数据库状态排序不能替代正式资格排序。
- Blocker: 产品无绑定/多绑定、绑定只指向已删除路线、ACTIVE 版本缺失/不唯一、路线无法精确匹配唯一启用且已绑定产品主数据的 DCC 项目、当前 ACTIVE 路线版本的已发布 QA 产品上下文缺失/不唯一、快照节点与 SCHEDULE 配置集合不一致、PQC 规程未按发布快照 routeProcessId 建档、明确适用末检却缺 FINAL 项目、FIRST/FINAL 同一检验类型固定数量不一致、工序重复、没有启用工序、数量系数非正数、ERP 数量非正数或正式 PQC 规程缺失时必须 fail fast。ERP 计划开工时间缺失不是零排产 blocker；历史 PQC 发布版本 `finalInspectionApplicable=null` 不再单独阻塞。有效排产工单为 1 条时继续使用排产路线/版本/工序计划；大于 1 条时仍按冲突阻塞。
- Verification: 后端测试至少覆盖零排产成功、工单产品与 QA 产品 ID 不同但经正式路线和唯一 DCC 项目解析成功、旧路线版本 QA 排除、DCC 缺失/歧义、QA 产品上下文歧义、取消工单先行阻断、孤儿路线绑定不参与有效路线唯一性、缺绑定、缺 ACTIVE 版本、快照不完整、ERP 计划开工时间为空仍可加入且 PQC 日期等于活跃订单 `joinedAt` 日期、单排产 `planDate` 有值继续使用工序 `planDate`、单排产 `planDate` 为空时使用活跃订单 `joinedAt` 日期和多排产冲突；宽关键词回归必须构造超过候选上限的匹配项，并让符合资格的目标工单位于原始数据库排序上限之外，证明最终仍按资格排序进入返回结果。真实 E2E 必须通过页面按工单号和宽关键词分别搜索并确认候选资格，写入型验收还必须使用任务自有工单，并只读核验工序快照数量系数/计划数量、PQC 任务生成、PQC 任务 routeProcessId 来自发布快照、FIRST/PATROL/FINAL 任务数量和业务日期，最后精确清理任务数据。
- Forbidden action: 禁止把零排产或跨产品 QA 解析实现为默认路线、任取第一条绑定/版本/DCC 项目、产品名称匹配、项目名称匹配、前缀或模糊匹配、跨路线 QA、读取草稿当前配置、用当前路线工序表 ID 替代发布快照 routeProcessId、默认数量系数、默认 QA 规程、用需求日期或未落库的临时时间猜测 PQC 日期、继续要求 ERP 计划开工时间、空工序成功、前端文案放宽或 API-only 成功；禁止在 mapper 或其它资格解析之前按固定条数截断宽关键词结果，也禁止仅把“已确认”状态提前排序后继续资格前截断。
- Evidence: `doc/tasks/20260807-active-order-without-schedule-order/verification-report.md`；`doc/tasks/20260808-pressure-pump-active-orders/verification-report.md`；`doc/tasks/20260809-active-order-route-dcc-qa-resolution/verification-report.md`。

### FIFO 自动分配当前工序快照边界

- Trigger: 生产组长报工管理、`FIFO 自动分配`、活跃订单提示“缺少当前工序生产系数和目标数量快照”、同一基础工序 `processId` 但不同 `routeProcessId`、`MesTeamLeaderFifoAllocationService`、`MesTeamLeaderOrderProcessTargetService`。
- Preflight check: 修改 FIFO 预览或报工确认前，先区分自动候选和指定确认：FIFO 预览只能消费含当前 `routeProcessId + processId` 快照的活跃订单，缺当前 routeProcess 快照的较早活跃订单应视为当前预览不可分配并继续后续候选；手工/最终确认指定的活跃订单仍必须用 `requireTarget` fail-fast。生产系数未显式设置时按业务默认 `1` 归一，目标数量缺省按 ERP 数量乘以生产系数派生；非正 ERP 数量、非正系数或非正目标数量仍必须失败。
- Blocker: FIFO 预览因为非当前 routeProcess 的活跃订单直接阻塞当前工序、手工确认把缺当前快照的指定订单静默成功、生产系数缺省被当成非法空值、非正数量被默认成功、或预览和最终确认共用同一宽松路径时必须停止并补回归。
- Verification: 后端回归必须同时覆盖“FIFO 预览跳过不含当前 `routeProcessId + processId` 快照的活跃订单并继续分配”“缺省生产系数按 `1` 派生目标数量”“非正系数仍失败”“最终确认/手工指定仍 fail-fast”，并复跑 FIFO 闭环和 PQC 分配相邻测试。
- Forbidden action: 禁止为了消除 FIFO 报错而全局默认目标数量、吞掉 `requireTarget` 错误、用基础 `processId` 替代 routeProcess 身份、把缺正式快照的指定订单当成功、改前端隐藏错误或用 API-only 说明替代服务回归。
- Evidence: `doc/tasks/20260808-fifo-active-order-process-target/verification-report.md`。

### 工序共享分配池与旧报工终结链路边界

- Trigger: 生产组长报工分配、共享数量池、`allocation/confirm`、旧报工确认、PQC 质量门禁、正式批记录回填、跨订单目标工序上下文。
- Preflight check: 先区分“共享分配保存”和“旧报工确认终结”两个写入职责。共享分配只读取生产报工的正式输出数量，按目标活跃订单自身 `routeProcessId + processId` 保存版本化分配、完成量、数量碎片和调整审计；来源报工只提供数量/字段值，不能把来源工序 ID 当成目标订单工序 ID。共享分配不得隐式调用旧 PQC 或批记录回填门禁；旧确认接口仍独立执行既有质量和批记录规则。调整审计字段为非空时，复核说明为空必须由服务按分配模式写入明确系统原因，不能让数据库默认值或前端必填假设决定事务成败。
- Blocker: 共享分配因缺旧 PQC/批记录配置、已退出排产池的订单、来源与目标 routeProcess 不同而错误拒绝，或为绕过拒绝而放宽旧确认质量门禁、静默跳过目标上下文、吞异常、写入默认成功时必须停止并补回归。旧确认质量/批记录规则被共享分配改弱也必须停止。
- Verification: 后端回归必须分别覆盖共享分配无 PQC/批记录配置仍可提交、目标订单无排产记录仍按活跃订单快照完成、来源与目标 routeProcess 不同的目标上下文、旧确认继续执行 PQC/批记录门禁，以及空复核说明的审计原因落库；真实 Playwright 必须验证 FIFO 保存、未放行手动调整、余量留存、报工管理分配订单列和历史投影。
- Forbidden action: 禁止把旧确认终结服务作为共享分配的隐式前置条件，禁止用 `formBindings`、默认批记录槽位、来源事件 routeProcess、前端空说明或数据库默认值替代正式目标上下文和审计原因，禁止用 API-only、SQL 或 mock 冒充真实页面成功。
- Evidence: `doc/tasks/20260809-process-report-shared-allocation-pool/verification-report.md`。

## MES 生产人员档案正式工重复关联门禁

### 同一组长正式工关联必须先业务拒绝再写库

- Trigger: 生产人员档案、班组员工、一线生产员工弹窗、`getFrontlineRuntimeConfig().employees`、正式工搜索关联、全量用户下拉、跨部门正式工、临时工/正式工统一候选、`getUserListBySubordinate`、`getUserListByNickname`、`mes_pro_process_pool_team_employee_profile`、`system_user_id`、`employee_code=USER-<id>`、DuplicateKeyException、重复关联返回 500。
- Preflight check: 先明确正式工候选范围是“组长下属”还是“全量系统用户”；候选查询与提交关联校验必须使用同一范围，不能只放开下拉。一线生产员工弹窗、运行配置员工和切换员工校验必须同源于当前负责生产组长启用的生产人员档案。生产员工账号请求一线工序时，必须先按启用人员档案 `systemUserId -> 唯一 leaderUserId` 解析负责生产组长，再读取该组长在正式“工序开始”配置中负责路线下的全部工序；禁用档案、多组长归属或组长无正式负责工序必须显式失败，不得回退本人岗位、设备账号路线或历史工序员工绑定。即使设备/工序 scope leader 与当前负责组长不同，员工弹窗也不得切到设备 scope leader，不得用工序员工绑定或设备账号候选替代人员管理列表。新增正式工关联前还必须按当前 `leaderUserId + systemUserId` 查询现有未删除生产人员档案，并区分“已禁用可启用既有档案”和“从未关联可新增”；显示名唯一校验不能替代正式用户唯一关联校验。新增正式工/临时工档案只建立当前组长名下档案，不得要求该员工已在负责员工范围内。生产组长员工 scope 必须同步写入与正式提交一致的 `actualEmployeeId`：正式工使用 `systemUserId`，临时工使用人员档案 `id`，测试不得只断言其中一种身份。
- Historical data gate: 当运行时代码已保证新增/启停人员同步 `PRODUCTION/EMPLOYEE` scope，但旧人员档案仍缺范围时，必须用正式幂等数据迁移按 `tenant_id + leader_user_id + COALESCE(system_user_id, profile.id)` 补齐；迁移前应阻塞重复派生身份、重复有效范围和档案/范围启用状态错位，迁移后验证全量覆盖并复跑幂等。组长报工读取仍只消费正式 scope，禁止改成读取人员档案推断、管理员全量可见或查询时自愈写入。
- Blocker: 全量候选可见但提交仍按 `getUserListBySubordinate` 拒绝、候选只靠前端过滤、生产员工仍需逐工序绑定才能看到工序、员工档案存在却进入设备/岗位路线来源、一线生产弹窗和生产组长人员管理列表不一致、弹窗能选但 `switchActualEmployee` 按另一来源拒绝、重复正式工关联落到数据库唯一键异常、接口返回 500、禁用旧档案后再次新增同一系统用户、创建人员阶段触发 `assertCanAccessEmployee` / `assertCanMaintainProcess`、范围拒绝仍返回“员工或工序”混合文案，或只靠前端禁用按钮阻止重复时必须停止并补后端 RED/GREEN。
- Verification: 后端回归必须覆盖生产员工继承唯一负责组长的全部正式路线工序、禁用/多组长/无负责工序失败且不触发设备路线来源、候选数据源、运行配置员工来源、切换员工校验同源、空关键字不触发无条件全量扫描、重复正式工在 `employeeProfileMapper.insert` 前抛业务错误，且成功正式工路径仍不保存签名密码；前后端合同必须断言员工工序绑定写入口和运行时读取均已移除。新增人员成功路径必须断言不调用负责范围校验；跨模块新增 `AdminUserApi` 方法时必须用 `-am` 编译所有上游测试手写实现。真实 E2E 重跑时使用新的任务自有正式工候选或先明确启用既有档案。
- Forbidden action: 禁止只改候选接口不改关联校验，禁止让前端加载全系统用户后本地过滤，禁止保留员工工序绑定入口或用工序员工绑定冒充人员管理列表和工序授权，禁止生产员工归属异常时回退岗位/设备路线，禁止为接口扩展增加默认空列表兼容 fallback，禁止 catch DuplicateKeyException 后返回默认成功，禁止创建重复正式工档案，禁止把正式工重复关联伪装成显示名重名，禁止用“员工或工序”混合错误掩盖实际越权目标。
- Evidence: `doc/tasks/20260805-production-personnel-management/verification-report.md`、`doc/tasks/20260805-production-personnel-full-user-dropdown/verification-report.md`、`doc/tasks/20260806-production-employee-create-scope-fix/verification-report.md`、`doc/tasks/20260806-frontline-production-employee-options-match-leader-personnel/verification-report.md`、`doc/tasks/20260807-production-employee-inherits-leader-processes/verification-report.md`；目标测试 `MesTeamLeaderRuntimeConfigServiceTest#shouldRejectDuplicateFormalUserBeforeDatabaseInsert`、`MesTeamLeaderRuntimeConfigServiceTest#shouldSearchFormalCandidatesFromAllSystemUsers`、`MesTeamLeaderRuntimeConfigServiceTest#shouldLinkFormalUserWithoutStoringSignaturePassword`、`MesTeamLeaderRuntimeConfigServiceTest#shouldCreateTemporaryProductionPersonWithSignaturePasswordHashAndAudit`、`MesTeamLeaderScopeServiceTest#shouldRejectOutOfScopeEmployeeAccess`、`MesFrontlineRuntimeConfigServiceTest#getRuntimeConfig_returnsEnabledLeaderPersonnelProfilesInsteadOfOnlyProcessBindings`、`MesFrontlineProductionEmployeeLeaderProcessScopeTest`。
- Historical evidence: `doc/tasks/20260809-frontline-submit-leader-visibility/verification-report.md`，历史范围迁移后由真实生产组长页面证明正式提交事件可见，未复核记录不进入报工历史。

## MES PQC组长人员范围与管理数据可见性门禁

### PQC管理必须按唯一启用人员范围读取

- Trigger: PQC组长人员管理、PQC管理列表、指定租户管理员看不到一线 PQC 提交、`pqc_permission`、`mes_pro_process_pool_team_leader_scope`、`leader_type=PQC`、`scope_type=EMPLOYEE`。
- Preflight check: 先确认目标提交、实际检验员和查看账号属于同一租户，再核对实际检验员是否具有启用的 `pqc_permission` 角色，以及是否只存在一个启用的 PQC 组长人员范围。PQC管理读模型只按当前登录组长的启用人员范围读取，不因账号是租户管理员或超级管理员而自动全量可见。把检验员调整到另一组长时，必须先通过正式人员管理禁用原范围，再由目标组长通过正式候选搜索和关联页面创建新启用范围。
- Preflight detail: 若 `PQC管理` 页面显示 `No Data` 但怀疑测试数据仍存在，先比较页面请求的 `submitDate` 与 `mes_pro_process_pool_event.server_submit_time`；列表按正式提交事件时间过滤，不按 PQC 任务 `business_date` 推断。恢复任务自有测试 fixture 时，只能按明确任务标识、事件 ID 和记录 ID 精确更新 PQC 事件与对应 PQC 记录的提交时间，不得改实际检验员、人员范围、角色或任务状态来制造可见性。
- Blocker: 目标数据属于正确租户但当前查看账号范围未包含实际检验员、检验员缺 `pqc_permission` 导致正式候选为空、检验员仍被其他 PQC 组长启用占用，或同一检验员存在多个启用组长范围时必须停止；不得把租户一致误判为列表可见。
- Verification: 使用指定租户/账号真实登录，先在人员管理确认目标检验员为已启用，再进入 `PQC管理` 断言目标业务行和分页响应；只读数据库同时核对角色、唯一启用范围、提交事件实际检验员和 tenant_id。角色和人员范围写入必须走真实页面，API/数据库只用于最终只读复核。
- Forbidden action: 禁止给管理员增加全量可见 fallback，禁止直接改事件实际检验员、直接插入人员范围或角色关系，禁止保留多组长同时启用，禁止用另一组长页面或仅租户 ID 证明指定账号可见。
- Evidence: `doc/tasks/20260807-pqc-leader-management-five-records/verification-report.md`；`doc/tasks/20260808-restore-pqc-management-test-data/verification-report.md`。

## 禁止做法

- 禁止跨模块复制业务逻辑来绕过现有服务边界。
- 禁止未核对 schema 就写运行 SQL。
- 禁止捕获异常后静默返回成功、空数据或默认数据。
- 禁止缺少依赖或测试数据时跳过验证并宣称完成。

## 2026-07-25 子表集合替换软删除唯一键门禁

- Trigger: 后端更新父表时先删除再重建子表集合，且子表存在 `case_id + sort`、`parent_id + code`、`tenant_id + key` 等唯一约束，并启用了 MyBatis Plus 逻辑删除。
- Preflight check: 先核对 mapper 删除方式、唯一索引字段、逻辑删除字段是否参与唯一索引；集合替换语义若要求同一唯一键可重建，删除必须释放真实唯一键占用。
- Blocker: 逻辑删除记录仍占用唯一键且后续插入使用相同 key 时，不得用 catch、重试、跳过插入、修改 sort 或前端规避来绕过。
- Verification: 新增或更新后端回归测试，覆盖同一父记录连续两次替换子表集合且第二次使用相同排序或业务键；目标 Maven 测试必须 PASS。
- Forbidden action: 禁止把集合替换失败归因于前端重复提交；禁止为了避开唯一键冲突引入随机排序、默认成功或软失败。
- Evidence: `doc/tasks/20260725-codex-test-method-target-table-rows/verification-report.md`，`CodexTestCaseServiceImplTest#updateCase_allowsRepeatedCheckpointReplacement`。

## 2026-07-27 测试项固定名称删除唯一键门禁

- Trigger: `系统管理 > 测试管理` 使用固定名称反复创建、删除测试项，或表唯一键包含 `tenant_id + name + deleted` 且删除语义需要释放同名占用。
- Preflight check: 修改删除逻辑前先核对唯一索引、逻辑删除字段、运行中 execution 保护和子表清理顺序；若业务要求固定名称可重复闭环，删除必须真实释放同名唯一键占用。
- Blocker: 第二次创建/删除同名测试项触发 `DuplicateKeyException`、删除后仍占用 `deleted=1` 唯一键，或物理删除会绕过运行中执行保护时必须停止。
- Verification: 新增后端回归覆盖同一固定测试项名称连续创建、删除两轮，并复跑测试项管理、执行创建和 Runner 相邻测试。
- Forbidden action: 禁止改成随机名称、吞唯一键异常、前端隐藏删除失败、跳过运行中 execution 校验，或只清子表不释放测试项主表唯一键。
- Evidence: `doc/tasks/20260727-codex-test-node-chain/bug-regression-evidence.md`，`CodexTestCaseServiceImplTest#deleteCase_allowsRepeatedCreateAndDeleteWithSameName`。

## 2026-07-25 Maven Reactor 兄弟模块验证门禁

- Trigger: 多模块 Maven 项目中当前模块依赖兄弟模块，出现缺方法、缺字段、DO/DTO builder 不一致、或测试编译引用 sibling module 新接口时。
- Preflight check: 先确认失败符号所属模块；若符号来自同 reactor 兄弟模块，必须用 `mvn -pl <module> -am ...` 重跑，让 Maven 同时构建依赖模块。
- Blocker: `mvn -pl <module> ...` 因未构建兄弟模块而失败时，不得直接判定为产品代码阻塞；必须复验 `-am` 后再给结论。
- Verification: 任务日志同时记录窄范围失败、`-am` 复验命令、PASS/FAIL 结果和影响模块。
- Forbidden action: 禁止用旧本地产物、跳过编译、API-only、或改 unrelated sibling 代码来掩盖 reactor 构建边界问题。

## 2026-07-27 Windows Maven 增量输出删除卡住门禁

- Trigger: Windows 上目标 Maven 命令长时间无输出，`jcmd <pid> Thread.print` 显示主线程停在 `IncrementalBuildHelper.beforeRebuildExecution` 和 `WinNTFileSystem.delete0`。
- Preflight check: 先确认 Maven PID、父进程、启动命令和是否属于当前任务；检查同仓并发 Maven，但不得停止其他任务进程。
- Blocker: 目标 Maven 超时且未生成 surefire 报告时，不得宣称测试通过；只允许停止当前任务启动的 Maven PID，并记录命令、PID 和诊断栈。
- Verification: 保持项目标准 Maven 参数重新运行目标测试，必须得到明确 `BUILD SUCCESS` 和测试计数；一次关闭增量编译后的全量编译失败不能替代标准参数复验。
- Forbidden action: 禁止强杀所有 Java/Maven 进程、删除其他任务构建产物、用静态检查冒充 JUnit 通过，或把 `-Dmaven.compiler.useIncrementalCompilation=false` 固化为产品构建 fallback。
- Evidence: `doc/tasks/20260727-remove-lfs-assets/verification-report.md`。
## 业务修订审计身份服务端归属门禁

- Trigger: 新增或修改原始记录补正、报工修改、数据修订、重新签名、字段差异日志或其它需要审计身份的业务接口。
- Preflight check: 接口请求只接收业务字段、修改原因和当前用户签名凭据；当前登录人必须由控制器或安全上下文写入内部命令，服务端负责校验业务范围、签名密码、签名 actor、状态门禁、字段差异和修改后 payload。历史业务对象必须按提交时的版本/路线/工序快照修订，不能改绑最新发布版本。
- Blocker: 客户端可以提交或覆盖 `modifiedByUserId`、签名用户、签名 ID、签名快照、`afterPayload` 或 `changedFields`，服务端未校验当前用户业务范围，已审核通过记录仍可修改，或历史记录跟随最新配置漂移时必须停止。
- Verification: 后端合同测试负向断言请求 VO 不含审计身份和派生字段；服务测试覆盖当前用户范围、密码签名、actor 一致性、无变化拒绝、状态锁定、字段差异、快照绑定和受影响业务片段同步；前端真实路径只展示业务字段。
- Forbidden action: 禁止把前端隐藏内部输入框当作服务端安全边界，禁止信任客户端生成的审计身份或 JSON，禁止用最新配置覆盖历史快照，也禁止为兼容旧页面保留双写身份字段。

## 持久化列表相邻手动排序门禁

- Trigger: 业务列表增加上移、下移、置顶、置底或拖拽排序，并要求刷新、重新登录或分页后顺序保持。
- Preflight check: 先冻结正式排序范围和身份边界，例如 `tenantId + ownerUserId`；数据库增加非空正式排序字段并按旧列表的确定性顺序迁移历史数据。写接口只能从安全上下文取得当前操作者/负责人，在事务内锁定当前范围的正式列表，只交换目标记录与相邻记录的排序值；列表读取必须以正式排序字段为第一排序键。前端边界禁用应按完整正式列表判断，写成功后重新读取列表，不能在本地数组中伪造持久化结果。
- Blocker: 正式排序字段缺失、为空或重复，目标记录不属于当前范围或已失效，没有相邻记录，条件更新行数不符合预期，或运行库尚未应用正式迁移时必须明确失败；不得按加入时间、ID、前端数组下标或客户端提交的负责人身份继续写入。
- Verification: BDD/TDD 至少覆盖上移、下移、首末边界、越权/失效记录、并发条件更新失败、新增/重新激活记录进入序列末尾、历史顺序确定性迁移；前端运行聚焦合同和类型检查，真实写入 E2E 仅使用任务自有测试数据并从可见业务行执行操作。
- Forbidden action: 禁止只用 `splice`/`sort` 做前端临时排序，禁止改写加入时间或业务 ID 冒充顺序，禁止边界请求返回默认成功，禁止吞掉并发冲突，禁止在缺迁移时增加兼容旧 schema 的 fallback。
- Evidence: `doc/tasks/20260809-active-order-manual-sorting/verification-report.md`。
