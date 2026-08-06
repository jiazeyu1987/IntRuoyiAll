# Execution Log

## User Intent

- 用户要求继续推进 AC-M04：从当前系统分析已做到哪一步，并继续执行下一步。
- 用户随后要求“进行修复”；本轮按旧历史 blocker `activeOrderTransferTraceReadOnly / E2E_TRANSFER_TRACE_DATA` 复核当前系统是否仍存在 AC-M04 调拨追溯链路缺口。
- 用户明确回复“授权修复本机库 P0 backfill”；授权范围限定为本机 Docker MySQL 运行库，不包含任何远端测试服/正式服/备用服。

## BDD / TDD Notes

- BDD: AC-M04 验收产物同步 -> Given 最新报告显示 AC-M04 加入、冲突、跨角色只读、错误角色拒绝、最终清理和并发门禁已有 PASS/GREEN；When 检查当前 E2E 结果产物；Then 结果产物不得继续保留旧 `activeOrderCleanupDeferred` 作为当前 blocker，且必须准确保留未 `ACCEPTED` 的原因。
- BDD: AC-M04 调拨追溯修复复核 -> Given 生产班组长在加入活跃订单时提供正式 `transferIds`；When 前端提交加入动作且后端创建、重复加入或并发返回同一活跃订单；Then 同一 `activeOrderId` 必须记录正式调拨追溯并通过只读接口/页面暴露，不能用旧结果产物或空数据冒充完成。
- BDD: RRM 本机前置补齐 -> Given 用户要求由 Agent 添加缺失 RRM 前置且本机 `8081/48081` 运行态可用；When 注入 `RRM_*` 并运行 `real:check`；Then 六角色账号必须真实登录、业务 ID 必须指向当前正式数据、`real:check` 不得再返回 ENV/SOURCE/RUNTIME blocker，且密码和签名 JSON 不写入文档或提交。
- BDD: AC-M04 full real E2E 刷新 -> Given `real:check` 已 PASS 且生产班组长加入活跃订单返回同一 `activeOrderId`；When 运行 full real E2E；Then AC-M04 必须证明加入、冲突路线拒绝、跨角色只读、调拨追溯只读和最终清理均为 PASS，剩余非 AC-M04 coverage 或后续 PQC/eDHR 阻塞必须结构化记录，不能混写成 AC-M04 已 ACCEPTED。
- BDD: PQC 正式提交 RRM 前置 -> Given PQC 页面提交必须携带本轮新建的 `productionSubmitEventId`；When RRM full real E2E 进入 PQC 提交动作；Then 脚本必须先通过真实一线生产填写页 POST `/mes/pro/feedback/frontline/submit` 捕获新的 `processPoolEventId`，再把同一 ID 作为 `productionSubmitEventId/processPoolEventId` 打开 PQC 页面，禁止使用历史事件 ID 或环境变量硬塞成功。
- BDD: P0 runtime schema 正式迁移前置 -> Given 本机真实生产填写提交已经到达后端但运行库缺 P0 idempotency 字段；When 准备应用 `20260803_mes_process_pool_event_idempotency.sql`；Then 必须先通过只读 schema/source/preflight gate 证明历史数据可以正式 backfill，不能用空值、随机幂等键、旧事件 ID、删除历史测试行或部分迁移冒充完成。
- BDD: 授权后本机 P0 backfill 修复 -> Given 用户授权修复本机库且备份、rollback、逐行 manifest 已生成；When 执行本机 backfill 和正式迁移；Then P0 runtime preflight/source/runtime verifier 必须 PASS，且修复范围不得越过授权的本机库。
- BDD: 生产组长正式模块入口 -> Given 当前系统已提供 `/mes/pro/process-pool/production-leader` 正式生产组长页面并通过模块页签承载报工管理与班组配置；When full real E2E 验证生产组长阶段；Then 必须进入正式生产组长页面、切换真实模块页签并验证对应表面，不得继续依赖旧 `/mes/pro/process-pool/team-leader` 页面结构或因选择器超时原始崩溃。
- BDD: 正式模块页签异步挂载 -> Given 正式生产组长/PQC 组长页面在路由完成后异步挂载模块页签；When E2E 准备切换目标页签；Then 必须等待目标 `.el-tabs__item` 可见后再点击，不得在首次计数为 0 时提前返回并继续等待未激活模块的选择器。
- BDD: 生产组长模块表面归属 -> Given 当前正式生产组长页面将报工工作台、日结看板和活跃订单配置分别放在“报工管理”“看板”“班组配置”页签；When full real E2E 验证阶段表面和日结证据；Then 必须切换到对应页签后断言，不得在“报工管理”页签等待只属于“看板”的日结组件。
- BDD: 活跃订单最终清理页签 -> Given full real E2E 最终清理重新进入正式生产组长页面；When 定位并移出本轮 activeOrder；Then 必须先切换到“班组配置”页签再读取活跃订单配置表面，确保清理闭环不会因默认“人员管理”页签而原始超时。
- BDD: PQC 逐件合格按钮精确定位 -> Given 逐件检验弹窗同时包含“合格”和“不合格”按钮；When E2E 准备全部合格样本；Then 只能点击 `.pass` 合格按钮，不得使用包含文本匹配同时命中“不合格”，否则提交会被不良说明必填规则同步阻断。
- BDD: PQC 正式提交继承生产事件设备上下文 -> Given 本轮真实生产填写已经返回唯一 `productionSubmitEventId`，且该事件冻结了设备账号、设备和工作站；When PQC 检验员提交同一订单、路线和工序的检验结果；Then 后端必须按该事件 ID 读取并校验正式设备上下文，PQC 前端不得从当前登录人、路线工序默认工作站或页面设备状态猜测这些字段。
- BDD: PQC 拒绝错误生产事件根 -> Given PQC 提交携带的 `productionSubmitEventId` 不是 `PRODUCTION_SUBMIT`，或事件订单/路线/工序与 PQC 任务不一致；When 后端处理提交；Then 必须在更新 PQC 任务、写逐件明细和创建 PQC 事件之前 fail fast，禁止使用活跃池最新事件或客户端字段绕过。
- BDD: RRM 主工序运行态前置 -> Given 本机 RRM 正式主路线工序为 `928609 / 922985 / 980010`，班组设备 `41` 和员工档案 `980022 / user 964` 已启用且归属生产组长 `1520`；When 生产员工通过真实页面读取该工序运行态配置；Then 必须由同一生产组长的 PROCESS、WORKSTATION、工序设备和工序员工四条正式绑定共同提供上下文，不能从相邻工序、默认设备、登录人或前端字段推断。
- BDD: RRM 主工序夹具写入安全 -> Given 用户只授权本机 `ruoyi-vue-pro` 且目标四条语义绑定均不存在；When 执行修复 SQL；Then SQL 必须在一个事务内断言路线工序、设备、员工档案、相邻夹具、候选主键和语义键，精确插入四行并可按 creator/updater/remark 全字段条件回滚，禁止覆盖或复用其它任务数据。
- BDD: RRM 主工序生产任务前置 -> Given PQC source event 必须先通过真实生产填写页提交，且生产任务分页按 `workOrderId=980008 / routeId=922119 / processId=922985` 精确查询；When 解析生产填写上下文；Then 必须返回一条正式、启用、同租户且物料/工作站匹配的 `mes_pro_task`，不能复用相邻工序任务、默认任务或历史事件绕过。
- BDD: PQC 正式项目设备选择与工序同源 -> Given 本轮生产提交事件冻结主工序 `routeProcessId/processId`，且发布 QA 规程为每个检验项目提供正式设备及设备编号；When RRM E2E 重新打开 PQC 页面并逐项填写后提交；Then URL 必须携带同一主工序身份，每个 QA 项目必须真实选择非占位设备和对应设备编号，禁止依赖页面默认工序或空项目级设备选择。
- BDD: RRM 临时密码恢复 -> Given 临时登录前置修改了七个本机 RRM 测试账号且恢复标志未及时建立；When 继续任何写入型 E2E；Then 必须先从本机 binlog 对应更新事件的 WHERE-side 精确恢复七行密码、更新人和更新时间，并证明恢复行数为 7、临时值残留为 0，禁止猜测旧密码、复制其它账号 hash 或只改更新时间。
- BDD: 多笔生产 source event 签名唯一性 -> Given 一轮 full real E2E 会为正式提交、自我复核负向候选和退回补正候选分别创建生产提交事件；When 每笔事件进入后端唯一签名校验；Then 脚本必须从显式 `productionEmployee/productionExtra*` 池逐次保留不同签名 ID，禁止反复使用同一个 `productionEmployee` ID 或生成随机 fallback ID。
- BDD: PQC 待检工序与生产 source event 同源 -> Given PQC 页面本轮从正式 processes 响应选择了仍待检的工序；When E2E 为该 PQC 提交准备真实生产 source event；Then 必须使用 `pqcRegulationItemsRendered` 冻结的同一 `routeProcessId/processId/pqcTaskId` 查询生产运行态和任务，禁止继续固定 `RRM_ROUTE_PROCESS_ID_1` 或回落到主工序。
- BDD: 本机 RRM 包装输出可审计 -> Given 安全包装必须顺序运行 `real:check` 和 full real E2E；When 包装调用 pnpm 子命令；Then 子命令 stdout/stderr 必须实时显示，且包装必须在管道结束后立即保存并返回该 pnpm 进程退出码，禁止把输出数组误当作函数返回值或由后续命令覆盖退出码。
- BDD: PQC 组长只看正式负责员工提交 -> Given 本轮 PQC 事件 `133` 的实际检验人是 `914524`，且组长看板按 `PQC + EMPLOYEE` scope 过滤 `actual_employee_id`；When PQC 组长 `512` 查询提交看板；Then 只有在存在正式 `512 -> 914524` scope 时同一 `pqcTaskId=93 + signatureId=99009104` 才可见，禁止移除范围过滤或扩大为全租户可见。
- BDD: PQC 本轮选中工序生产任务前置 -> Given PQC 页面冻结的待检任务为 `68 / routeProcess=928611 / process=922987`，且该路线工序正式工作站为 `980009`；When E2E 为同一工序准备真实生产提交 source event；Then `mes_pro_task` 必须返回同一 `workOrder=980008 / route=922119 / process=922987 / workstation=980009` 的正式任务，禁止复用 `922985` 或 `922986` 相邻任务。
- BDD: PQC 本轮工序任务写入安全 -> Given 用户只授权本机 Docker MySQL 且目标任务语义键、候选主键和编码均不存在；When 执行任务补齐 SQL；Then 必须先备份精确范围，在事务内断言 PQC 任务、工单、路线工序、工序、工作站、物料和相邻任务来源，精确插入一行并保留依赖检查 rollback，禁止修改任何远端环境。
- BDD: PQC 组长复核签名上下文 -> Given 后端对正确、不正确、重复终态和自我复核请求都先校验 `reviewSignatureId + reviewSignatureEmployeeUserId + reviewSignatureSnapshotJson`；When 真实 E2E 执行任一复核路径；Then 必须使用 `RRM_SIGNATURE_IDS_JSON.pqcLeader`、当前 PQC 组长用户 ID 和合法 JSON 快照，禁止靠空字段触发错误后误判成目标业务守卫。
- BDD: PQC 复核弹窗闭环 -> Given 页面复核成功后 `reviewVisible=false`，失败时弹窗保持打开；When E2E 点击“提交复核”；Then 成功必须等待弹窗隐藏后再操作筛选，失败必须先记录结构化 blocker 并关闭当前复核弹窗，禁止使用 force click 绕过遮罩。
- BDD: PQC 复核 scope 本机修复安全 -> Given 用户仅授权本机 Docker MySQL 且目标语义 scope 不存在；When 写入 `512/PQC/EMPLOYEE/914524`；Then 必须先核对用户、既有 scope、事件、PQC 记录、候选主键和语义键，在一个事务中精确插入一行，并提供按全字段删除的 rollback。
- BDD: AC-M21 汇集表运行态闭合 -> Given AC-M20 已先创建过程检验汇集表、AC-M21 的 `CREATE TABLE IF NOT EXISTS` 不会修改既有表；When PQC 组长批准本轮提交并批量写汇集明细；Then 正式追加迁移必须幂等补齐 `active_order_id`、`route_version_id`、`actual_inspection_quantity`，从同租户正式 PQC 任务回填缺失值，缺正式来源时 fail fast，并补齐标准唯一键和查询索引。
- 本轮优先做产物一致性和静态/JSON 校验；若发现真实脚本或源码缺口，再按 RED/GREEN 进入实现。

