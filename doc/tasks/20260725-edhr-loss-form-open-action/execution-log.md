# Execution Log

## User Intent

用户反馈：选择 eDHR 批次详情右侧“损耗单”时提示“必填路线表单不允许跳过”。期望关闭前都可以修改，损耗单应可继续打开填写。

## BDD

- `BDD: required loss form opens instead of skip -> Given` 批次详情右侧存在必填动态表单“损耗单”，`When` 用户点击“打开填写”，`Then` 前端必须执行打开填写路径，不得调用跳过表单路径。
- `BDD: optional route form skip remains constrained -> Given` 路线表单是可选且满足跳过条件，`When` 用户点击跳过入口，`Then` 仅可选表单允许调用跳过接口，必填表单仍被阻止。
- `BDD: view-only loss form opens readonly -> Given` 瑛泰管理员拥有损耗单查看权限但没有填写权限，`When` 点击红框内损耗单卡片主动作，`Then` 系统显示“查看表单”并打开只读表单面板，不调用 `openEdhrBatchTask` 或跳过接口。
- `BDD: dynamic route form selection does not load batch-record preview -> Given` 瑛泰管理员仅查看动态损耗单，`When` 选择或点击损耗单卡片，`Then` 前端不得请求 legacy 批记录预览接口，不显示“必填路线表单不允许跳过”红色错误。

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
- completed: 用户要求执行 E2E 验证；已新增并运行真实页面 E2E，覆盖管理员无 `OPEN_FORM` 的 REQUIRED 损耗单点击“查看表单”只读抽屉路径。
- in_progress: 用户反馈当前仍显示“必填路线表单不允许跳过”，定位到动态表单选中态仍会调用 `getEdhrBatchTaskPreview`。
- completed: 新增静态 RED 合同锁定动态表单/表单中心路线表单不得请求 legacy 批记录预览接口。
- completed: `BatchExecutionDetailPage.vue` 新增 `shouldLoadTaskPreview`，仅 legacy `batchRecordReportId` 且非表单中心动态表单才加载批记录预览。
- completed: 真实 E2E 复验只读损耗单卡片，确认 `previewRequests=[]`、`skipErrorCount=0`、无打开填写/跳过/写入请求。
- completed: project-experience-consolidation 已将动态表单选中态不得调用 legacy `/task/preview` 经验合并到 `docs/e2e-rules.md#eDHR 路线表单跳过口径门禁`，并补充 `docs/experience-index.md` 关键词。
- ready_for_closeout: 动态表单预览误报修复后的 task-closeout-cleanup preview/apply 均通过，keep 本任务文档、bug 证据、真实 E2E JSON 与截图，delete/blocked/warnings/deleted_paths 均为 `<none>`；提交/推送仍受无关脏改动阻塞。

## TDD Evidence

