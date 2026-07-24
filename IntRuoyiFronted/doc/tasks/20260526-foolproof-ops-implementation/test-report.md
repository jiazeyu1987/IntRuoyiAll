# Test Report

## 当前状态

- 状态：pass_with_scope_waiver / worker-runtime-health-target-fix-completed

## 2026-05-26 Worker paired worktree health target 门禁

- `.env.local` -> PASS；`VITE_BASE_URL=http://127.0.0.1:48098`、`VITE_PROXY_TARGET=http://127.0.0.1:48098`、`VITE_PORT=8098`。
- `node tests/e2e/runtime-control-foolproof-static.spec.js` -> PASS；输出 `PASS: runtime control foolproof canonical API, components, candidate-only, paired-port, and explicit health proof contracts are wired`。
- `node --check tests/e2e/runtime-control-publish-test-real-flow.e2e.js` -> PASS。
- `node --check tests/e2e/runtime-control-real-dr-flow.e2e.js` -> PASS。
- static source scan over `.env.local`, `runtime-control-publish-test-real-flow.e2e.js`, and `runtime-control-real-dr-flow.e2e.js` -> PASS；未发现旧 `172.30.30.58:48081/8081/8083` health proof 或旧本地 `48081/8081` 端口。
- 本次 worker 未运行真实发布/DR，未提交 Git。
- task-closeout-cleanup preview -> BLOCKED；缺少 `master` 主分支已检出的 worktree，preview delete 为 `<none>`，未执行 apply。

## 已通过

- `node tests/e2e/runtime-control-foolproof-static.spec.js` -> PASS after worker explicit-target gate fix.
- `node --check tests/e2e/runtime-control-ops-e2e-helper.js` -> PASS after worker explicit-target gate fix.
- `node --check tests/e2e/runtime-control-publish-test-submit-route.e2e.js` -> PASS.
- `node --check tests/e2e/runtime-control-publish-test-real-flow.e2e.js` -> PASS.
- `node --check tests/e2e/runtime-control-promote-prod-real-flow.e2e.js` -> PASS.
- static source scan over allowed executable scripts -> PASS；未发现 `DEFAULT_BASE_URL`、`process.env.RUNTIME_CONTROL_E2E_BASE_URL ||`、`process.env.RUNTIME_CONTROL_E2E_ACTION_ORIGIN ||`、旧远端 action origin fallback 或固定本地 action origin fallback。
- `$env:RUNTIME_CONTROL_E2E_ACTION_ORIGIN='http://127.0.0.1:48098'; node tests\e2e\runtime-control-publish-test-submit-route.e2e.js` -> FAIL-FAST as expected, missing `RUNTIME_CONTROL_E2E_BASE_URL`; no browser operation was submitted.
- `$env:RUNTIME_CONTROL_E2E_BASE_URL='http://127.0.0.1:8098'; node tests\e2e\runtime-control-publish-test-submit-route.e2e.js` -> FAIL-FAST as expected, missing `RUNTIME_CONTROL_E2E_ACTION_ORIGIN`; no browser operation was submitted.
- read-only reviewer `019e6497-b7a4-7c11-89e7-9b9d457da1e6` -> backend high-risk fallback/default-success/unknown-operator check clean; frontend explicit-target gate passed after docs update.
- `node tests/e2e/runtime-control-static.spec.js` -> PASS
- `node tests/e2e/runtime-control-ops-static.spec.js` -> PASS
- `node tests/e2e/runtime-control-foolproof-static.spec.js` -> PASS
- `node --check tests/e2e/runtime-control-rollback-app.e2e.js` -> PASS
- `node --check tests/e2e/runtime-control-restore-data.e2e.js` -> PASS
- `node --check tests/e2e/runtime-control-real-dr-flow.e2e.js` -> PASS
- `runtime-control-real-dr-flow.e2e.js` explicit-target static contract -> PASS as part of `runtime-control-foolproof-static.spec.js`; no old remote frontend/backend defaults remain.
- `runtime-control-real-dr-flow.e2e.js` explicit restore candidate contract -> PASS as part of `runtime-control-foolproof-static.spec.js`; the script requires `RUNTIME_CONTROL_REAL_DR_RESTORE_BACKUP_ID` and does not reuse the just-created backup before rehearsal.
- `NODE_OPTIONS=--max-old-space-size=16384 pnpm ts:check` -> PASS, full command exited 0
- Historical candidate-picker evidence below used the then-running local frontend `http://127.0.0.1:8081`; it proves the candidate-only UI path at that time only. Current paired worktree evidence and any future E2E command must use explicit `RUNTIME_CONTROL_E2E_BASE_URL=http://127.0.0.1:8098` and `RUNTIME_CONTROL_E2E_ACTION_ORIGIN=http://127.0.0.1:48098` or an equivalent current-branch deployment.
- `RUNTIME_CONTROL_E2E_BASE_URL=http://127.0.0.1:8081 RUNTIME_CONTROL_E2E_TENANT=测试租户 RUNTIME_CONTROL_E2E_USERNAME=aoteman RUNTIME_CONTROL_E2E_PASSWORD=admin123 node tests\e2e\runtime-control-rollback-app.e2e.js` -> PASS
- `RUNTIME_CONTROL_E2E_BASE_URL=http://127.0.0.1:8081 RUNTIME_CONTROL_E2E_TENANT=测试租户 RUNTIME_CONTROL_E2E_USERNAME=aoteman RUNTIME_CONTROL_E2E_PASSWORD=admin123 node tests\e2e\runtime-control-restore-data.e2e.js` -> PASS
- `RUNTIME_CONTROL_E2E_BASE_URL=http://127.0.0.1:8081 RUNTIME_CONTROL_E2E_TENANT=芋道源码 RUNTIME_CONTROL_E2E_USERNAME=admin RUNTIME_CONTROL_E2E_PASSWORD=admin123 node tests\e2e\runtime-control-rollback-app.e2e.js` -> PASS, read-only verification only
- `RUNTIME_CONTROL_E2E_BASE_URL=http://127.0.0.1:8081 RUNTIME_CONTROL_E2E_TENANT=芋道源码 RUNTIME_CONTROL_E2E_USERNAME=admin RUNTIME_CONTROL_E2E_PASSWORD=admin123 node tests\e2e\runtime-control-restore-data.e2e.js` -> PASS, read-only verification only

