# Execution Log

## User Intent

用户反馈：选择 eDHR 批次详情右侧“损耗单”时提示“必填路线表单不允许跳过”。期望关闭前都可以修改，损耗单应可继续打开填写。

## BDD

- `BDD: required loss form opens instead of skip -> Given` 批次详情右侧存在必填动态表单“损耗单”，`When` 用户点击“打开填写”，`Then` 前端必须执行打开填写路径，不得调用跳过表单路径。
- `BDD: optional route form skip remains constrained -> Given` 路线表单是可选且满足跳过条件，`When` 用户点击跳过入口，`Then` 仅可选表单允许调用跳过接口，必填表单仍被阻止。
- `BDD: view-only loss form opens readonly -> Given` 瑛泰管理员拥有损耗单查看权限但没有填写权限，`When` 点击红框内损耗单卡片主动作，`Then` 系统显示“查看表单”并打开只读表单面板，不调用 `openEdhrBatchTask` 或跳过接口。

## Milestone Updates

- in_progress: 创建任务记录，准备读取经验门禁并定位源码。
- completed: 读取 `docs/experience-index.md` 命中 eDHR 动态表单、损耗单和静态合同门禁，并补入 `task.md`。
- completed: 定位到前端 `isOptionalTask` 把 `!isRequiredBatchRecordTask(row)` 作为可选/可跳过口径，和后端 `requiredPolicy == OPTIONAL` 的跳过规则不一致。
- completed: 新增 `IntRuoyiFronted/tests/e2e/edhr-loss-form-open-action-static.spec.js`，锁定必填损耗单不得因 `requiredFlag=false` 或非必填进度口径进入跳过路径。
- completed: 修改 `progress.ts` 增加 `isOptionalRouteFormTask`，并让 `BatchExecutionDetailPage.vue` 的 `isOptionalTask` 只认 `requiredPolicy === 'OPTIONAL'`。
- ready_for_closeout: 目标验证通过；最终提交/推送因当前分支已有未推送基线提交和其它任务残留脏文件阻塞。
- completed: project-experience-consolidation 将“eDHR 路线表单跳过口径门禁”沉淀到 `docs/e2e-rules.md`，并在 `docs/experience-index.md` 增加关键词路由。
- completed: task-closeout-cleanup preview/apply 均通过，keep `task.md`、`execution-log.md`、`verification-report.md`，delete/blocked/warnings 均为 `<none>`。
- blocker: `int_main` 当前已领先 `origin/int_main`，且仍有其它任务脏文件；按提交/推送门禁，本任务不能标记 `completed`。
- in_progress: 用户补充“瑛泰管理员无损耗单填写权限但有查看权限”，追加分析并准备实现只读查看入口。
- completed: 新增只读查看分支：无 `OPEN_FORM` 但有可见路线表单时，主动作显示“查看表单”，动态表单打开只读抽屉，并禁用表单中心动作面板。
- ready_for_closeout: 只读查看扩展验证通过，等待提交/推送门禁解除。
- completed: 只读查看扩展后的 task-closeout-cleanup preview/apply 均通过，keep 三份任务文档，delete/blocked/warnings/deleted_paths 均为 `<none>`。

## TDD Evidence

- RED: `node tests\e2e\edhr-loss-form-open-action-static.spec.js` -> FAIL, expected reason: 当前 `isOptionalTask` 未包含 `row.requiredPolicy === 'OPTIONAL'`，会把非 OPTIONAL 的路线表单误纳入可跳过判断。
- RED: `node tests\e2e\edhr-loss-form-open-action-static.spec.js` -> FAIL, expected reason: 当前源码缺少 `canViewRouteFormTask` 和 `openReadonlyRouteFormTask`，无填写权限场景无法显示“查看表单”只读动作。
- GREEN: `node tests\e2e\edhr-loss-form-open-action-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-batch-process-companion-forms-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-pre-release-editable-submit-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-batch-detail-open-task-worktaskid-static.spec.js` -> PASS。
- GREEN: `git diff --check -- <task-owned paths>` -> PASS。
- GREEN: `python -X utf8 -c "<read task-owned files as UTF-8>"` -> PASS, output `UTF8_READ_OK`。
- GREEN: `rg -n "必填路线表单不允许跳过|edhr-路线表单跳过口径门禁|requiredPolicy OPTIONAL" ...` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260725-edhr-loss-form-open-action --mode preview` -> PASS, no delete/blocked/warnings。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260725-edhr-loss-form-open-action --mode apply` -> PASS, no deleted paths。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260725-edhr-loss-form-open-action --mode preview` -> PASS after view-only extension, no delete/blocked/warnings。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260725-edhr-loss-form-open-action --mode apply` -> PASS after view-only extension, no deleted paths。

## Additional Verification Notes

- Attempted broader static contract: `node tests\e2e\edhr-batch-context-carrier-header-static.spec.js` failed on existing assertion `右侧打开填写按钮必须统一使用当前选中的填写载体`; this appears to be a broad/stale contract mismatch outside the focused loss-form skip fix because the card-specific companion-form contract passes.
- Attempted broader static contract: `node tests\e2e\edhr-batch-pending-form-entry-static.spec.js` failed before assertions because it references missing path `E:\IntRuoyi\ruoyi-vue-pro\...MesProEdhrBatchExecutionServiceImpl.java`; this is a test harness precondition issue outside this focused fix.

## Blockers

- Commit/push blocked: current branch `int_main` is ahead of `origin/int_main`, and `git status --short --branch` still reports unrelated dirty files such as `IntRuoyiFronted/scripts/codex-test-runner.mjs`, `IntRuoyiFronted/tests/e2e/edhr-full-chain-multi-user-real-flow.e2e.js`, `IntRuoyiFronted/tests/e2e/system-codex-test-management-static.spec.js`, multiple unrelated `doc/tasks/...` files, and `doc/tasks/20260725-codex-runner-void-test/codex-runner-loop.pid`. Do not stage/push this task together with unrelated concurrent work.
