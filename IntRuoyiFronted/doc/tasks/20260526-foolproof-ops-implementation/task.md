# 任务：傻瓜式运维十项能力前端实现

## 任务目标

- 在新的前端 worktree 中配合后端实现运行控制台傻瓜式运维 UI。
- 所有前端实现必须以 `ruoyi-vue-pro/doc/tasks/20260526-foolproof-ops-implementation` 为主控任务。
- 前端不得绕过后端真实接口，不得为 E2E 添加临时控件或隐藏错误。
- 本前端目录只记录当前 paired worktree 的前端范围证据；根仓库或历史部署任务文档仅作为上下文，不能替代当前分支前后端 target、真实 rehearsal 或最终 DR 证据。

## Worktree

- 前端：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-foolproof-ops-implementation\yudao-ui-admin-vue3`
- 后端主控：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-foolproof-ops-implementation\ruoyi-vue-pro`
- 分支：`task/20260526-foolproof-ops-implementation`

## 里程碑

- [x] M1：创建新的前端实现 worktree。
- [x] M2：建立前端任务文档。
- [x] M3：T0 前端契约 RED 测试。
- [x] M4：T5 前端 API、组件和运行控制台集成。
- [x] M5：独立测试、真实路径 E2E、reviewer 放行。

## 预期验证

- `node tests/e2e/runtime-control-static.spec.js`
- `node tests/e2e/runtime-control-ops-static.spec.js`
- `node tests/e2e/runtime-control-foolproof-static.spec.js`
- `node --check tests/e2e/runtime-control-rollback-app.e2e.js`
- `node --check tests/e2e/runtime-control-restore-data.e2e.js`
- `node --check tests/e2e/runtime-control-real-dr-flow.e2e.js`
- `pnpm ts:check` with runtime-control scope review

## 当前状态

- 状态：completed / pass_with_scope_waiver
- 当前阶段：T6 runtime-control paired worktree 端口与 post-action health proof 显式 URL 门禁修复已完成，并随主控任务按用户 scope waiver 放行
- 放行范围：用户已明确允许“允许不执行真实 DR，仅按当前非破坏性证据放行。”真实高危 DR 串联提交未执行，缺少 `RUNTIME_CONTROL_ALLOW_REAL_DR=1`、显式 current-code 前后端目标、四个显式 post-action health proof URL、已演练恢复候选 `RUNTIME_CONTROL_REAL_DR_RESTORE_BACKUP_ID` 和真实 DR 执行证据；这些事项已转为后续残余风险，不能声明真实 DR 已验证。
- 状态文件：前端目录保留独立 `task-state.json` 作为前端范围镜像；最终整体状态仍以后端主控目录 `ruoyi-vue-pro/doc/tasks/20260526-foolproof-ops-implementation/task-state.json` 为准。

## 2026-05-26 Worker 子任务：runtime-control paired worktree health target 门禁

- 目标：runtime-control 证据不得使用旧端口或硬编码旧健康目标；前端本地配置必须指向 paired worktree 端口，publish/DR post-action health proof 必须来自显式环境变量并在缺失时 fail-fast。
- 范围：仅修改 `.env.local`、`tests/e2e/runtime-control-foolproof-static.spec.js`、`tests/e2e/runtime-control-publish-test-real-flow.e2e.js`、`tests/e2e/runtime-control-real-dr-flow.e2e.js` 与本任务文档。
- 里程碑：
  - [x] H1：静态测试新增 paired worktree 端口与 health URL 合同并先失败。
  - [x] H2：`.env.local` 改为 `VITE_BASE_URL/VITE_PROXY_TARGET=http://127.0.0.1:48098`、`VITE_PORT=8098`。
  - [x] H3：publish/DR 脚本改为显式 `RUNTIME_CONTROL_TEST_BACKEND_HEALTH_URL`、`RUNTIME_CONTROL_TEST_FRONTEND_URL`、`RUNTIME_CONTROL_TEST_WEBSITE_URL`、`RUNTIME_CONTROL_TEST_SHOWROOM_URL`，缺失 fail-fast，`HEALTH_OK` 输出包含实际 URL。
  - [x] H4：静态测试与语法检查通过，真实发布/DR 不执行，不提交。
- 预期验证：
  - `node tests/e2e/runtime-control-foolproof-static.spec.js`
  - `node --check tests/e2e/runtime-control-publish-test-real-flow.e2e.js`
  - `node --check tests/e2e/runtime-control-real-dr-flow.e2e.js`
- 最终验证：以上命令均已通过；只读扫描确认 `.env.local`、publish real-flow、real DR flow 无旧 `172.30.30.58:48081/8081/8083` health proof 或旧 `48081/8081` 本地端口；真实发布/DR 未执行；按用户要求未提交。

## 2026-05-26 Worker 子任务：runtime-control E2E 显式目标门禁

- 目标：runtime-control 相关 E2E 脚本不得默认使用旧远端或固定本地 origin 作为证据；必须显式要求 `RUNTIME_CONTROL_E2E_BASE_URL` 和需要时的 `RUNTIME_CONTROL_E2E_ACTION_ORIGIN`。
- 范围：仅修改 `tests/e2e/runtime-control-foolproof-static.spec.js`、`tests/e2e/runtime-control-ops-e2e-helper.js`、`tests/e2e/runtime-control-publish-test-submit-route.e2e.js`、`tests/e2e/runtime-control-publish-test-real-flow.e2e.js`、`tests/e2e/runtime-control-promote-prod-real-flow.e2e.js` 与本任务文档。
- 里程碑：
  - [x] W1：静态测试新增显式目标门禁断言并先失败。
  - [x] W2：E2E helper 与发布/提升脚本移除旧远端/固定本地默认目标。
  - [x] W3：静态测试与语法检查通过，真实发布/提升/DR 不执行。
- 预期验证：
  - `node tests/e2e/runtime-control-foolproof-static.spec.js`
  - `node --check tests/e2e/runtime-control-ops-e2e-helper.js`
  - `node --check tests/e2e/runtime-control-publish-test-submit-route.e2e.js`
  - `node --check tests/e2e/runtime-control-publish-test-real-flow.e2e.js`
  - `node --check tests/e2e/runtime-control-promote-prod-real-flow.e2e.js`
- 最终验证：以上命令均已通过；真实发布、提升正式服、真实 DR 均未执行。

## Cleanup Keep

- `doc/tasks/20260526-foolproof-ops-implementation/task.md`
- `doc/tasks/20260526-foolproof-ops-implementation/test-plan.md`
- `doc/tasks/20260526-foolproof-ops-implementation/execution-log.md`
- `doc/tasks/20260526-foolproof-ops-implementation/test-report.md`
- `doc/tasks/20260526-foolproof-ops-implementation/review-report.md`
- `doc/tasks/20260526-foolproof-ops-implementation/verification-report.md`
- `doc/tasks/20260526-foolproof-ops-implementation/task-state.json`