## 已豁免残余风险

- 用户已明确允许 `允许不执行真实 DR，仅按当前非破坏性证据放行。`
- 本次 worker 未执行真实发布、提升正式服或真实 DR；该事项不再阻塞本次前端镜像放行，但不能声明真实 DR 已验证。
- Real destructive DR flow -> not executed. `node tests\e2e\runtime-control-real-dr-flow.e2e.js` fails fast unless `RUNTIME_CONTROL_E2E_BASE_URL`, `RUNTIME_CONTROL_E2E_ACTION_ORIGIN`, `RUNTIME_CONTROL_ALLOW_REAL_DR=1`, `RUNTIME_CONTROL_REAL_DR_ROLLBACK_TAG`, and `RUNTIME_CONTROL_REAL_DR_RESTORE_BACKUP_ID` are set. This prevents unapproved real backup/restore/rollback submission, prevents old remote deployments from being mistaken for current worktree evidence, and prevents restoring an unverified backup.
- `task-closeout-cleanup --mode preview` -> FAIL；缺少前置条件：未找到 `master` 主分支已检出的 worktree；影响：不能执行 worktree closeout apply。本次 worker 预览显示 delete 为 `<none>`，keep 为 `task.md`、`execution-log.md`、`test-report.md`。
- Historical independent reviewer `019e63bc-5aaf-72a0-b240-2d72bc408f28` -> pre-waiver `final_decision=fail` because real destructive DR flow had not been executed. After the 2026-05-27 user scope waiver, frontend mirror release status follows backend main task as `PASS_WITH_SCOPE_WAIVER` while keeping real DR as residual risk.

## 真实 DR 执行准备

- run directory: `D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-foolproof-ops-implementation\yudao-ui-admin-vue3`.
- command requires explicit approval and env:

