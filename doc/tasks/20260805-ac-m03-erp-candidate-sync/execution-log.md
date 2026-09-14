# Execution Log

## 2026-08-05

- User intent: 继续 AC-M03「同步 ERP 候选数据」分析与补证，回答当前做到哪一步以及应该怎么做，并推进实现。
- Rule bootstrap: 已读取 `docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/backend-development.md`、`docs/database-rules.md`。
- Skill bootstrap: 已读取 `backend-api-delivery` 与 `bdd-tdd-acceptance-planner` 技能及其引用契约。
- Worktree baseline: `git status --short --branch` 显示 `int_main...origin/int_main [ahead 1]`，且存在大量非本任务修改；本任务仅限 AC-M03 任务目录和 MES 同步相关文件。
- Experience gate: `docs/experience-index.md` 存在；本任务命中通用后端、数据库、PowerShell/Maven、task-closeout 门禁，暂未发现 AC-M03 专用长期经验。
- BDD: AC-M03 duplicate ERP order idempotency -> Given ERP 返回同一正式生产订单/物料来源多次或重复运行同步 When 同步任务执行 Then 只保留一个正式工单事实和一个同步记录，不生成重复工单。
- BDD: AC-M03 out-of-order ERP snapshot -> Given 已同步较新的 ERP 正式快照 When 旧的乱序快照再次到达 Then 不得回退已确认状态或覆盖较新快照。
- BDD: AC-M03 conflicting ERP source identity -> Given 相同正式来源 ID 指向冲突工单编号或物料 When 同步任务执行 Then 必须阻塞或显式记录冲突，不得创建第二个事实掩盖冲突。
- BDD: AC-M03 transfer and batch trace idempotency -> Given 同一活动工单重复触发调拨/批次追溯 When 追溯服务重复执行 Then 正式 transfer/detail/materialStock/batch ID 的追溯事实只生成一次。

## Current Evidence

- AC-M03 当前矩阵证据仍为 `UNCOVERED_BY_REAL_E2E`，不等同 ACCEPTED。
- 已知源代码显示 ERP 生产订单同步和调拨/批次追溯存在部分能力，但尚缺订单、调拨、发货、批次端到端正式 ID 幂等与异常路径证据。

## RED

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesKingdeeProductionOrderSyncServiceImplTest#syncWorkOrders_usesSourceRecordWorkOrderWhenBillNoChanges" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected `createdCount=0` but current implementation returned `createdCount=1`, proving same ERP formal source key could create a duplicate MES work-order fact after `billNo` changed.
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesKingdeeProductionOrderSyncServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 20 tests run with 2 failures: source-linked work order was not reused, and conflicting `billNo` did not fail fast.

## Implementation

- Updated `MesKingdeeProductionOrderSyncServiceImpl` so active sync now deduplicates by formal source key before work-order code.
- Changed existing work-order resolution to prefer `mes_kingdee_production_order_sync_record.work_order_id` over `billNo`.
- Added fail-fast validation when the source-linked work order conflicts with another existing work order that already owns the incoming `billNo`.
- Added fail-fast validation for missing ERP `fid` or material number before building the formal source key.
- Left transfer/batch trace implementation unchanged because existing service already uses idempotency keys over active order, transfer, line and detail IDs.

## GREEN

- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesKingdeeProductionOrderSyncServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 20 tests run, 0 failures, 0 errors.
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesActiveOrderTransferTraceServiceTest,MesActiveOrderTransferTraceSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 5 tests run, 0 failures, 0 errors.
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260805-ac-m03-erp-candidate-sync/backend-api-evidence.md` -> PASS, backend API evidence is valid.
- GREEN: `git diff --check -- <AC-M03 touched files>` -> PASS; Git only reported LF-to-CRLF working-copy warnings for the two Java files.
- GREEN: UTF-8 readback for all task Markdown files -> PASS.

## Maven Timeout Note

- One intermediate GREEN rerun timed out while Maven PID `54004` was still compiling. `jcmd 54004 Thread.print` showed the main thread in javac class reading, and the process command line matched this task's Maven command.
- Per Maven target/process gate, only PID `54004` was stopped; unrelated runtime Java PIDs `3392` and `42300` were not touched.
- The same standard Maven command was rerun afterward and passed.

## Remaining AC-M03 Acceptance Gap

- Backend/source gate is stronger after this task, but AC-M03 remains below ACCEPTED until M6 real E2E coverage records an action key for ERP candidate sync and proves duplicate, out-of-order, and conflict paths through the accepted matrix evidence chain.

## Experience Consolidation

- Ran project-experience-consolidation review before summary. No new long-term experience document was created because the only reusable workflow lesson, task-owned Maven timeout handling, is already covered by `docs/powershell-memory.md`; AC-M03 business state remains task-local evidence.

## Closeout Boundary

- `git status --short --branch` at task start already showed `int_main...origin/int_main [ahead 1]` plus many unrelated dirty files.
- Current task-owned changed files are limited to the MES production order sync service/test and `doc/tasks/20260805-ac-m03-erp-candidate-sync/`.
- No commit/push was performed because committing per project policy would require handling unrelated pre-existing dirty/ahead state; this was not safe to do as part of the AC-M03 implementation slice without explicit user direction.