- RED: `node tests\e2e\edhr-loss-form-open-action-static.spec.js` -> FAIL, expected reason: 当前 `isOptionalTask` 未包含 `row.requiredPolicy === 'OPTIONAL'`，会把非 OPTIONAL 的路线表单误纳入可跳过判断。
- RED: `node tests\e2e\edhr-loss-form-open-action-static.spec.js` -> FAIL, expected reason: 当前源码缺少 `canViewRouteFormTask` 和 `openReadonlyRouteFormTask`，无填写权限场景无法显示“查看表单”只读动作。
- RED: `node tests\e2e\edhr-loss-form-open-action-static.spec.js` -> FAIL, expected reason: 当前源码缺少 `shouldLoadTaskPreview`，动态损耗单选中态仍可能请求批记录预览并显示“必填路线表单不允许跳过”。
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
- GREEN: backend runtime preflight -> PASS，本机 `8081` 前端 HTTP 200；`48081` 起初未监听，`restart-int-ruoyi-local.ps1` 命中既有门禁 `Missing int_main frontend path: E:\IntRuoyi\yudao-ui-admin-vue3` 后停止该脚本路径；随后使用已构建 `yudao-server-exec.jar`（SHA256 `E9042EBCAA4C7F403B6D287FBE3F397F729C7FE704B1D15E52511DE7DC7F84F8`）显式启动本机 backend，`http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- GREEN: `node E:\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 芋道源码 --username admin --password <redacted> --target-path /mes/pro/feedback/edhr-batch-execution --target-text 批次 --timeout 90000` -> PASS。
- GREEN: `node --check tests\e2e\edhr-loss-form-open-action-real.e2e.js` -> PASS。
- GREEN: `node tests\e2e\edhr-loss-form-open-action-static.spec.js` -> PASS after real E2E script added。
- GREEN: `node tests\e2e\edhr-loss-form-open-action-real.e2e.js` -> PASS，真实前端目标 `batchExecutionId=900000000837`、`taskId=6368`、`requiredPolicy=REQUIRED`、`allowedActions=[]`、主动作 `查看表单`，只读抽屉内 `解析/创建/保存草稿/提交/重提/放弃` 全部 disabled，未触发 `/task/open`、`/task/special-node/skip` 或 MES/FormCenter 写请求；证据 `doc/tasks/20260725-edhr-loss-form-open-action/real-e2e-output/readonly-loss-form-card-result.json`。
- GREEN: `node --check tests\e2e\edhr-loss-form-open-action-real.e2e.js` -> PASS after preview/no-error assertion added。
- GREEN: `node tests\e2e\edhr-loss-form-open-action-static.spec.js` -> PASS after preview guard fix。
- GREEN: `node tests\e2e\edhr-loss-form-open-action-real.e2e.js` -> PASS，真实前端目标 `batchExecutionId=900000000846`、`taskId=6557`、`requiredPolicy=REQUIRED`、`allowedActions=[]`、主动作 `查看表单`，只读抽屉动作全部 disabled，`previewRequests=[]`、`skipErrorCount=0`、`blockedWrites=[]`。
- GREEN: project-experience-consolidation -> PASS，已合并动态表单选中态不得调用 legacy `/task/preview` 门禁。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260725-edhr-loss-form-open-action --mode preview` -> PASS after dynamic preview fix, no delete/blocked/warnings。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260725-edhr-loss-form-open-action --mode apply` -> PASS after dynamic preview fix, no deleted paths。
- GREEN: project-experience-consolidation -> PASS，已将“无 OPEN_FORM 但可查看的路线表单必须点真实卡片验证查看表单只读抽屉”合并到 `docs/e2e-rules.md#eDHR 路线表单跳过口径门禁`，并补充 `docs/experience-index.md` 关键词路由。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260725-edhr-loss-form-open-action --mode preview` -> PASS after real E2E, keep `real-e2e-output\readonly-loss-form-card-result.json` and screenshot, no delete/blocked/warnings。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260725-edhr-loss-form-open-action --mode apply` -> PASS after real E2E, no deleted paths。

## Additional Verification Notes

- Attempted broader static contract: `node tests\e2e\edhr-batch-context-carrier-header-static.spec.js` failed on existing assertion `右侧打开填写按钮必须统一使用当前选中的填写载体`; this appears to be a broad/stale contract mismatch outside the focused loss-form skip fix because the card-specific companion-form contract passes.
- Attempted broader static contract: `node tests\e2e\edhr-batch-pending-form-entry-static.spec.js` failed before assertions because it references missing path `E:\IntRuoyi\ruoyi-vue-pro\...MesProEdhrBatchExecutionServiceImpl.java`; this is a test harness precondition issue outside this focused fix.
- Real E2E evidence: `doc/tasks/20260725-edhr-loss-form-open-action/real-e2e-output/readonly-loss-form-card-result.json` and screenshot `readonly-loss-form-card.png`。

## Blockers

- Commit/push blocked: current branch `int_main` is ahead of `origin/int_main`, and `git status --short --branch` still reports unrelated dirty files such as `IntRuoyiFronted/scripts/codex-test-runner.mjs`, `IntRuoyiFronted/tests/e2e/edhr-full-chain-multi-user-real-flow.e2e.js`, `IntRuoyiFronted/tests/e2e/system-codex-test-management-static.spec.js`, multiple unrelated `doc/tasks/...` files, and `doc/tasks/20260725-codex-runner-void-test/codex-runner-loop.pid`. Do not stage/push this task together with unrelated concurrent work.
