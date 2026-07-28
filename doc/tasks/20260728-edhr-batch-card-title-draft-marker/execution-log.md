# Execution Log

## User Intent

用户要求按已确认计划修复 eDHR 批次详情右侧当前工序表单卡片标题：不再把同一个 `EDHRB-...` 批次编号作为每张卡片主标题，改为表单名称；草稿任务名称追加 ASCII `*`。

## Preconditions And Gates

- 已读取前端开发、E2E、任务收尾、PowerShell 编码、本地运行、登录访问规则。
- 已读取 `docs/experience-index.md` 并摘录本任务适用门禁到 `task.md`。
- 当前工作区在任务开始前存在多项未提交/未跟踪改动；已按脏工作区基线门禁提交独立基线。

## Dirty Worktree Baseline

- Command: `git status --short --branch --untracked-files=all` -> 任务开始前分支 `int_main...origin/int_main [ahead 8, behind 6]`，存在多项既有脏改动。
- Command: `git diff --cached | rg -n "(?i)(password|token|secret|private key|密钥|密码)"` -> 命中环境变量名和“不记录密码”说明，未发现裸露凭据。
- Baseline commit: `08c3eae0 chore: baseline dirty worktree before edhr card title task`
- Baseline files:
  - `IntRuoyiFronted/src/components/TableQuickFilter/index.vue`
  - `IntRuoyiFronted/src/hooks/web/useTableQuickFilter.ts`
  - `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`
  - `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue`
  - `IntRuoyiFronted/tests/e2e/assist-grid-per-user-mapping-static.spec.js`
  - `IntRuoyiFronted/tests/e2e/assist-grid-role-responsibility-static.spec.js`
  - `IntRuoyiFronted/tests/e2e/edhr-batch-record-form-list-product-filter-autocomplete-static.spec.js`
  - `IntRuoyiFronted/tests/e2e/edhr-dynamic-form-cell-link-real.e2e.js`
  - `IntRuoyiFronted/tests/e2e/edhr-visual-fill-config-real-flow.e2e.js`
  - `IntRuoyiFronted/tests/e2e/edhr-visual-fill-config-static.spec.js`
  - `doc/tasks/20260728-assist-role-responsibility-mode/execution-log.md`
  - `doc/tasks/20260728-assist-role-responsibility-mode/task.md`
  - `doc/tasks/20260728-assist-role-responsibility-mode/verification-report.md`
  - `doc/tasks/20260728-batch-record-product-name-dropdown/bug-regression-evidence.md`
  - `doc/tasks/20260728-batch-record-product-name-dropdown/execution-log.md`
  - `doc/tasks/20260728-batch-record-product-name-dropdown/frontend-feature-evidence.md`
  - `doc/tasks/20260728-batch-record-product-name-dropdown/task.md`
  - `doc/tasks/20260728-batch-record-product-name-dropdown/verification-report.md`
  - `doc/tasks/20260728-edhr-cell-link-main-e2e-repair/dynamic-form-real-e2e-evidence.md`
  - `doc/tasks/20260728-loss-process-latest-assist-default/execution-log.md`
  - `doc/tasks/20260728-loss-process-latest-assist-default/initialize_latest_template_assist_default.py`
  - `doc/tasks/20260728-loss-process-latest-assist-default/task.md`
  - `doc/tasks/20260728-loss-process-latest-assist-default/verification-report.md`
  - `docs/e2e-rules.md`
  - `docs/experience-index.md`
- Baseline after-status: `int_main...origin/int_main [ahead 9, behind 6]`，工作区仅剩本任务文档未跟踪。

## BDD

- BDD: 右侧卡片标题使用表单名称 -> Given 用户打开 eDHR 批次详情页并选中某个工序 / When 右侧展示该工序的多张表单任务卡片 / Then 每张卡片的主标题显示该任务的表单名称，而不是重复显示同一个批次执行编号。
- BDD: 草稿任务名称追加星号 -> Given 当前工序下存在状态为草稿的表单任务 / When 右侧渲染该任务卡片 / Then 卡片标题在表单名称后追加 ASCII `*`。
- BDD: 非草稿任务名称不追加星号 -> Given 当前工序下存在待打开、填写中、已完成或其他非草稿状态的表单任务 / When 右侧渲染该任务卡片 / Then 卡片标题仅显示表单名称，不追加 `*`。

## RED

- RED: `node tests/e2e/edhr-batch-card-title-draft-marker-static.spec.js` -> FAIL, expected reason: 右侧当前工序表单卡片列表仍包含 `edhr-batch-detail__rail-execution-code`，并继续使用 `detail?.batchExecutionCode` 作为每张卡片主标题。

## GREEN

- GREEN: `node tests/e2e/edhr-batch-card-title-draft-marker-static.spec.js` -> PASS
- GREEN: `node tests/e2e/edhr-batch-companion-forms-right-panel-static.spec.js` -> PASS
- GREEN: `node tests/e2e/edhr-batch-process-companion-forms-static.spec.js` -> PASS
- GREEN: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> PASS
- GREEN: `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js` -> PASS
- GREEN: `pnpm ts:check` -> PASS

## Verification Evidence

- 聚焦静态合同确认右侧当前工序表单卡片列表不再包含 `edhr-batch-detail__rail-execution-code` 或卡片级 `detail?.batchExecutionCode`，可见标题和 `title` 均使用 `resolveTaskCardDisplayName(task)`。
- 静态合同确认 `resolveTaskCardDisplayName(row)` 复用 `resolveTaskDisplayName(row)`，仅当 `row.status === EDHR_BATCH_TASK_STATUS_DRAFT` 且名称不是 `--` 时追加 ASCII `*`。
- 红框隐藏合同确认右侧独立填写元信息仍无残留，单据卡片填写人 `resolveTaskCardFillersText(task)` 仍保留。
- 类型检查通过 `vue-tsc --noEmit -p tsconfig.relaxed.json`。
- 真实页面只读验证前置检查：`where.exe npx` -> PASS；`Get-NetTCPConnection -LocalPort 8081,48081 -State Listen` -> 8081/48081 均监听；环境变量检查显示 `EDHR_COMPANION_E2E_PASSWORD=False`、`PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH=False`。
- Experience consolidation: 在 `docs/e2e-rules.md` 新增 `eDHR 右侧表单卡片标题门禁`，并在 `docs/experience-index.md` 增加 `resolveTaskCardDisplayName`、`草稿星号`、`EDHRB 重复标题` 等关键词路由；`rg -n "eDHR 右侧表单卡片标题|resolveTaskCardDisplayName|草稿星号" docs/experience-index.md docs/e2e-rules.md` -> PASS。
- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-edhr-batch-card-title-draft-marker --mode preview` -> PASS, keep task docs/evidence, delete `<none>`, blocked `<none>`。
- Cleanup apply: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-edhr-batch-card-title-draft-marker --mode apply` -> PASS, deleted_paths `<none>`。
- Diff check: `git diff --check -- <task-owned frontend/test/docs paths>` -> PASS。

## Blockers

- Real E2E blocker: 本地前端和后端端口已监听，但缺少真实 Playwright 登录所需 `EDHR_COMPANION_E2E_PASSWORD` 与浏览器路径 `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH`。影响：无法走真实批次详情页面确认实际卡片标题、草稿 `*` 和控制台错误；未使用 API-only 作为替代验收。
- Push/remote status pending: 当前分支在任务开始前已与 `origin/int_main` 分叉；提交后需要尝试 `git push origin int_main` 并记录结果。
