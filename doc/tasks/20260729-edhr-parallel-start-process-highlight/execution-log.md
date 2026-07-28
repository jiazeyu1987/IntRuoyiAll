# Execution Log

## User Intent

用户确认业务口径：球囊扩张压力泵工艺路线中，“工序开始”后的第一组 3 个直接后继工序都已进入当前可执行状态，批次执行详情页应与关系图一致全部显示黄色背景。

## BDD

BDD: 开始节点并行第一组全部显示当前运行态 -> Given 工艺路线关系图中“工序开始”直接连到粗洗工序、清洗工序、清洁工序，且这三个批次任务都处于 `WAITING/待打开` When 批记录管理员打开批次执行详情 Then 左侧这 3 个工序都应显示黄色当前运行态，后续非直接后继工序仍保持未开始状态，填写动作仍只由 `OPEN_FORM` 控制。

BDD: 多前置汇合工序不得进入第一组当前可执行 -> Given 路线关系图存在三个无入边工序共同指向一个汇合工序 When 新建批次后所有工序任务均未填写完成 Then 三个无入边工序 `available=true`，汇合工序 `available=false` 且提示直接前置工序未全部完成。

## Commands

- READ: `bug-regression-fix-loop`、`backend-api-delivery`、`frontend-feature-delivery` 技能及其 contract -> PASS。
- READ: `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/backend-development.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md` -> PASS。
- READ: `docs/experience-index.md`、`docs/e2e-rules.md`、`docs/branch-runtime-ports.md` -> PASS。
- STATUS: `git status --short --branch` -> branch `int_main` behind `origin/int_main` by 12 commits; workspace contains existing dirty changes from parallel tasks before this task started.
- RED: `node tests/e2e/edhr-batch-parallel-current-process-highlight-static.spec.js` -> FAIL, expected reason: missing `isCurrentExecutableProcessGroup`; page only consumes single `currentProcess*`, so parallel root tasks cannot all be yellow.
- DIAGNOSTIC: temporary backend assertion on `MesProEdhrBatchExecutionServiceTest#openOrCreate_allowsValidMultiStartMergeRouteGraphWhenBatchBindingsExist` -> FAIL because that historical fixture does not refresh the active route version snapshot after mutating graph edges; assertion was removed, no backend production or test diff remains.
- GREEN: `node tests/e2e/edhr-batch-parallel-current-process-highlight-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-admin-current-process-highlight-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-process-state-background-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-admin-filler-visibility-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-process-companion-forms-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/edhr-batch-product-info-virtual-process-static.spec.js` -> PASS。
- REGRESSION: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_allowsValidMultiStartMergeRouteGraphWhenBatchBindingsExist" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- REGRESSION: `pnpm ts:check` -> PASS。
- EXPERIENCE: 合并到 `docs/frontend-development.md#eDHR 当前工序运行态展示门禁`，并更新 `docs/experience-index.md` 关键字 -> PASS。
- CLEANUP PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260729-edhr-parallel-start-process-highlight --mode preview` -> PASS，计划删除本任务临时 `bug-regression-evidence.md`、`frontend-feature-evidence.md`。
- CLEANUP APPLY: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260729-edhr-parallel-start-process-highlight --mode apply` -> PASS。
- WORKTREE: `git worktree add -b codex/20260729-edhr-parallel-start-process-highlight D:\IntRuoyiWorktree\20260729-edhr-parallel-start-process-highlight origin/int_main` -> PASS，用干净 worktree 隔离主工作区并行改动。
- WORKTREE GREEN: `node tests/e2e/edhr-batch-parallel-current-process-highlight-static.spec.js`、`node tests/e2e/edhr-batch-admin-current-process-highlight-static.spec.js`、`node tests/e2e/edhr-batch-process-state-background-static.spec.js`、`node tests/e2e/edhr-batch-admin-filler-visibility-static.spec.js`、`node tests/e2e/edhr-batch-process-companion-forms-static.spec.js`、`node tests/e2e/edhr-batch-product-info-virtual-process-static.spec.js` -> PASS。
- WORKTREE GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_allowsValidMultiStartMergeRouteGraphWhenBatchBindingsExist" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- WORKTREE DEPENDENCY: `pnpm install --frozen-lockfile --reporter append-only` -> PASS；此前较短 120s/300s 安装窗口超时，未产生锁文件改动。
- WORKTREE GREEN: `pnpm ts:check` -> PASS。
- PORT GUARD: `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`int_main slot 12`，frontend `8093`，backend `48093`。
- COMMIT: `git commit -m "修复批次执行并行当前工序高亮"` -> PASS，implementation commit `6423023d`。
- PUSH: `git push origin HEAD:int_main` -> PASS，`origin/int_main` updated to implementation commit `6423023d`。
- RED: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#getUsesCurrentRouteGraphWhenBatchTasksWereCreatedFromCurrentRouteConfig" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，expected reason: 批次任务使用当前路线工序 ID，但冻结路线快照仍是旧工序 ID 时，详情门禁按旧快照校验并抛出 `PRO_EDHR_BATCH_EXECUTION_DEFAULT_REPORT_REQUIRED`。
- GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_allowsValidMultiStartMergeRouteGraphWhenBatchBindingsExist+getUsesCurrentRouteGraphWhenBatchTasksWereCreatedFromCurrentRouteConfig" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，2 tests。
- GREEN: `mvn.cmd -pl yudao-server -am -DskipTests package` -> PASS。
- RUNTIME: slot 12 backend restarted on `48093`; `GET http://127.0.0.1:48093/actuator/health` -> `{"status":"UP"}`。
- API VERIFY: 芋道源码/admin 调用 `GET /admin-api/mes/pro/edhr-batch-execution/get?id=900000000903` -> PASS，`tasksLen=25`，当前可执行工序仅 `粗洗工序:928609`、`清洗工序:928611`、`清洁工序:928612`，`closeBlockers=[]`。
- E2E RED: `node doc/tasks/20260729-edhr-parallel-start-process-highlight/parallel-current-real-e2e.cjs` -> FAIL，expected reason: 脚本等待内部执行号/批号文本，页面目标验证应以详情接口命中目标批次和工序组渲染为准。
- E2E GREEN: `node doc/tasks/20260729-edhr-parallel-start-process-highlight/parallel-current-real-e2e.cjs` -> PASS，真实页面显示 `粗洗工序`、`清洗工序`、`清洁工序` 三个黄色当前工序，`组装Ⅰ工序` 非黄色，MES 写请求数 `0`。
- GREEN: experience-preflight -> PASS，已合并到 `docs/backend-development.md#当前配置与发布快照边界`、`docs/frontend-development.md#eDHR 当前工序运行态展示门禁`、`docs/e2e-rules.md#真实 E2E 页面加载判据门禁`，并更新 `docs/experience-index.md` 关键词。
- CLEANUP PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260729-edhr-parallel-start-process-highlight --mode preview --worktree-closeout off` -> PASS，保留核心任务文档和 `real-e2e-evidence.md`，计划删除一次性 Playwright 脚本和截图。
- CLEANUP APPLY: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260729-edhr-parallel-start-process-highlight --mode apply --worktree-closeout off` -> PASS，已删除 `parallel-current-real-e2e.cjs` 与 `parallel-current-process-highlight.png`。
- SYNC: `git fetch origin`; `git rebase origin/int_main` -> PASS，当前任务提交重放为 `483a9ce4`，基线 `origin/int_main` 为 `52a314ea`。
- POST-REBASE GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_allowsValidMultiStartMergeRouteGraphWhenBatchBindingsExist+getUsesCurrentRouteGraphWhenBatchTasksWereCreatedFromCurrentRouteConfig" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，2 tests。
- RUNTIME CLEANUP: `Stop-Process -Id 14464,54944,33612,55808,11492,38304,17552 -Force`；`Get-NetTCPConnection -LocalPort 8093,48093` -> PASS，`8093`、`48093` 均无监听。

