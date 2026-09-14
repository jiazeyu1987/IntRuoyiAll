# Execution Log

## Task Intent

- 用户要求：在独立 worktree 完成流程修复1前后端实现、提交、融合并验证；只处理流程1，不宣称其它流程全链路完成。
- 任务目录：`doc/tasks/20260821-flow-repair-01-active-order-pick-list-binding`。
- 设计结论：当前加入接口只有 `workOrderId`，必须增加显式领料单选择、绑定聚合、明细快照和批次执行关系；后续消费者不得继续按工单号反查。

## Read Evidence

- 已读取根目录 `AGENTS.md`，确认 Windows、UTF-8、无 fallback、任务文档、BDD/TDD、E2E 和三类独立表单来源规则。
- 已读取 `docs/task-closeout-rules.md`，确认任务目录、结构校验和 closeout 状态要求。
- 已读取 `docs/experience-index.md`，命中正式领料单来源、来源证据哈希、批次执行关系、状态 owner、幂等和旧数据迁移门禁。
- 已读取 `docs/product/production-role-system-operations.md`。
- 已读取 `docs/backend-development.md` 中“活跃订单申请放行资料必须只使用正式来源”章节：加入/完成/回填/批次/四份资料/放行必须按正式来源和同事务边界执行，禁止 `formBindings`、默认槽位、反查和 mock。
- 已读取 `docs/frontend-development.md` 和 `docs/e2e-rules.md`，确认真实页面、租户/账号、Long ID、异常清理和证据要求。

## Read-Only Code Audit

### Audit commands

- `rg -n --glob '*.java' "active-order|activeOrder|ActiveOrder|workOrderId|pickList|领料" IntRuoyiBackend\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\controller\admin\pro\processpool\team IntRuoyiBackend\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\processpool\team`
- `rg -n --glob '*.ts' --glob '*.vue' "active-order|activeOrder|workOrderId|领料单|pickList" IntRuoyiFronted\src\api\mes\pro\processpool IntRuoyiFronted\src\views\mes\pro\processpool`
- `rg -n -A 95 -B 8 "public MesTeamLeaderActiveOrderAddResult addActiveOrder" ...\MesTeamLeaderActiveOrderServiceImpl.java`
- `rg -n -A 100 -B 15 "resolveActiveOrderHistory|selectExistingActiveOrder" ...\MesTeamLeaderActiveOrderServiceImpl.java`
- `Get-Content -Encoding utf8 ...\MesTeamLeaderActiveOrderAddReqVO.java`, `...AddReqBO.java`, `...MesProcessPoolActiveOrderDO.java`。
- `Get-Content -Encoding utf8 ...\MesProductionPickListSourceService.java`, `...\MesProductionPickListSourceServiceImpl.java`。
- `Get-Content -Encoding utf8 ...\MesProEdhrBatchExecutionDO.java`, `...\MesProEdhrBatchExecutionMapper.java`。
- `rg -n -A 90 -B 35 "MesProductionPickListSourceService|resolveValue|pickListId|pickListItemId" ...\MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl.java`

### Audit facts

1. `MesTeamLeaderActiveOrderAddReqVO`、`MesTeamLeaderActiveOrderAddReqBO` 和前端 `TeamLeaderActiveOrderAddReqVO` 只有 `workOrderId`。
2. `TeamLeaderWorkbenchPage.vue` 新增弹窗只有订单号/产品远程下拉，提交只调用 `addTeamLeaderActiveOrder({ workOrderId })`。
3. `MesProcessPoolActiveOrderDO` 没有 `pickListId`、`pickListBindingId` 或领料单快照；Mapper 的 active/reuse/history 查询按工单和路线状态。
4. `MesTeamLeaderActiveOrderServiceImpl.addActiveOrder` 在解析历史后可能直接 `REUSE/RECOVER`，新建只写工单、路线、QA、数量和状态，未验证领料单。
5. `MesProductionPickListSourceServiceImpl.resolveValue` 在放行批记录映射阶段按生产订单号读取领料单明细，筛选 `documentStatus=C` 并要求唯一已审核头；同物料多明细按稳定分录号排序取第一条。它没有接收活跃订单绑定 ID。
6. `MesProEdhrBatchExecutionDO` 和 `MesProEdhrBatchExecutionMapper` 只有工单/批号/路线上下文；批次执行创建和复用没有领料单关系。
7. `MesTeamLeaderActiveOrderReleaseBatchRecordWriterImpl` 只在 writer plan 阶段把临时解析的 `pickListId/pickListItemId/evidenceHash` 放入来源集合，时机晚于加入和批次创建。
8. 已读取流程修复6-11任务目录并校正职责：6回填后建批，7批次映射/放行后追溯，8四材料门禁，9多入口合同，10最终放行状态/追溯，11总门禁。

## BDD Scenarios Recorded

BDD: 生产组长必须显式选择正式领料单 -> Given 输入生产工单并加载候选 When 未选择领料单提交 Then 页面禁用提交且后端拒绝缺少绑定。

BDD: 成功加入同时落领料单头和明细快照 -> Given 已审核单据、稳定分录且领料单 productionOrderNo 与当前生产工单号精确一致 When 提交绑定请求 Then 活跃订单、绑定、明细快照和审计同事务成功。

BDD: 未审核领料单阻断 -> Given `documentStatus != C` When 提交 Then 返回 `PICK_LIST_NOT_APPROVED` 且无部分写入。

BDD: 工单和领料单不匹配阻断 -> Given 领料单 productionOrderNo 与当前生产工单正式工单号不一致 When 提交 Then 返回 `PICK_LIST_WORK_ORDER_MISMATCH`。