```powershell
$env:RUNTIME_CONTROL_ALLOW_REAL_DR='1'
$env:RUNTIME_CONTROL_REAL_DR_ROLLBACK_TAG='20260524_035800'
$env:RUNTIME_CONTROL_REAL_DR_RESTORE_BACKUP_ID='<已演练可恢复备份点>'
$env:RUNTIME_CONTROL_E2E_BASE_URL='http://127.0.0.1:8098'
$env:RUNTIME_CONTROL_E2E_ACTION_ORIGIN='http://127.0.0.1:48098'
$env:RUNTIME_CONTROL_TEST_BACKEND_HEALTH_URL='http://127.0.0.1:48098/actuator/health'
$env:RUNTIME_CONTROL_TEST_FRONTEND_URL='http://127.0.0.1:8098/'
$env:RUNTIME_CONTROL_TEST_WEBSITE_URL='<当前测试服或本地网站根地址>'
$env:RUNTIME_CONTROL_TEST_SHOWROOM_URL='<当前测试服或本地展厅地址>'
$env:RUNTIME_CONTROL_E2E_TENANT='测试租户'
$env:RUNTIME_CONTROL_E2E_USERNAME='aoteman'
$env:RUNTIME_CONTROL_E2E_PASSWORD='admin123'
node tests\e2e\runtime-control-real-dr-flow.e2e.js
```

- pass evidence required: `BACKUP_ID`, restore operation success, rollback operation success, four `HEALTH_OK` checks with actual URLs, and `PASS: runtime control real test-server backup restore rollback flow`.
- read-only rollback tag candidate: `20260524_035800`, from test-server backup point `/mnt/nas/备份/20260525-103432`; this candidate still requires explicit approval before running the real DR flow.
- current-code backend required: remote test frontend/backend cannot be used as final current-worktree evidence yet; remote rollback dialog still exposes old free-text `镜像标签`, and remote backend does not expose `/admin-api/infra/runtime-control/rollback-candidates`. The real DR flow must use current worktree frontend plus a current-code backend action origin, or a test deployment containing this worktree frontend/backend.
- restore candidate required: the read-only backup point `20260525-103432` can supply rollback tag `20260524_035800`, but it lacks `manifest/rehearsal-report.json` and `manifest/现场快照.md`, so it is not a valid restore candidate for the final real DR chain.

## Current-Code Backend Read-Only Evidence

- GREEN: local current-code backend `http://127.0.0.1:48098/actuator/health` -> PASS, `status=UP`.
- GREEN: test tenant login through the current-code backend -> PASS, `tenant-id=122`, `userId=113`.
- GREEN: `GET /admin-api/infra/runtime-control/rollback-candidates` on `http://127.0.0.1:48098` -> PASS, one available rollback candidate `rollback:20260525-103432`, `imageTag=20260524_035800`.
- GREEN: `GET /admin-api/infra/runtime-control/restore-candidates` on `http://127.0.0.1:48098` -> PASS, one blocked restore candidate `restore:20260525-103432`, blocked by missing rehearsal report and pre-restore snapshot.
- RESIDUAL RISK: this validates the current-code action origin for read-only candidate fetching only. It does not replace the final Linux destructive DR E2E evidence, which is waived for this release and must be补验 before claiming real DR readiness.

## 契约校准

- 前端未新增也未调用 `/infra/runtime-control/foolproof-overview`。该端点不在后端 `RuntimeControlController.java` 中，静态测试已改为要求组合真实 canonical endpoints。
- 回滚/恢复 UI 已移除手填版本/备份输入，提交 `/actions` 只使用 `selectedImageCandidateId` / `selectedBackupCandidateId`。
- 候选接口加载失败时清空候选并继续抛错，页面通过 `operationBlockReason` 阻断提交，避免沿用旧候选形成隐式 fallback。
- 默认 Playwright 登录已改为 `测试租户/aoteman`，并显式处理 Element Plus 租户下拉；测试租户缺 operate 权限时已先失败，后端主控任务只补测试租户权限后再通过。
## 2026-05-27 Final Scope Waiver Verification

- waiver: 用户明确授权 `允许不执行真实 DR，仅按当前非破坏性证据放行。`
- decision: `PASS_WITH_SCOPE_WAIVER`
- accepted evidence: 前端 runtime-control foolproof 静态合同、E2E helper 和真实流脚本语法检查、显式 paired worktree 目标门禁、显式 post-action health proof URL 门禁、候选-only 回滚/恢复交互证据。
- final frontend gate: `node tests\e2e\runtime-control-foolproof-static.spec.js` plus seven runtime-control `node --check` scripts -> PASS.
- not executed: 真实发布、提升正式服和真实 DR。
- residual risk: 真实 DR 未验证，后续声明生产级 DR readiness 前必须随主控任务补验。
