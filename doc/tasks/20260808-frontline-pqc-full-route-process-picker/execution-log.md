# Execution Log

## User Intent

- 用户明确要求：一线 PQC 选工序应显示整条工艺路线，而不是只有一个待检工序。

## BDD Scenarios

- `BDD: 一线 PQC 展示冻结整条路线 -> Given 活跃订单绑定 14 道冻结路线工序但只有粗洗存在 PENDING PQC 任务 / When 一线 PQC 打开工序选择 / Then 工序选择展示冻结路线全部工序，只有粗洗带出 pqcTaskId、规程快照和检验项。`
- `BDD: 一线 PQC 不纳入当前路线后续新增工序 -> Given 当前路线比活跃订单冻结版本多出新增工序 / When 一线 PQC 请求 active-order/processes / Then 返回集合不包含未冻结进该活跃订单的新增工序。`
- `BDD: 无待检任务工序不可提交 -> Given 选中的路线工序没有 PENDING PQC 任务 / When 用户进入 PQC 填写区或尝试提交 / Then 前端显示缺少 PQC 任务或 QA 规程快照，后端提交仍要求正式 pqcTaskId。`

## Milestone Updates

- 2026-08-08：识别现有实现按 `mes_pro_process_pool_active_order_process_snapshot` 枚举且 `resolvePqcTaskContext` 对无待检任务工序抛错或跳过，导致只显示有任务的单一工序。
- 2026-08-08：新增并使用回归场景 `shouldDisplayFullFrozenRouteAndAttachPqcTaskOnlyToPendingProcess`，锁定冻结路线两道工序都展示，仅第一道待检工序带 `pqcTaskId` 和检验项。
- 2026-08-08：后端 `active-order/processes` 改为按活跃订单 `routeVersionId` 对应 `routeSnapshotJson.configSnapshots.flowGraph.nodes` 枚举冻结路线工序；当前路线工序仅用于校验并补充名称、工作站等元数据。
- 2026-08-08：PQC 任务上下文仅对正式 `PENDING` 任务附着；无待检任务的冻结工序返回 `pqcTaskId=null`、`inspectionItems=[]`，提交链路继续要求正式 `pqcTaskId`。
- 2026-08-08：前端初始工序选择在 PQC 模式下优先选择首个 `hasPqcTaskSnapshot` 工序，否则才显示第一个冻结工序，避免页面默认停在不可填写工序。
- 2026-08-08：经验沉淀检查：`docs/experience-index.md` 已将 `active-order/processes`、活跃订单冻结工序、当前路线新增非冻结工序路由到 `docs/backend-development.md#mes-pqc-项目级检验快照门禁`，本任务不新建长期经验文档。
- 2026-08-08：task-closeout-cleanup preview/apply 完成，keep `task.md`、`execution-log.md`、`verification-report.md`，delete/blocked/warnings 均为 `<none>`。

## Verification Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldDisplayFullFrozenRouteAndAttachPqcTaskOnlyToPendingProcess" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，旧实现返回工序 `[4001]`，期望冻结路线 `[4001, 4002]`。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`。
- GREEN: `node tests\e2e\mes-frontline-pqc-process-picker-production-layout-static.spec.cjs` -> PASS，前端 PQC 工序选择生产布局和初始选择合同通过。
- GREEN: `git diff --check -- <本任务涉及文件>` -> PASS，无 whitespace/error 检查问题。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260808-frontline-pqc-full-route-process-picker\verification-report.md` -> PASS，缺陷修复证据结构有效。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-frontline-pqc-full-route-process-picker --mode preview` -> PASS，keep 3 个核心任务文件，无 delete/blocked/warnings。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-frontline-pqc-full-route-process-picker --mode apply` -> PASS，无删除项。

## Blockers

- 无当前阻塞。
- 注意：当前工作区已有大量非本任务改动；本任务只处理 PQC 工序选择相关文件和本任务文档，未暂存、未提交。
