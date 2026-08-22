# Execution Log

## Task Intent

- 用户最终要求：在独立 worktree 实现“活跃订单加入时正式绑定领料单”，并完成 task-owned commit、fast-forward 融合和主线验证；不启动服务、不运行写入型 E2E。
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

## TDD Evidence

代码检索和文档审阅不是 TDD 证据。前端静态合同已完成 RED -> GREEN；后端 Maven 由于环境缺失仍 NOT RUN。

RED: `node IntRuoyiFronted/src/api/mes/pro/processpool/teamLeaderPickListBinding.static.spec.cjs` -> FAIL（实现前），原因是 API/page 没有领料绑定字段和选择入口。

GREEN: `node IntRuoyiFronted/src/api/mes/pro/processpool/teamLeaderPickListBinding.static.spec.cjs` -> PASS（实现后）。

RED/GREEN: 后端定向 Maven -> NOT RUN，环境缺少 `mvn`/`mvnw.cmd`，不是用静态检查冒充通过。

REGRESSION: 后端既有活跃订单/来源服务测试 -> NOT RUN（缺少 Maven）；`pnpm run ts:check` -> FAIL，命中仓库既有 `batchrecordcelllink` 的重复 `routeProcessId` 声明和调用字段错误，未命中本次变更文件，不能宣称 PASS。

## Milestone Update

- Milestone 1-2: completed. Rules and current code facts recorded.
- Milestone 3: completed. Target data, API, state、FR1-PICK-6/7/9主契约及8/10/11邻接契约已在 development-plan.md 校正。
- Milestone 4: completed. BDD and strict TDD plan are in `test-plan.md`.
- Milestone 5: completed. Active-order binding, full-header/detail snapshot persistence, frontend selection, and downstream binding-ID consumption are implemented; backend compile/test evidence remains blocked by the missing Maven tool.

## Blockers

- Maven blocker: `mvn -pl yudao-module-mes -am -DskipTests compile` -> NOT RUN，当前 worktree 没有 `mvn` 或 `mvnw.cmd`，因此后端编译/单测未执行。
- 主线融合 blocker: 必须先完成 task-owned 文件筛选和 commit，再以 fast-forward-only 融合；主工作树存在其它未提交改动，不能覆盖。

- BLOCKED for E2E: no confirmed test tenant/account/formal approved pick-list fixture and no runtime was started, as required by user scope.

## Implementation Evidence

- `node IntRuoyiFronted/src/api/mes/pro/processpool/teamLeaderPickListBinding.static.spec.cjs` -> PASS。
- `pnpm run ts:check` -> FAIL（仓库既有 batchrecordcelllink 类型错误，详见 verification-report.md）。
- RED/GREEN: 原静态合同在实现前按旧字段失败；实现后同一静态合同 -> PASS。该证据只覆盖前端接口/页面合同，不替代后端测试。
- 后端加入请求新增 `pickListId`、候选 hash、幂等键；绑定头/全部明细快照、source hash、binding version 和唯一约束已实现。
- `MesProductionPickListSourceService.ResolveCommand` 强制携带 `pickListBindingId`；writer、完成后回填和 dossier 传递稳定绑定 ID；source service 从绑定头/全部明细快照读取，禁止 `selectListByProductionOrderNo` 作为活跃订单来源。
- `git diff --check` -> PASS（仅报告仓库既有 LF/CRLF 转换警告，无 whitespace error）。

## Scope Compliance

- Production code modified: yes, task-owned Java/Vue/SQL/tests only.
- Database/schema modified: task-owned migration added; not applied to any database.
- Services started/stopped: no.
- Write-type E2E executed: no.
- Fallback or silent downgrade introduced: no.

## Closeout Evidence

- task-owned closeout 尚未执行；在 Maven/主线融合完成前保持 `ready_for_closeout`。
