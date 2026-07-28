# Frontend Feature Evidence

## Goal

Change the test management execution UX from "must have an online Runner before clicking execute" to "click execute and let backend start the controlled Runner wrapper on demand".

## Non-Goals

- No new mock Runner.
- No direct frontend command execution.
- No API-only replacement for the real execution path.

## Entry Point

- Page: `IntRuoyiFronted/src/views/system/codex-test-management/index.vue`
- API wrapper: `IntRuoyiFronted/src/api/system/codexTestManagement/index.ts`
- Static contract: `IntRuoyiFronted/tests/e2e/system-codex-test-management-static.spec.js`

## Acceptance

- Execution click must not be blocked by stale/offline status preflight.
- Execution success must switch to monitor tab and start polling.
- Primary UI must not expose raw heartbeat wording.

## UI States

- Runner status strip now shows `可用 / 按需启动 / 启动中 / 配置异常 / 诊断失败`.
- The visible page no longer shows "最后心跳" or "没有在线 Codex Runner".
- After starting an execution, the page switches to the `运行监控` tab and starts monitor polling.
- Red target items still open the failed checkpoint reason dialog.

## BDD

- BDD: 运行监控展示步骤进度 -> Given Runner 正在执行测试方法第 N 项 When 监控页签刷新 Then N 之前的方法为绿色、第 N 项为黄色、后续保持待执行。
- BDD: 目标验证失败可查看原因 -> Given Runner 验证目标项失败并回写失败原因 When 用户在监控页签点击红色目标 Then 页面展示对应失败原因。

## RED:

- `node -e "const cp=require('node:child_process'); const page=cp.execSync('git show HEAD:IntRuoyiFronted/src/views/system/codex-test-management/index.vue',{encoding:'utf8'}); if (/blockExecutionWhenRunnerStatusUnavailable|runnerLastHeartbeatText/.test(page)) { console.error('RED: baseline still blocks on runner status or exposes heartbeat diagnostics'); process.exit(1); }"` -> FAIL, old baseline still blocked on Runner status and exposed heartbeat diagnostics.

## GREEN:

- `node tests\e2e\system-codex-test-management-static.spec.js` -> PASS.

## Verification

- Static contract verifies on-demand execution path and hidden heartbeat wording.

## Regression

- `pnpm ts:check` -> FAIL unrelated blocker: `src/views/mes/pro/route/RouteEditPage.vue(429,5): error TS2304: Cannot find name 'suppressRouteVersionSubmitAfterSaveOnce'.`
- The failure is outside the test management files changed in this task.

## Blockers

- No live Playwright browser E2E was run because this task did not restart the local frontend/backend runtime.
