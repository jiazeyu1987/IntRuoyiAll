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

## 第三方报工直报正式链路门禁

### 导入成功必须落到正式报工而不是直接进度

- Trigger: 第三方报工、李萍报工单、直接报工 Excel、`importDirectWorkReportWorkbook`、`DIRECT_WORK_REPORT`、导入结果弹框显示成功但正式报工列表无新增、排产进度疑似未增长、排产员工作台工序列表班次产能为 0。
- Preflight check: 先确认导入成功路径是否创建 `MesProFeedbackDO`、设置 `sourceImportRecordId`、回写导入记录 `feedbackId`、调用正式提交服务，并由正式报工状态参与排产进度汇总；导入后若该任务会被重排保护，还必须核对 `mes_pro_task.workstation_id` 已指向正式报工工作站，且该工作站能解析启用产线；若工作台班次产能为 0，必须按当前路线工序 `process_id` 核对启用未删除工作站、`shift_hours`、工作站设备绑定和 `mes_dv_machinery_process` 小时产能，而不是只按工序编码找旧工作站；手动重排后验证资源落库时，必须通过 `mes_pro_task_schedule_ext.schedule_order_id -> mes_pro_task.workstation_id` 核对实际任务资源，不能只看可能未回写的历史 `mes_pro_schedule_order_process.workstation_id` 快照。
- Blocker: 若缺少报工人、审批人、唯一未完成任务、排产工序剩余数量、正式路线工序快照、受保护任务工作站、工作站产线、当前 `process_id` 工作站、工作站班次小时或设备工序产能，不得写 `progressSourceType=DIRECT_WORK_REPORT` 或直接改进度/班次产能伪造成功；必须返回结构化跳过原因或 fail fast。
- Verification: 后端回归必须同时覆盖匹配行创建/提交正式报工、缺用户跳过、重复导入再次正式报工、超剩余跳过、导入后受保护任务可参与重排；前端静态合同需确认导入确认后刷新正式报工列表并广播受影响排产工单刷新 payload，真实 E2E 需至少覆盖一次第三方报工导入后的手动重排预览或应用；跨环境补工作站数据后必须复验工作台目标工序 `shiftCapacityTotal` 为非 0 且资源链路行数可追溯；重排应用后必须记录排产工单计划时间、`mes_pro_task_schedule_ext` 任务数、空/失效工作站数、覆盖工作站数和最近一次重排快照。
- Forbidden action: 禁止用导入记录直接进度、前端假新增、默认成功、空列表刷新或 API-only 结果替代正式报工持久化链路。
- Evidence: `doc/tasks/20260801-third-party-feedback-import-list-progress/verification-report.md`；`doc/tasks/20260802-test-server-replan-protected-task-workstation/verification-report.md`；`doc/tasks/20260802-test-server-replan-shift-hours-duration/verification-report.md`。

## MES PQC 项目级检验快照门禁

### PQC 检验项目事实必须来自发布规程和结构化 itemResults

- Trigger: PQC 填写、PQC 组长复核、QA 检验规程、检验设备、设备编号、接收标准、检验方法、参数上下限、`itemResults`、`rawPayload.pqcPieceValues`、`pqcItemDetails`、固定 `length/appearance/seal/pressure` 字段。
- Preflight check: 修改 PQC 链路前先核对发布 QA 规程项目、项目级设备表、设备台账编号归属、接收标准上下限、单位和精度字段；提交契约必须以结构化 `itemResults[]` 为业务事实，后端在提交时从发布规程冻结设备、编号、方法、标准、上下限、单位、精度、实测值和判定。
- Blocker: 客户端提交可改写接收标准或检验方法、后端仍把 `rawPayload.pqcPieceValues` 当权威、组长页仍按固定四项字段展示、设备编号未按项目设备归属校验、缺发布规程项目或设备主数据时默认成功，必须停止。
- Verification: 后端回归需覆盖 schema、项目设备 mapper、`itemResults` 提交、设备编号归属校验和明细冻结；前端静态或真实路径需覆盖填写页每项目设备/编号/标准/方法入口、组长页读取 `pqcItemDetails/itemResults`，并复跑相邻 eDHR/PQC 布局合同和 `pnpm ts:check`。
- Forbidden action: 禁止用整单设备替代项目级设备，禁止用固定四项字段、前端文案、默认上下限、空标准、raw payload 或 API-only 展示替代正式项目级快照。
- Evidence: `doc/tasks/20260803-pqc-equipment-standard-method-implementation/verification-report.md`。