## Command Intent

- 已读取任务、E2E、前端、登录、运行态、worktree、PowerShell 编码和经验索引门禁。
- 已核对 package scripts、真实 E2E 脚本、result.json 与最新报告的一致性。
- 已使用 OfficeCLI 只读核对 `C:\Users\BJB110\Desktop\3\岗位需求分解矩阵.xlsx`，确认第 8 行 AC-M04 原始要求。
- 已复核当前源码链路：E2E 填写 `RRM_TRANSFER_IDS` / `调拨单ID列表`，前端 `TeamLeaderWorkbenchPage.vue` 解析并提交 `transferIds`，后端 `MesTeamLeaderActiveOrderServiceImpl` 在新建、重复和并发路径调用 `recordTransferTracesIfRequested`，`MesActiveOrderTransferTraceServiceImpl` 从正式调拨单/行/明细投影追溯数据。

## Milestone Updates

- completed：任务文档已建立。
- completed：`test-report.md`、`verification-report.md`、`task-state.json` 与真实 E2E 脚本均显示 AC-M04 已有 action/gate 通过证据，但未达到 `ACCEPTED`。
- completed：当前磁盘 `result.json` 已是本轮缺环境 `real:check` 生成的 ENV blocker-only 产物，不能代表 canonical full real E2E。
- completed：只读检查历史独立 worktree `D:\IntRuoyiWorktree\rrm-m0-m6-verification-20260803`，其 `result.json` 是真实 full E2E 产物，但状态为 21 action / 63 blockers，额外包含 `activeOrderTransferTraceReadOnly / E2E_TRANSFER_TRACE_DATA`，不是当前主任务报告里的 20 action / 62 `E2E_COVERAGE` canonical 状态，不能直接复制覆盖主工作区。
- completed：当前代码层 AC-M04 transfer trace source contract PASS；未发现需要修改生产代码的当前缺口。
- completed：等待主工作区并发 Maven 进程释放后，AC-M04/调拨边界目标 JUnit 已复跑通过，获得新的 `BUILD SUCCESS`。
- completed：角色矩阵大静态前置失败根因为 AC-M19 静态合同仍匹配旧幂等键；已将断言同步到当前正式 `PROCESS_POOL_REPORT_BACKFILL_AGG:...` 聚合键，复跑 PASS。
- blocked：缺少 `RRM_*` 真实 E2E 环境变量，无法刷新 full real E2E 产物或安全同步 `result.json`。
- completed：本机 RRM 前置已补齐；七个 RRM 角色账号在测试租户可登录，`real:check` 已恢复 PASS。
- completed：full real E2E 已刷新为 `mode=real` 产物；AC-M04 相关 `joinActiveOrder`、`activeOrderConflictRouteRejected`、`activeOrderCrossRoleReadOnly`、`activeOrderTransferTraceReadOnly`、`activeOrderCleanupCompleted` 均为 PASS。
- blocked：full real E2E 整体仍 `BLOCKED`，剩余 74 个 blocker：2 个 `E2E_PQC_SUBMISSION_UI`、1 个 `E2E_PQC_SUBMISSION_DATA`、1 个 `E2E_PQC_DETAIL_DATA`、1 个 `E2E_PQC_DETAIL_PERMISSION`、1 个 `E2E_PQC_REVIEW_DATA`、1 个 `E2E_PQC_REVIEW_TERMINAL`、1 个 `E2E_PQC_REVIEW_SELF`、1 个 `E2E_PQC_AGGREGATION_READONLY`、1 个 `E2E_RELEASE_TRACEABILITY_PREP`、1 个 `E2E_CONCURRENCY`、1 个 `E2E_PERFORMANCE`、62 个 `E2E_COVERAGE`。
- in_progress：正在补齐 RRM PQC 正式提交前置，目标是复用真实生产填写页生成本轮 `processPoolEventId`，再进入 PQC 页面提交；当前先新增静态合同 RED，随后实施最小脚本修复。
- blocked：PQC 正式提交前端禁用态已解除，真实提交进入后端后被运行库 schema 阻塞；`mes_pro_process_pool_event` 缺 `event_idempotency_key` / `recordbook_entry_id`，且完整 P0 runtime migration 预检显示 88 行历史 backfill blocker，当前结构化来源无法唯一推导，未获业务/DBA 授权和逐行 manifest 前不得写库修复。
- completed：用户已授权本机库 P0 backfill；已完成备份、rollback、manifest、最小 DB 修复和 P0 runtime verifier 复验。仍禁止远端操作和无 manifest 写入。
- in_progress：P0 runtime schema/backfill blocker 已解除，下一步重跑 RRM `real:check` 与 full real E2E，确认 PQC 正式提交是否继续前进。
- in_progress：PQC 正式提交已返回 `taskId=93` 并落库为事件 `133`、PQC 记录 `90`；当前首个 blocker 已收敛为 PQC 组长 `512` 缺少实际检验人 `914524` 的正式 `EMPLOYEE` scope。
- in_progress：P0 修复后 `real:check` PASS；full real E2E 在生产组长阶段等待旧 `/team-leader` 页面选择器时原始超时，当前按正式 `/production-leader` 页面补 RED/GREEN。
- RED: 只读 Playwright 探针 -> 初次读取正式生产组长页时模块页签容器数量为 0；约 1.5 秒后页签出现并可切换到报工管理，证明 `selectRealFlowTab` 的立即 `count() === 0` 返回存在异步挂载竞态。
- RED: full real E2E -> 在正式“报工管理”页签已显示报工工作台后，等待 `[data-role-matrix-daily-close]` 超时；源码确认日结组件由 `showProductionDashboardModule` 控制，正式归属是“看板”页签。
- RED: full real E2E -> 阶段串已执行到最终清理，但 `verifyActiveOrderCleanupTraceability` 进入 `/production-leader` 后直接等待 `[data-team-leader-active-order-config]`，因默认“人员管理”页签未切换而超时。
- RED: PQC 提交根因 -> `fillVisiblePqcPieceModalValues` 使用 `{ hasText: '合格' }` 定位逐件按钮，而“不合格”包含“合格”，导致每行先点合格再点不合格；`validatePqcDefectDescription` 随后因未填写不良说明在模板校验请求前同步抛错。
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` -> BLOCKED；最新 `result.json` 的生产填写来源阻塞为 `生产填写运行态配置 失败：班组长工作台缺少负责范围上下文：frontline runtime deviceId=41`。
- RED: local MySQL read-only fixture probe -> FAIL expected four active semantic bindings, observed `PROCESS(922985)=0`、`WORKSTATION(980010)=0`、`process-device(922985,41)=0`、`employee-binding(922985,980022,964)=0`；候选主键 `980039/980040/15/21` 冲突数均为 `0`。
- RED: first `rrm-primary-process-runtime-prereq-apply.sql` execution -> FAIL with MySQL `ERROR 1267 Illegal mix of collations` during post-verification. Follow-up probe found the four inserts persisted because `CREATE PROCEDURE` implicitly ended the initial transaction and CALL ran under autocommit; the result is not accepted. Required correction: exact rollback, procedure DDL outside the transaction, CALL inside a new transaction, and `v_display_name` explicitly `utf8mb4_unicode_ci`.

## Verification Evidence

- XLSX: `officecli view "C:\Users\BJB110\Desktop\3\岗位需求分解矩阵.xlsx" text --max-lines 80` -> PASS；第 8 行要求候选订单加入活跃订单池后出现在活跃订单列表、PQC 任务来源和报工分配候选。
- SOURCE_SCAN: `rg` 核对 `test-report.md`、`verification-report.md`、`task-state.json` -> PASS；canonical 当前证据为 `activeOrderCleanupCompleted=PASS`、`m6ConcurrencyGateVerified=PASS`、`m6PerformanceGateVerified=PASS`，剩余 blocker 为 62 个 `E2E_COVERAGE`。
- SCRIPT_SCAN: `rg` 核对 `role-requirement-matrix-real-flow.e2e.js` -> PASS；脚本包含 `verifyActiveOrderCleanupTraceability`、`runFinalActiveOrderCleanup` 和 `activeOrderCleanupCompleted`，未命中旧 `activeOrderCleanupDeferred`。
- STATIC: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS。
- SYNTAX: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- SCRIPT_ENTRY: package scripts 存在 `e2e:role-requirement-matrix:preflight:static`、`e2e:role-requirement-matrix:real:check`、`e2e:role-requirement-matrix:real`。
- ENV_CHECK: `Get-ChildItem Env:RRM_*` -> `NO_RRM_ENV_NAMES`；未输出任何密码或 secret 值。
- REAL_CHECK: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> BLOCKED，35 个 ENV blocker；当前 shell 缺 `RRM_FRONTEND_URL`、`RRM_BACKEND_URL`、角色账号标签、签名 ID、生产订单、路线、工序、调拨和 QA 规程等真实 E2E 前置。
- ARTIFACT: 当前 `IntRuoyiFronted\test-results\role-requirement-matrix-real-flow\result.json` parse -> `status=BLOCKED`、`mode=check`、`blockers=35`、`categories={ENV:35}`，没有 action/gate evidence。
- WORKTREE_ARTIFACT: `D:\IntRuoyiWorktree\rrm-m0-m6-verification-20260803\IntRuoyiFronted\test-results\role-requirement-matrix-real-flow\result.json` parse -> `status=BLOCKED`、`mode=real`、`phaseEvidence=6`、`actionEvidence=21`、`gateEvidence=2`、`blockers=63`、`categories={E2E_TRANSFER_TRACE_DATA:1,E2E_COVERAGE:62}`，其中 `joinActiveOrder`、`activeOrderConflictRouteRejected`、`activeOrderCrossRoleReadOnly`、`activeOrderCleanupCompleted` 为 PASS，但 `activeOrderTransferTraceReadOnly` 为 BLOCKED。
- WORKTREE_DECISION: 历史 worktree 产物可以证明 AC-M04 清理和门禁已通过，但多了 transfer trace blocker，且与当前主任务 canonical 62-blocker 状态不一致；按 no-fallback 规则，不复制该文件覆盖主工作区 `result.json`。
- SOURCE_CONTRACT_ACM04: inline Node source contract -> PASS，断言 E2E 写入 `config.transferIds`、调用 `verifyActiveOrderTransferTraceReadOnly`，前端 API/页面包含 `transferIds` 与只读追溯接口，后端加入路径调用 `recordTransferTracesForActiveOrder`，并存在 `shouldRecordFormalTransferTraceWhenAddingActiveOrderWithTransferIds` 与 `shouldProjectFormalTransferDetailsForActiveOrderTransferIds` 回归。
- MAVEN_PROCESS_SAFETY: 发现主工作区并发 Maven 进程 PID 49984 / 7500；先用 `jcmd <pid> Thread.print` 只读确认仍在 javac/Lombok 编译，未强杀，等待释放后再跑本任务目标测试。
- STATIC_BROAD_RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> FAIL，首个失败为 AC-M19 batch-record backfill 静态断言仍期待旧 `PROCESS_POOL_REPORT_BACKFILL:1001:9001:5001`。
- STATIC_FIX: 更新 `IntRuoyiFronted\tests\e2e\role-requirement-matrix-preflight-static.spec.cjs`，将 AC-M19 断言对齐当前正式聚合幂等键 `PROCESS_POOL_REPORT_BACKFILL_AGG:9001:5001:6001:agg-single-1001-7101`；不修改 AC-M04 生产代码。
- STATIC_BROAD_GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS，输出 `PASS role-requirement-matrix preflight static contract`。
- SYNTAX_RECHECK: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js; node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-preflight-static.spec.cjs` -> PASS。
- MAVEN_TARGET_GREEN: `mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesWmTransferManualWriteControllerTest,MesActiveOrderTransferTraceServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> BUILD SUCCESS；`MesWmTransferManualWriteControllerTest` 3、`MesActiveOrderTransferTraceServiceTest` 4、`MesTeamLeaderActiveOrderServiceTest` 14，合计 21 tests / 0 failures / 0 errors / 0 skipped。
- TRANSFER_READONLY_STATIC: `node IntRuoyiFronted\tests\e2e\mes-wm-transfer-readonly-static.spec.cjs` -> PASS，输出 `PASS: MES transfer page is read-only for manual write operations`。
- CONTINUE_CHECK_2026_08_05_1423: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:preflight:static` -> PASS；`pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> BLOCKED，仍为 35 个 `ENV` blocker；`result.json` 保持 `status=BLOCKED`、`mode=check`、`categories={ENV:35}`。
- RRM_RUNTIME_PROBE_2026_08_05: 主运行态 `8081/48081` 可用，旧 RRM slot `8098/48098` 已不监听；当前将按合法 `int_main` URL 注入 `RRM_FRONTEND_URL=http://127.0.0.1:8081` 与 `RRM_BACKEND_URL=http://127.0.0.1:48081`。
- RED: 本机脱敏登录探针 -> FAIL，`liuyueyue`、`lvyujie`、`sunxiaoqing`、`shangmengying`、`huzonggang`、`zhengxiaofang`、`aoteman` 均返回业务失败码，`admin` 可登录；预期原因是历史 RRM 角色账号密码前置未在当前运行库可用。
- EXPERIENCE_REFRESH: 已按 `project-experience-consolidation` 检索现有经验归宿；本轮教训已被 `docs\e2e-rules.md` 的真实 E2E/result artifact 隔离门禁、`docs\frontend-development.md` 的前端静态契约隔离门禁、`docs\backend-development.md` 的 Windows Maven 目标测试阻塞门禁覆盖，不新建长期经验文档。
- COMMAND_NOTE: 首次尝试列出 worktree 候选 env/rrm 文件时 PowerShell regex 过滤写法错误，产生 `Invalid pattern`；随后改用字符串 `Contains(...)` 过滤，只列文件路径，不读取或输出任何 `.env` 内容。
- EXPERIENCE: 已读取 `project-experience-consolidation`；本轮没有新增长期经验文档，原因是相关经验已由现有 `docs\e2e-rules.md` 的真实 E2E / result artifact 隔离门禁覆盖，且当前该规则文件存在非本任务脏改动，不触碰无关文件。
- ACCOUNT_FIX_2026_08_05: 已在本机授权测试租户中修复 `liuyueyue`、`lvyujie`、`sunxiaoqing`、`shangmengying`、`huzonggang`、`zhengxiaofang`、`aoteman` 七个 RRM 角色账号登录前置；未记录明文密码。
- GREEN: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real:check` -> PASS，输出 `PASS role-requirement-matrix real E2E preflight`。
- RED: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` -> FAIL，首次失败在 `performActiveOrderJoin`，原因是脚本捕获到早期列表刷新响应后直接断言，未重读最终活跃订单列表。
- GREEN: 已同步真实 E2E 脚本和静态合同，加入活跃订单成功后如列表响应未包含同一 `activeOrderId/workOrderId`，立即通过登录态只读接口重读最终列表再断言；`node --check` 与 `preflight:static` PASS。
- RED: full real E2E 继续执行后在 PQC 页面规程元信息断言失败，原因是页面实际把判定类型放在 `data-pqc-inspection-meta`，把接收标准和检验方法放在相邻可见卡片，旧断言错误要求三者都出现在同一 meta 文本中。
- GREEN: 已同步真实 E2E 断言与静态合同，分别从 `data-pqc-inspection-meta`、`data-pqc-standard-button`、`data-pqc-method-button` 验证正式 QA 规程项目；`node --check` 与 `preflight:static` PASS。
- RED: full real E2E 继续执行后在 PQC 逐件弹窗等待失败，原因是脚本点击了检验项目卡片本身，当前页面只有“逐件选择/填写”按钮触发弹窗。
- GREEN: 已为 PQC 逐件按钮增加稳定 `data-pqc-piece-open-button`，并让 E2E 按 QA 项目 Tab 逐个点击该按钮完成逐件明细；`node --check` 与 `preflight:static` PASS。
- FULL_REAL_E2E_2026_08_05: `pnpm --dir IntRuoyiFronted e2e:role-requirement-matrix:real` -> BLOCKED，`result.json` 为 `status=BLOCKED`、`mode=real`、`phaseEvidence=6`、`actionEvidence=22`、`gateEvidence=2`、`blockers=74`；AC-M04 关键动作 PASS，整体阻塞集中在 PQC 正式提交未发出提交响应、PQC 组长提交夹具不足、eDHR 放行准备下拉未定位目标路线、并发/性能/coverage 准出。
- EXPERIENCE_REFRESH_2026_08_05: 已读取 `project-experience-consolidation` 并检索 `docs\experience-index.md`、`docs\e2e-rules.md`、`docs\frontend-development.md`、`docs\login-access.md`；本轮经验已由真实 E2E 主链路与扩展诊断隔离、静态合同与真实 E2E 同步、前端静态契约隔离等现有门禁覆盖，不新建长期经验文档。
- RED: schema probe for `mes_pro_process_pool_event.event_idempotency_key` -> FAIL，当前本机库缺列，真实生产填写提交进入 `/admin-api/mes/pro/feedback/frontline/submit` 后触发 `Unknown column 'event_idempotency_key' in 'field list'`。
- RED: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_migration_apply_preflight.py` -> BLOCKED；本机运行库存在 79 行 `mes_pro_process_pool_pqc_record.production_submit_event_id`、2 行 `mes_pro_process_pool_event.event_idempotency_key`、2 行 `mes_pro_process_pool_event.recordbook_entry_id`、5 行 `mes_pro_process_pool_quantity_fragment.production_submit_event_id` 正式 backfill 前置缺口。
- RED: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_sources.py` -> BLOCKED；source audit 显示 79 行 PQC、2 行事件幂等键、2 行事件记录本 entry、5 行 quantity fragment 当前无法从唯一正式结构化来源推导。
- RED: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_repair_plan.py` -> BLOCKED；repair plan gate 明确当前读-only，不允许 DB 写入，需业务/DBA 授权、备份、rollback、逐行 repair manifest 和 dry-run 后才能处理历史 backfill。
- DATABASE_EVIDENCE_VALIDATOR: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence E:\IntRuoyi\doc\tasks\20260805-ac-m04-acceptance-sync\database-schema-evidence.md` -> PASS，输出 `Database schema evidence is valid.`
- DOC_UTF8_CHECK: task/execution-log/verification-report/database-schema-evidence 均可按 UTF-8 读取；未记录任何密码或 token。
- AUTH: 用户明确回复“授权修复本机库 P0 backfill”；授权范围限定为本机 Docker MySQL `127.0.0.2:23306/ruoyi-vue-pro`，未授权测试服/正式服/备用服。
- BACKUP: `db-backup/acm04-p0-backfill-extended-20260805-203724.sql` -> SHA256 `317BD20FD77F473327B5DAAAEAC5C4A51D474958A9B32A7D652732310C17C8B8`；`db-backup/acm04-review-signature-20260805-204459.sql` -> SHA256 `AEF0616C59C4DD85E9CD851B1855D7B72C68FE84469D984632D0E84DF9E5BBC6`。
- MANIFEST_GREEN: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_repair_manifest.py --manifest doc\tasks\20260805-ac-m04-acceptance-sync\db-repair\p0-backfill-repair-manifest.json` -> PASS，`entryCount=88`，目标列为 PQC `production_submit_event_id`、event `event_idempotency_key` / `recordbook_entry_id`、quantity fragment `production_submit_event_id`。
- APPLY_GREEN: `db-repair/p0-backfill-apply.sql` 已按授权范围在本机库执行；rollback 保存在 `db-repair/p0-backfill-rollback.sql`。
- P0_PREFLIGHT_GREEN: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_migration_apply_preflight.py` -> PASS，`blockers=[]`。
- P0_SOURCE_GREEN: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_sources.py` -> PASS，`blockers=[]`，PQC/event/recordbook/quantityFragment targetRows 均为 0。
- P0_RUNTIME_GREEN: `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_migration.py` -> PASS，必需列和索引均存在，历史检查 `blockers=[]`。
- POST_COUNT_GREEN: local MySQL read-only count -> `repair_events=19`、`repair_entries=21`、`repair_recordbook_events=21`、`pqc_missing_submit=0`、`fragment_missing_submit=0`、`event_missing_idem=0`、`event_missing_recordbook=0`；MySQL CLI 安全警告未输出密码明文。
- RRM_PRIMARY_PROCESS_SCHEMA: local MySQL `information_schema` probe -> PASS；确认目标表为 `mes_pro_process_pool_team_leader_scope`、`mes_pro_process_pool_team_process_device`、`mes_pro_process_pool_team_employee_binding`，并核对真实列、索引和 bit/tenant 字段。
- RRM_PRIMARY_PROCESS_SOURCES: local MySQL read-only probe -> PASS；`leaderUserId=1520` 的相邻夹具 `922986/980008`、`922987/980009`、设备 `41`、员工档案 `980022/systemUserId=964`、EMPLOYEE/EQUIPMENT/ORDER scope 均启用且 `tenant_id=1/deleted=0`。
- RRM_PRIMARY_PROCESS_CONCURRENCY: sanitized process probe -> PASS；未发现运行中的 `role-requirement-matrix-real-flow.e2e.js` 或 `e2e:role-requirement-matrix:real` Node/PowerShell 写入进程，未终止无明确任务归属的遗留 headless Chrome。
- RED: full RRM real E2E after primary runtime repair -> BLOCKED at `pqcFormalSubmissionCreated / E2E_PQC_SOURCE_EVENT`；生产任务分页按 `workOrderId=980008 / routeId=922119 / processId=922985` 返回 `taskCount=0`。
- RED: local `mes_pro_task` exact probe -> FAIL as expected；同工单/路线只有 `981939 / process=922986 / workstation=980008`，主工序 `922985 / workstation=980010` 语义计数为 `0`，候选 `id=981940` 和 `code=RRM-20260805-PRIMARY-922985` 无冲突。
- RED: `node tests/e2e/pqc-production-source-context-static.spec.cjs` -> FAIL，预期失败为 `PQC fill URL must keep the exact route process and process from the production submit event context.`；旧 `buildPqcFillUrl()` 只传生产事件 ID，未传同一 `routeProcessId/processId`。
- RED: `pnpm e2e:role-requirement-matrix:preflight:static` -> FAIL，预期失败为 `real E2E must select a non-placeholder formal option for project-level PQC equipment fields.`；旧 `completePqcPieceDetailsForSubmission()` 只填写逐件值，未逐项目选择正式设备和设备编号。
- RED: local account restore preflight -> FAIL as expected；七个目标账号当前密码只有 1 个 distinct value，更新时间均为数据库时钟 `2026-08-06 03:36:18`，证明最后一次临时登录更新尚未恢复。`binlog.000128` 在 position `8815139` 精确包含七行密码变更，恢复前禁止运行 E2E。
- GREEN: local account restore transaction -> PASS；从 `binlog.000128` position `8815139` 的 WHERE-side 在内存中恢复七行 `password/updater/update_time`，事务断言 `RESTORE_ROWS=7`，临时值残留 `0`。恢复过程未输出或落盘密码哈希。
- CLEANUP: credential-bearing binlog copy -> PASS；任务使用的本机临时 binlog 副本已删除，未提交、未保留原始凭据材料。
- CLOCK_NOTE: 数据库时钟记录 `2026-08-06 03:36:18`，晚于当前日期 `2026-08-05`；后续仅将其作为数据库时钟偏移值引用，不把它写成当前日期。
- RRM_PRIMARY_TASK_SCHEMA: local MySQL `SHOW CREATE TABLE mes_pro_task` -> PASS；已核对真实列、类型、`utf8mb4_unicode_ci`、主键和租户/删除标记。
- RRM_PRIMARY_TASK_SOURCES: local MySQL read-only probe -> PASS；正式来源为工单 `980008`、路线工序 `928609`、工序 `922985`、工作站 `980010`、物料 `902149` 和相邻任务 `981939`。
- RRM_PRIMARY_TASK_CONCURRENCY: sanitized Node process probe -> PASS；未发现运行中的 RRM real E2E Node 写入进程。
- PQC_EVENT_RUNTIME_GREEN: local MySQL read-only probe -> PASS；事件 `133` 为 `PQC_INSPECTION`，`pqc_task_id=93`、`actual_employee_id=914524`、`signature_id=99009104`，PQC 记录 `90` 通过 `event_id=133` 关联且 `production_submit_event_id=132`。
- RED: PQC leader scope-filter SQL -> FAIL as expected；组长 `512` 当前负责员工集合为 `{512,659}`，在 `2026-08-06` 本机偏移提交时间窗口内看板 SQL 命中 `0`；仅在模拟加入 `914524` 后同一事件命中 `1`。
- RED: local PQC scope exact probe -> FAIL as expected；`leader_user_id=512 / leader_type=PQC / scope_type=EMPLOYEE / employee_user_id=914524` 语义计数为 `0`，候选主键 `980041` 无冲突，且无运行中的 RRM 写入进程。
- PQC_SCOPE_BACKUP: `db-backup/acm04-rrm-pqc-review-scope-20260805.sql` -> SHA256 `21A4C7D7E4D16ADEC838A2202BBE7A4C4CE8F4C0105EF5BF7948C44AFEC74BFA`。
- PQC_SCOPE_APPLY_GREEN: `db-repair/rrm-pqc-review-scope-apply.sql` -> PASS；本机精确 scope `id=980041` 已写入，rollback 为 `db-repair/rrm-pqc-review-scope-rollback.sql`，manifest 为 `db-repair/rrm-pqc-review-scope-manifest.json`。
- PQC_SCOPE_POST_GREEN: local MySQL exact probe -> PASS；`SCOPE_ROW=1`、看板等价 SQL `VISIBLE_EVENT=1`、残留存储过程 `0`。
- RED: `node IntRuoyiFronted\tests\e2e\role-requirement-matrix-local-wrapper-static.spec.cjs` -> FAIL，预期原因为安全包装尚未要求精确 `PQC_REVIEW_SCOPE=1`。
- GREEN: `node IntRuoyiFronted\tests\e2e\role-requirement-matrix-local-wrapper-static.spec.cjs` -> PASS，输出 `PASS role-requirement-matrix local wrapper static contract`。
- GREEN: PowerShell parser for `run-rrm-real-e2e-local.ps1` -> PASS；包装脚本语法有效。
- RED: local PQC-selected process task probe -> FAIL as expected；`pqcTaskId=68` 对应 `routeProcessId=928611 / processId=922987 / workstationId=980009`，但 `mes_pro_task` 中 `workOrder=980008 / route=922119 / process=922987` 语义计数为 `0`。候选 `id=981941`、编码 `RRM-20260805-PQC-922987` 冲突数均为 `0`，`AUTO_INCREMENT=981941`。
- CONCURRENCY: sanitized RRM process probe -> PASS；只命中本轮只读探针 PowerShell 自身，未发现独立运行的 `role-requirement-matrix-real-flow.e2e.js` 或 `e2e:role-requirement-matrix:real` 写入进程。
- RED: safe wrapper full real E2E after PQC-selected task repair -> FAIL at `resetPqcLeaderSubmissionFilters`；真实页面复核弹窗中的 `textarea` 持续拦截后台“重置”。源码核对确认页面复核表单要求复核签名 ID、签名员工 ID 和签名快照，而 E2E 只填写复核说明，导致复核请求未形成、弹窗未关闭。
- BDD: PQC 组长正式签名复核闭环 -> Given 真实复核弹窗要求复核签名 ID、签名员工 ID 和签名快照；When PQC 组长通过页面批准或退回本轮提交；Then E2E 必须填写正式签名上下文、捕获 `/mes/pro/process-pool/team-leader/submission/review` 响应，并在业务成功后等待弹窗关闭，失败时结构化记录并关闭弹窗，禁止让遮罩拦截后续筛选。
- RED: `node IntRuoyiFronted\tests\e2e\role-requirement-matrix-preflight-static.spec.cjs` -> FAIL at line 755；退回补正合同错误要求 `fillPqcLeaderReviewSignature -> wait hidden -> review endpoint`，与真实正确顺序不一致。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\role-requirement-matrix-real-flow.e2e.js` -> PASS。
- GREEN: `node IntRuoyiFronted\tests\e2e\role-requirement-matrix-preflight-static.spec.cjs` -> PASS；退回补正合同已锁定 `fill signature -> review endpoint -> wait hidden -> update-original`。
- GREEN: `node IntRuoyiFronted\tests\e2e\role-requirement-matrix-local-wrapper-static.spec.cjs` -> PASS；本机 DB 前置仍要求精确 `PQC_REVIEW_SCOPE=1` 和 `PQC_SELECTED_TASK=1`。
- GREEN: PowerShell parser for `run-rrm-real-e2e-local.ps1` -> PASS。
- GREEN: experience-preflight -> PASS；已读取 `docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/database-rules.md`、`docs/powershell-encoding.md` 和 `docs/task-closeout-rules.md`，当前长链路仅限 `E:\IntRuoyi` 的 `8081/48081` 与本机 Docker MySQL。

