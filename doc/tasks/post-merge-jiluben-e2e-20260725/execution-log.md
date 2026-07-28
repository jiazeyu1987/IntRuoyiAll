# Execution Log

## 2026-07-25

- User intent: 用户要求在融合后进行 E2E 验证，并解决验证过程中遇到的问题。
- Pre-existing workspace state: 开始时 `docs/experience-index.md`、`docs/local-runtime.md`、`doc/tasks/20260725-start-local-frontend-backend/` 以及后续出现的其它任务目录不是本任务产物；本任务不修改、不提交这些无关项。
- Skills read: `playwright`, `bug-regression-fix-loop`, `bug-regression-fix-loop/references/bug-contract.md`。
- Rules read: `docs\task-closeout-rules.md`, `docs\e2e-rules.md`, `docs\local-runtime.md`, `docs\login-access.md`, `docs\worktree-restrictions.md`, `docs\branch-runtime-ports.md`, `docs\powershell-encoding.md`, `docs\powershell-memory.md`, `docs\frontend-development.md`。
- Preflight: `npx --version` -> PASS, 11.6.2。
- Runtime: `48081` health -> PASS；本机默认配置命中 `127.0.0.1:3306` root 权限问题，按既有本地运行证据用 JVM 参数临时指向 Docker MySQL `23306` 和 Redis `26379` 启动后端，未改配置文件。
- BDD: 融合后 eDHR 详情真实页面可访问 -> Given `int_main` 前后端在 8081/48081 运行 / When 用户进入 eDHR 批次执行详情 / Then 页面应加载真实接口数据且不出现融合后的前端运行时错误。
- BDD: 历史执行只读追踪返回上下文 -> Given 已存在批次执行和执行记录 / When 管理员只读账号从批次详情进入执行记录 tracking 路径 / Then 页面显示追踪详情并能带 `batchTaskId` 返回批次详情。
- BDD: 填写页 toolbar 不受 tracking 模式限制 -> Given 用户通过正式打开工序逻辑进入填写页 / When `isTrackingReadonlyMode=false` / Then 页面仍显示标题和返回按钮，不被 tracking 条件隐藏。
- RED: `node tests\e2e\edhr-batch-process-companion-forms-real.e2e.js`（管理员只读，结构化模式）-> FAIL, `.edhr-page-shell__title` 等待超时；根因是 `ExecutionPage.vue` toolbar 被 `v-if="isTrackingReadonlyMode"` 包住，填写模式标题/返回按钮被隐藏。
- RED: 同一 E2E 补齐标题后继续 FAIL, 填写工作区提示“当前用户不是该 eDHR 工作任务责任人”或“非当前活动表单”；根因是只读脚本手写旧 `executionId` 填写 URL，绕过正式 `openEdhrBatchTask` POST 语义，历史执行记录应走 tracking 只读路径。
- RED: 测试租户责任账号 tracking 复验 -> FAIL, `BATCH_RECORD_EXECUTION:1276:VIEW` 对象级权限不足；管理员只读模式是历史执行记录审计的正式前置。
- RED: `node tests\e2e\edhr-batch-process-companion-forms-static.spec.js` -> FAIL, 静态合同仍要求模板直拼 `resolveTaskSlotBlocker(task) || task.disabledReason || task.gateMessage` 和旧 batchTaskId 片段；当前实现已集中到 `resolveTaskGateText` 与 `readRouteQueryString`。
- Fix: `ExecutionPage.vue` 移除 toolbar 上的 tracking-only 条件，让填写模式和 tracking 模式都显示标题/返回按钮。
- Fix: `edhr-batch-process-companion-forms-real.e2e.js` 排除特殊节点工序组、同步必填任务口径、重载结构化候选详情，并将历史执行返回上下文改为正式 `viewMode=tracking` 只读路径。
- Fix: `edhr-open-process-form-route-static.spec.js` 增加 toolbar 不得只在 tracking 模式显示的静态回归断言。
- Fix: `edhr-batch-process-companion-forms-static.spec.js` 同步当前 `resolveTaskGateText` 和 `readRouteQueryString` 返回上下文合同。
- GREEN: `node tests\e2e\edhr-batch-process-companion-forms-real.e2e.js`（管理员只读，结构化模式；环境变量只记录标签不记录密码）-> PASS, `STRUCTURAL_ONLY`，batch `900000000505`，routeProcessId `923557`，无 MES 写请求。
- GREEN: `node tests\e2e\edhr-open-process-form-route-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-batch-process-companion-forms-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\edhr-batch-detail-hide-red-box-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\mes-edhr-batch-review-signoff-summary-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\edhr-recordbook-batch-sync-static.spec.js` -> PASS。
- REGRESSION: `node tests\e2e\edhr-batch-process-state-background-static.spec.js` -> PASS。
- REGRESSION: `node --check tests\e2e\edhr-batch-process-companion-forms-real.e2e.js` -> PASS。
- REGRESSION: `pnpm ts:check` -> PASS。
- REGRESSION: `git diff --check` -> PASS。
- REGRESSION: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS, `int_main` frontend 8081 / backend 48081。
- Current status: ready_for_closeout；待 cleanup preview/apply、经验沉淀、提交并推送本任务文件。- CLEANUP: `task_closeout.py --task-id post-merge-jiluben-e2e-20260725 --mode preview` -> PASS, delete candidates only task-owned temp logs and intermediate bug evidence.
- CLEANUP: first apply -> BLOCKED, `backend-48081.err.log` held by task-owned Java PID `34940`; stopped PID `34940` after confirming it listened on `48081`.
- CLEANUP: `task_closeout.py --task-id post-merge-jiluben-e2e-20260725 --mode apply` -> PASS, deleted task-owned temp logs and intermediate bug evidence.
- EXPERIENCE: Updated `docs/e2e-rules.md#eDHR 历史执行只读验证门禁` to prevent future direct old-execution fill URL misuse and clarify tracking readonly requirements.
- Current status: completed；待提交并推送本任务文件。