## Milestones

- completed: 识别后端详情接口当前只返回单个 `currentProcess*`，前端黄底只消费单个当前工序。
- completed: 建立 RED 合同，锁定开始节点第一组并行工序都应显示当前运行态。
- completed: `BatchExecutionDetailPage.vue` 新增 `isCurrentExecutableProcessGroup`，按任务 `available === true`、未完成、非可选来识别当前可执行工序组；产品信息虚拟 80 工序排除；`canOpenTask` 仍由 `OPEN_FORM` 控制。
- completed: 目标静态合同、相邻批次详情合同、后端多起点路线创建回归和前端类型检查通过。
- completed: 经验沉淀到现有前端长期门禁文档。
- completed: 收尾清理已删除本任务临时 evidence，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- completed: implementation commit `6423023d` 已推送到 `origin/int_main`。
- completed: 后端任务门禁改为使用完整直接前置集合，多前置汇合工序必须等待所有直接前置工序完成。
- completed: 旧冻结快照与当前路线配置不一致但批次任务来自当前配置时，按当前路线关系图计算可执行工序；缺失正式图源仍显式阻塞。
- completed: 真实芋道源码/admin Playwright E2E 已通过，页面三个第一组工序均黄底。
- completed: 最终 closeout 记录已补齐，本任务运行态已清理。

## Verification

- PASS: 见 Commands 中 RED/GREEN/REGRESSION/API VERIFY/E2E GREEN 记录。

## Blockers

- 无。