BDD: 明细稳定身份阻断 -> Given 分录号缺失或重复 When 提交 Then 返回结构化 blocker。

BDD: 同物料多明细确定性 -> Given 多条合法分录 When 读取来源 Then 全部可追溯；canonical 仅用于单值字段解析时按 `sourceEntryId` 升序确定，不能替代全量明细快照。

BDD: 同键同载荷幂等 -> Given K 已成功 When 相同载荷重试 Then 返回同一回执且不重复写入。

BDD: 同键不同载荷冲突 -> Given K 已绑定 P When K 改绑 Q Then 返回 `IDEMPOTENCY_CONFLICT`。

BDD: 活跃订单重复加入只允许相同绑定复用 -> Given 已有 P When 再选 P/Q Then P 返回 REUSE，Q 返回绑定冲突。

BDD: 并发加入只形成一个绑定 -> Given 并发提交相同工单/领料单 When 事务竞争 Then 唯一约束只保留一个有效绑定。

BDD: 来源漂移不静默换单 -> Given 绑定后 ERP 头/明细发生变化 When 完成或建批 Then 阻断并保留原快照。

BDD: 完成节点把绑定传给批次执行 -> Given 双 100% 和三类回填成功 When 创建批次 Then 同事务写批次-绑定关系并冻结绑定。

BDD: 活跃订单与独立入口分类 -> Given 活跃链路必须有 pickListBindingId、独立入口可无 activeOrderId 但有等价正式来源凭证 When 创建/放行 Then 按流程修复9分类合同处理，缺凭证才阻断。

BDD: 全量明细快照 -> Given 同物料多分录 When 绑定、建批、追溯 Then 全部明细保留，canonical 只用于单值字段。

BDD: 生产工单号精确匹配 -> Given 领料单包含生产工单号列 When 查询候选或提交绑定 Then 仅允许与当前生产工单正式工单号一致的已审核领料单。

BDD: 追溯返回冻结来源 -> Given 批次关系存在且 ERP 已更新 When 查询追溯 Then 返回绑定时快照、当前核验状态和漂移审计。

## TDD/Verification Evidence

代码检索、业务规则阅读和文档结构检查是 AUDIT/STRUCTURE 证据，不是 TDD RED/GREEN。历史设计 RED 保留为实现前证据，当前 GREEN/REGRESSION 只记录真实已运行命令。

RED (历史): `mvn ... MesTeamLeaderActiveOrderPickListBindingTest` -> FAIL；实现前缺少请求字段、绑定聚合和 schema，后续代码已关闭该缺口。

RED (历史): 前端绑定静态合同 -> FAIL；实现前 UI/API 只有 `workOrderId`，后续已增加候选和必选提交。

RED (历史): schema 合同 -> FAIL；实现前缺少绑定头/明细表，后续 SQL 已建立正式关系表和唯一约束。

GREEN: `C:\Users\BJB110\Documents\Codex\tools\apache-maven-3.9.16\bin\mvn.cmd -pl yudao-module-mes -DskipTests compile` -> PASS。

GREEN: 定向 JUnit/schema 合同 Maven 命令 -> PASS；Tests run: 100, Failures: 0, Errors: 0, Skipped: 0。

GREEN: `node src/api/mes/pro/processpool/teamLeaderPickListBinding.static.spec.cjs` -> PASS。

REGRESSION: `pnpm run ts:check` -> PASS。

REGRESSION: `git diff --check 5ac8b03ea..HEAD` -> PASS；`scripts/preflight/branch-runtime-port-guard.ps1` -> PASS（int_main 8081/48081）。

REGRESSION: 真实 Playwright 写入路径 -> NOT RUN；缺少确认测试租户、账号、已审核领料单和可清理数据，不能用 mock/API-only 替代。

## Milestone Update

- Milestone 1-2: completed. Rules and current code facts recorded.
- Milestone 3: completed. Target data, API, state、FR1-PICK-6/7/9主契约及8/10/11邻接契约已在 development-plan.md 校正。
- Milestone 4: completed. BDD and strict TDD plan are in `test-plan.md`.
- Milestone 5: completed. 流程1代码已实现、提交并融合当前 `int_main`；最新主线 HEAD 为 `1bc0be23e8665485456265b4e92ef78a7154c1f2`。MES compile、100 个定向测试、前端静态、schema、diff 和 runtime guard 均有 PASS 证据。

## Blockers

- BLOCKED for E2E: 没有确认测试租户、账号、已审核领料单、可清理数据和已启动服务。
- Cross-thread boundary: 流程修复6/7/8/9/10/11的后续批次、材料、入口合同、最终放行和总门禁不由流程1宣称完成。

- BLOCKED for E2E: no confirmed test tenant/account/formal approved pick-list fixture and no runtime was started, as required by user scope.

## Scope Compliance

- Production code modified: yes，范围仅流程1 task-owned Java/Vue/SQL/测试。
- Database/schema modified: migration SQL 已提交；本次未连接或写入数据库。
- Services started/stopped: no.
- Write-type E2E executed: no.
- Fallback or silent downgrade introduced: no.

## Closeout Evidence

- `task_closeout.py --task-id 20260821-flow-repair-01-active-order-pick-list-binding --mode preview --extra-keep development-plan.md --extra-keep test-plan.md` -> PASS；五份正式文档保留，仅删除任务临时 .keep。
- `task_closeout.py --task-id 20260821-flow-repair-01-active-order-pick-list-binding --mode apply --extra-keep development-plan.md --extra-keep test-plan.md` -> PASS；仅删除 .keep，无 blocked/warnings。
- 收尾后 `task.md` 和 `verification-report.md` 状态为 `completed`；E2E 保持 NOT RUN blocker。
