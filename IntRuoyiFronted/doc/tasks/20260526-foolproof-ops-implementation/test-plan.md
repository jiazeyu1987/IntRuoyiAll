# 前端 Test Plan：傻瓜式运维十项能力

## 适用范围

- 本文件是前端 worktree 的测试计划镜像。
- 最终整体测试计划以后端主控目录 `ruoyi-vue-pro/doc/tasks/20260526-foolproof-ops-implementation/test-plan.md` 为准。
- 前端不得使用旧端口、旧远端或固定本地 target 作为当前证据。

## 必跑验证

- `node tests/e2e/runtime-control-static.spec.js`
- `node tests/e2e/runtime-control-ops-static.spec.js`
- `node tests/e2e/runtime-control-foolproof-static.spec.js`
- `node --check tests/e2e/runtime-control-ops-e2e-helper.js`
- `node --check tests/e2e/runtime-control-publish-test-submit-route.e2e.js`
- `node --check tests/e2e/runtime-control-publish-test-real-flow.e2e.js`
- `node --check tests/e2e/runtime-control-promote-prod-real-flow.e2e.js`
- `node --check tests/e2e/runtime-control-real-dr-flow.e2e.js`
- `node --check tests/e2e/runtime-control-rollback-app.e2e.js`
- `node --check tests/e2e/runtime-control-restore-data.e2e.js`
- `pnpm ts:check`

## 当前 Target 门禁

- 当前 paired worktree 前端：`RUNTIME_CONTROL_E2E_BASE_URL=http://127.0.0.1:8098`
- 当前 paired worktree 后端：`RUNTIME_CONTROL_E2E_ACTION_ORIGIN=http://127.0.0.1:48098`
- publish/DR post-action health proof 必须显式传入：
  - `RUNTIME_CONTROL_TEST_BACKEND_HEALTH_URL`
  - `RUNTIME_CONTROL_TEST_FRONTEND_URL`
  - `RUNTIME_CONTROL_TEST_WEBSITE_URL`
  - `RUNTIME_CONTROL_TEST_SHOWROOM_URL`
- promote-prod real-flow 必须显式传入：
  - `RUNTIME_CONTROL_PROD_BACKEND_HEALTH_URL`
  - `RUNTIME_CONTROL_PROD_FRONTEND_URL`
  - `RUNTIME_CONTROL_PROD_WEBSITE_URL`
  - `RUNTIME_CONTROL_PROD_SHOWROOM_URL`
  - `RUNTIME_CONTROL_PROD_LOGIN_URL`
  - `RUNTIME_CONTROL_PROD_EXPECTED_BACKEND_ORIGIN`
  - `RUNTIME_CONTROL_PROD_FORBIDDEN_TEST_BACKEND_ORIGIN`

## 历史证据标注

- `http://127.0.0.1:8081` 的候选 E2E 结果只作为历史候选-only UI 证据。
- 当前放行证据不得把 `8081` 历史命令当作 paired worktree 当前 target。

## 真实 DR 后续补验

- 用户已明确允许 `允许不执行真实 DR，仅按当前非破坏性证据放行。`
- 本次未执行真实发布、提升正式服或真实 DR，前端镜像随主控任务按 `PASS_WITH_SCOPE_WAIVER` 放行。
- 后续若要声明真实 DR readiness，仍需用户明确批准、`RUNTIME_CONTROL_ALLOW_REAL_DR=1`、current-code Linux-capable action origin、已演练 `RUNTIME_CONTROL_REAL_DR_RESTORE_BACKUP_ID`、回滚候选 `20260524_035800` 在获批命令中显式设置并复核，以及四个带实际 URL 的 `HEALTH_OK`。
