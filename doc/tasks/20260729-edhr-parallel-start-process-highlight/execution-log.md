# Execution Log

## User Intent

用户确认业务口径：球囊扩张压力泵工艺路线中，“工序开始”后的第一组 3 个直接后继工序都已进入当前可执行状态，批次执行详情页应与关系图一致全部显示黄色背景。

## BDD

BDD: 开始节点并行第一组全部显示当前运行态 -> Given 工艺路线关系图中“工序开始”直接连到粗洗工序、清洗工序、清洁工序，且这三个批次任务都处于 `WAITING/待打开` When 批记录管理员打开批次执行详情 Then 左侧这 3 个工序都应显示黄色当前运行态，后续非直接后继工序仍保持未开始状态，填写动作仍只由 `OPEN_FORM` 控制。

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

## Milestones

- completed: 识别后端详情接口当前只返回单个 `currentProcess*`，前端黄底只消费单个当前工序。
- completed: 建立 RED 合同，锁定开始节点第一组并行工序都应显示当前运行态。
- completed: `BatchExecutionDetailPage.vue` 新增 `isCurrentExecutableProcessGroup`，按任务 `available === true`、未完成、非可选来识别当前可执行工序组；产品信息虚拟 80 工序排除；`canOpenTask` 仍由 `OPEN_FORM` 控制。
- completed: 目标静态合同、相邻批次详情合同、后端多起点路线创建回归和前端类型检查通过。
- completed: 经验沉淀到现有前端长期门禁文档。
- completed: 收尾清理已删除本任务临时 evidence，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- in_progress: 提交和推送。

## Verification

- PASS: 见 Commands 中 RED/GREEN/REGRESSION 记录。

## Blockers

- NOTE: 当前工作区存在并行任务未提交改动，提交阶段必须按项目 Git 规则处理基线与选择性暂存；不得回滚或覆盖这些改动。
