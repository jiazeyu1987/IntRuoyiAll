# Execution Log

## User Intent

- 用户确认当前仍只能看到一个工序，并明确新口径：一线 PQC 选工序要可以看到“这个生产工单对应的产品的对应的产线的所有工序”。

## BDD Scenarios

- `BDD: 一线 PQC 展示生产工单产品产线全工序 -> Given 生产工单产品绑定的对应产线/路线有多道工序且只有一道工序有 PENDING PQC 任务 / When 一线 PQC 打开工序选择 / Then 工序选择展示该产线/路线全部工序，只有待检工序带 pqcTaskId、规程快照和检验项。`
- `BDD: 无待检任务工序不可提交 -> Given 用户选择了产线/路线中没有 PENDING PQC 任务的工序 / When 尝试进入 PQC 填写或提交 / Then 前端提示缺少 PQC 任务或规程，后端提交仍要求正式 pqcTaskId。`
- `BDD: 产品路线绑定不匹配必须阻塞 -> Given 生产工单产品未绑定当前路线或路线元数据缺失 / When 请求 active-order/processes / Then 后端 fail fast，不返回默认空成功或任意路线工序。`

## Milestone Updates

- 2026-08-08：任务启动，按用户新口径把展示集合从“冻结活跃订单工序/待检任务工序”调整为“生产工单产品对应产线/路线全部工序”。
- 2026-08-08：更新 `MesFrontlinePqcContextServiceImpl#listProcessesByActiveOrder`，候选工序改为读取当前路线全量 `routeProcessMapper.selectListByRouteId(routeId)`，并用 `resolveProductLineRouteProcesses` 校验路线工序身份与重复项。
- 2026-08-08：保留 `resolvePendingPqcTaskContext` 附着逻辑，只有正式 `PENDING` PQC 任务工序携带 `pqcTaskId`、规程快照和检验项；无待检任务工序只展示，不新增提交降级。
- 2026-08-08：更新回归用例 `shouldDisplayProductLineRouteProcessesEvenWhenOnlyOneProcessHasPqcTask`，覆盖两道路线工序仅一道有待检 PQC 任务时仍展示全工序。
- 2026-08-08：按 `project-experience-consolidation` 收尾要求，将旧“冻结快照为工序选择权威”的 PQC 门禁更新为“待检准入与工序选择分离”，同步 `docs/backend-development.md` 与 `docs/experience-index.md` 检索关键词，避免后续按旧口径回退。

## Verification Evidence

- `RED: mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldDisplayProductLineRouteProcessesEvenWhenOnlyOneProcessHasPqcTask" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, expected: <[4001, 4002]> but was: <[4001]>`
- `GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldDisplayProductLineRouteProcessesEvenWhenOnlyOneProcessHasPqcTask" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0.`
- `GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test -> PASS, Tests run: 31, Failures: 0, Errors: 0, Skipped: 0.`
- `GREEN: mvn -q -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test -> PASS.`
- `GREEN: node tests\e2e\mes-frontline-pqc-process-picker-production-layout-static.spec.cjs -> PASS: PQC process picker uses production picker layout.`
- `GREEN: git diff --check -- <task files> -> PASS, only Git CRLF normalization warnings.`
- `GREEN: rg -n "[ \t]+$" doc\tasks\20260808-frontline-pqc-product-line-process-picker -> PASS, no trailing whitespace.`
- `GREEN: python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence E:\IntRuoyi\doc\tasks\20260808-frontline-pqc-product-line-process-picker\bug-regression-evidence.md -> PASS, Bug regression evidence is valid.`
- `GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-frontline-pqc-product-line-process-picker --mode preview -> PASS, keep task records and bug evidence, delete none, blocked none.`
- `GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-frontline-pqc-product-line-process-picker --mode apply -> PASS, deleted none, blocked none.`
- `GREEN: git diff --check -- docs/backend-development.md docs/experience-index.md <task implementation files> -> PASS, only Git CRLF normalization warnings.`

## Blockers

- 无阻塞。
- 当前工作区已有大量非本任务改动；本任务只处理 PQC 工序选择相关文件和本任务文档，不 stage、不 commit、不清理其它任务产物。
