# Frontend Feature Evidence

## Scope

- `系统管理 > 测试管理` Runner status strip.
- Execute-time Runner status refresh.
- API wrapper `getCodexTestRunnerStatus()`.

## Behavior

- Shows Runner status, diagnostic message, and last heartbeat age.
- Refreshes Runner status before starting sequential, parallel, or single-case execution.
- If the status endpoint is unavailable in an older runtime, the page shows inline diagnostic text and leaves the backend start endpoint as the authoritative validation path.

## Verification

- RED: static contract failed before API/UI/status integration.
- GREEN: `node tests\e2e\system-codex-test-management-static.spec.js` passed.