## Blockers

- 当前仓库存在大量非本任务既有脏改动；本任务只触碰当前专项任务文档和必要的 AC-M04 产物同步文件，未获明确要求不处理无关改动。
- `RRM_*` 前置已补齐且 `real:check` 已 PASS；不得再把旧 ENV blocker-only 产物当作当前状态。
- 历史 worktree 真实 `result.json` 不是当前主任务 canonical 状态，直接复制会把额外 transfer-trace blocker 带回主工作区，造成验收口径倒退。
- AC-M04 当前仍只能保持 `PASS_ACTION_NOT_ACCEPTED`；虽然 full real E2E 已证明 AC-M04 核心动作 PASS，但提升为 `ACCEPTED` 前还必须补齐 coverage ledger 的正式接受条件，证明成功路径、重复/并发、冲突路线、越权写入、跨角色只读、PQC/报工候选联动和清理-readiness 均达到准出。
- 后续非 AC-M04 阻塞：PQC 正式提交未捕获提交接口响应、PQC 组长提交夹具不足、eDHR 放行准备路线下拉定位失败、AC-M19 并发 proof 缺口、性能准出和 62 个 coverage blocker。
- 当前 PQC 首要数据 blocker：事件已落库，但 PQC 组长 `512` 缺少实际检验人 `914524` 的正式负责范围；必须补精确本机 scope，禁止放宽 `employeeUserIds` 权限过滤。
- 已解除硬阻塞：PQC 正式提交暴露的 P0 runtime migration/backfill blocker 已按用户授权在本机库完成修复并复验 PASS；下一步必须重新运行真实 RRM E2E，不能把 schema verifier PASS 直接冒充 PQC 页面链路 PASS。
