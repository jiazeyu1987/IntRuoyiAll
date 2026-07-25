# Execution Log

## User Intent

用户反馈：选择 eDHR 批次详情右侧“损耗单”时提示“必填路线表单不允许跳过”。期望关闭前都可以修改，损耗单应可继续打开填写。

## BDD

- `BDD: required loss form opens instead of skip -> Given` 批次详情右侧存在必填动态表单“损耗单”，`When` 用户点击“打开填写”，`Then` 前端必须执行打开填写路径，不得调用跳过表单路径。
- `BDD: optional route form skip remains constrained -> Given` 路线表单是可选且满足跳过条件，`When` 用户点击跳过入口，`Then` 仅可选表单允许调用跳过接口，必填表单仍被阻止。

## Milestone Updates

- in_progress: 创建任务记录，准备读取经验门禁并定位源码。
- completed: 读取 `docs/experience-index.md` 命中 eDHR 动态表单、损耗单和静态合同门禁，并补入 `task.md`。
- completed: 定位到前端 `isOptionalTask` 把 `!isRequiredBatchRecordTask(row)` 作为可选/可跳过口径，和后端 `requiredPolicy == OPTIONAL` 的跳过规则不一致。
- completed: 新增 `IntRuoyiFronted/tests/e2e/edhr-loss-form-open-action-static.spec.js`，锁定必填损耗单不得因 `requiredFlag=false` 或非必填进度口径进入跳过路径。
- completed: 修改 `progress.ts` 增加 `isOptionalRouteFormTask`，并让 `BatchExecutionDetailPage.vue` 的 `isOptionalTask` 只认 `requiredPolicy === 'OPTIONAL'`。
- ready_for_closeout: 目标验证通过；最终提交/推送因当前分支已有未推送基线提交和其它任务残留脏文件阻塞。
- completed: project-experience-consolidation 将“eDHR 路线表单跳过口径门禁”沉淀到 `docs/e2e-rules.md`，并在 `docs/experience-index.md` 增加关键词路由。

## TDD Evidence

- RED: `node tests\e2e\edhr-loss-form-open-action-static.spec.js` -> FAIL, expected reason: 当前 `isOptionalTask` 未包含 `row.requiredPolicy === 'OPTIONAL'`，会把非 OPTIONAL 的路线表单误纳入可跳过判断。
- GREEN: `node tests\e2e\edhr-loss-form-open-action-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-batch-process-companion-forms-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-pre-release-editable-submit-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-batch-detail-open-task-worktaskid-static.spec.js` -> PASS。
- GREEN: `git diff --check -- <task-owned paths>` -> PASS。
- GREEN: `python -X utf8 -c "<read task-owned files as UTF-8>"` -> PASS, output `UTF8_READ_OK`。

## Additional Verification Notes

- Attempted broader static contract: `node tests\e2e\edhr-batch-context-carrier-header-static.spec.js` failed on existing assertion `右侧打开填写按钮必须统一使用当前选中的填写载体`; this appears to be a broad/stale contract mismatch outside the focused loss-form skip fix because the card-specific companion-form contract passes.
- Attempted broader static contract: `node tests\e2e\edhr-batch-pending-form-entry-static.spec.js` failed before assertions because it references missing path `E:\IntRuoyi\ruoyi-vue-pro\...MesProEdhrBatchExecutionServiceImpl.java`; this is a test harness precondition issue outside this focused fix.

## Blockers

- Commit/push blocked: current branch `int_main` is already ahead of `origin/int_main` by 2 commits, and `git status --short --branch` still reports unrelated dirty files (`IntRuoyiBackend/yudao-module-mes/.../MesProBatchRecordReportLayoutCalibrator.java`, `MesProBatchRecordSharedRowTypeRules.java`, and `doc/tasks/20260725-codex-runner-void-test/codex-runner-loop.pid`). Do not stage/push this task together with unrelated concurrent work.