### PQC 末检适用性必须有发布规程依据

- Trigger: AC-M15、PQC 末检、末检不适用、QA 规程发布、`finalInspectionApplicable`、`finalInspectionNotApplicableReason`、`FINAL` 检验项目、PQC 任务生成、放行完整性预检。
- Preflight check: 修改末检、QA 规程发布、PQC 任务生成或放行完整性前，必须核对发布版本表、保存/发布 VO、前端 payload、生成器和放行校验是否都读取同一份 `finalInspectionApplicable` 与 `finalInspectionNotApplicableReason`；缺 FINAL 任务只能由“发布规程明确不适用且有非空依据”解释。
- Blocker: 未显式配置末检适用性、末检不适用但缺依据、适用却缺 FINAL 项目、不适用却仍保存 FINAL 项目、生成器因缺任务默认跳过末检、或放行预检无法追溯发布版本依据时必须停止。
- Verification: 后端回归必须覆盖适用生成 FINAL、不适用且有依据跳过 FINAL、未显式配置阻塞、放行不适用通过和缺适用性阻塞；前端静态或真实路径必须覆盖末检关闭时填写正式依据、payload 提交字段、禁用检验类型不序列化为项目；schema 测试需锁定版本表字段。
- Forbidden action: 禁止把缺少 FINAL 任务、空规则列表、前端开关、默认 false、历史任务状态或 API-only 说明当作末检不适用依据；禁止用 fallback 默认放行掩盖发布规程缺字段。
- Evidence: `doc/tasks/20260805-pqc-regulation-task-generation-fix/verification-report.md`。

### PQC 过程检验汇集必须形成最终确认明细

- Trigger: AC-M21、过程检验记录汇集、PQC 组长复核通过、`aggregateApprovedPqcSubmission`、`processInspectionAggregationStatus`、`mes_pqc_process_inspection_aggregate_detail`、`mes_pqc_inspection_task.task_status`。
- Preflight check: 修改 PQC 汇集链路前先核对 `mes_pro_process_pool_event`、`mes_pro_process_pool_pqc_record`、`mes_pqc_inspection_task`、`mes_pqc_inspection_piece_detail` 和汇集明细表的租户、事件、任务、轮次、规程版本、逐件明细来源；汇集只能读取正式 `SUBMITTED` 任务和结构化逐件明细，并在同一事务中 CAS 标记记录已汇集、确认任务为 `CONFIRMED`、写入结构化汇集明细。
- Blocker: 只能证明状态标记而没有结构化明细、仍从 raw payload 汇集、未校验租户/事件/任务一致性、未排除旧修订/未确认任务/重复汇集、或任务确认与明细插入不在同一事务时必须停止。
- Verification: 后端回归必须覆盖成功汇集明细字段、重复汇集 CAS、跨租户拒绝、无逐件明细拒绝、任务确认 CAS 失败回滚，并配合 schema 测试验证唯一键 `tenant_id + event_id + source_piece_detail_id + deleted`。
- Forbidden action: 禁止用前端展示、状态字段、默认空明细、raw payload、API-only 截图或吞唯一键异常替代正式结构化汇集事实。
- Evidence: `doc/tasks/20260805-ac-m21-process-inspection-aggregation-fix/verification-report.md`。

### QA 规程配置状态必须来自产品级规程记录

- Trigger: QA 规程配置页、DCC 项目代码对应产品、`已配置 QA 规程`、`待配置 QA 规程`、`project-statuses`、`mes_qa_inspection_regulation.product_id`、前端硬编码 `IDI` 或压力泵模板判断配置状态。
- Preflight check: 修改 QA 规程配置状态前，先核对 DCC 项目代码的 `productMasterId` 与 QA 规程表 `product_id` 的正式关系；配置状态必须由后端按产品 ID 查询 QA 规程记录并返回，前端只能展示和错误处理，不得用项目代码、产品名称或样例模板集合推断已配置。
- Blocker: 页面把压力泵 `IDI`、产品名称、前端常量集合、空状态、模板初始化数据或查询失败当作配置状态来源，或状态接口失败时静默把项目归入待配置，必须停止并补齐正式状态接口和错误展示。
- Verification: 后端回归必须覆盖已配置与未配置产品按请求顺序返回；前端静态契约必须断言调用正式 `project-statuses` API、禁止硬编码配置集合，并覆盖状态加载失败可见错误；同时运行 `pnpm ts:check`。
- Forbidden action: 禁止用前端文案、默认项目、压力泵样例模板、API-only 展示或吞掉状态接口错误替代后台 QA 规程配置事实。
- Evidence: `doc/tasks/20260804-qa-regulation-dcc-project-code/verification-report.md`。

## MES 工艺路线产品绑定状态门禁

### 产品侧路线选择必须匹配后端可维护状态

- Trigger: MES 物料产品选择工艺路线、产品侧路线下拉、`getRouteSimpleList`、`item-binding-list`、`saveRouteProductByItem`、`validateRouteNotEnable`、已启用路线不可维护。
- Preflight check: 修改产品侧路线选择或 route-product 保存前，先核对下拉数据源返回的路线状态集合和后端维护校验是否一致；若后端禁止维护已启用路线，前端不能使用只返回已启用路线的精简列表作为可选项。
- Blocker: 下拉只提供已启用路线但保存接口会因 `PRO_ROUTE_IS_ENABLE` 失败、已启用当前绑定允许清空或改选、产品侧新增第二套路由字段、或用前端隐藏错误替代后端 fail-fast 时必须停止。
- Verification: 前端静态契约必须断言产品侧使用专用路线选择接口、禁用已启用路线选项、不调用只返回已启用路线的 `simple-list`；后端回归必须覆盖创建、迁移、解除绑定和旧路线产品 BOM 清理。
- Forbidden action: 禁止为了让产品能选择路线而放宽 `validateRouteNotEnable`、禁用后端校验、使用 `MdItemApi.routeId` 第二关系源、默认成功、吞掉保存错误或混入表单槽位/批记录表单链路。
- Evidence: `doc/tasks/20260804-mes-item-route-selection/verification-report.md`。

## MES 生产人员档案正式工重复关联门禁

### 同一组长正式工关联必须先业务拒绝再写库

- Trigger: 生产人员档案、班组员工、正式工搜索关联、临时工/正式工统一候选、`mes_pro_process_pool_team_employee_profile`、`system_user_id`、`employee_code=USER-<id>`、DuplicateKeyException、重复关联返回 500。
- Preflight check: 新增正式工关联前必须按当前 `leaderUserId + systemUserId` 查询现有未删除生产人员档案，并区分“已禁用可启用既有档案”和“从未关联可新增”；显示名唯一校验不能替代正式用户唯一关联校验。
- Blocker: 重复正式工关联落到数据库唯一键异常、接口返回 500、禁用旧档案后再次新增同一系统用户、或只靠前端禁用按钮阻止重复时必须停止并补后端 RED/GREEN。
- Verification: 后端回归必须覆盖重复正式工在 `employeeProfileMapper.insert` 前抛业务错误，且成功正式工路径仍不保存签名密码；真实 E2E 重跑时使用新的任务自有正式工候选或先明确启用既有档案。
- Forbidden action: 禁止 catch DuplicateKeyException 后返回默认成功，禁止创建重复正式工档案，禁止把正式工重复关联伪装成显示名重名，禁止让前端过滤全系统用户列表替代后端 scoped 候选。
- Evidence: `doc/tasks/20260805-production-personnel-management/verification-report.md`，目标测试 `MesTeamLeaderRuntimeConfigServiceTest#shouldRejectDuplicateFormalUserBeforeDatabaseInsert`。

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
