# 任务：补齐备份服接管真实 E2E 门禁

## 任务目标

根据根任务 `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260604-dr-recovery-rollback-gap-audit` 的剩余缺口，补齐运行控制台 `promote-backup` 真实前端路径 E2E 脚本和静态门禁。脚本必须默认 fail-fast，不得在缺少本轮授权、目标 URL、健康检查 URL、DCC 读回 URL 或发布包恢复集绑定证据时提交备份服接管操作；授权后必须通过 Playwright 登录运行控制台、选择已测试发布包、提交“上线备份服务器”、等待操作日志成功，并记录备份服健康与 DCC 读回证据。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260604-dcc-nas-transfer-category-binding/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只补齐运行控制台备份服接管 E2E 门禁、前端类型字段和任务证据，不执行远程测试服或备份服操作。

## BDD 场景

- BDD: 备份服接管真实 E2E 必须显式授权 -> Given `promote-backup` 会修改备份服务器应用版本并可能覆盖数据 / When 未设置 `RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_BACKUP=1` / Then 脚本必须在打开浏览器或提交动作前失败。
- BDD: 备份服接管必须绑定恢复集 -> Given 发布包需要作为接管单元 / When E2E 选择发布包 / Then 发布包候选必须暴露 `testedRecoverySetCandidateId`、`testedRecoverySetId` 和 `testedRecoverySetManifestHash`，缺失即阻塞。
- BDD: 接管后必须验证备份服与 DCC 读回 -> Given 备份服接管操作成功 / When E2E 收集结果 / Then 必须访问备份服 backend/frontend/Website/Showroom 健康 URL，并通过 DCC 下载或预览 URL 证明对象读回。

## Milestones

- [x] M1：建立任务文档并确认上一前端任务 completed。
- [x] M2：新增 RED 静态合同测试，证明 `promote-backup` 真实 E2E 门禁缺失。
- [x] M3：实现 `promote-backup` real-flow 脚本和发布包恢复集绑定类型字段。
- [x] M4：运行静态合同、语法检查和授权缺失 fail-fast 验证。
- [x] M5：更新根审计进度、执行 cleanup 预览并提交改动。

## Expected Verification

- RED/GREEN：`node tests/e2e/runtime-control-promote-backup-real-flow-static.spec.js`
- GREEN：`node --check tests/e2e/runtime-control-promote-backup-real-flow.e2e.js`
- GREEN：未设置授权时运行 `node tests/e2e/runtime-control-promote-backup-real-flow.e2e.js`，必须在浏览器打开前 fail-fast。
- GREEN：`node tests/e2e/runtime-control-foolproof-static.spec.js`
- GREEN：`node tests/e2e/runtime-control-release-package-static.spec.js`
- GREEN：`node tests/e2e/runtime-control-ops-static.spec.js`
- GREEN：`pnpm ts:check`
- GREEN：`git diff --check -- tests/e2e/runtime-control-promote-backup-real-flow.e2e.js tests/e2e/runtime-control-promote-backup-real-flow-static.spec.js tests/e2e/runtime-control-foolproof-static.spec.js tests/e2e/runtime-control-ops-static.spec.js src/api/infra/runtimeControl/index.ts doc/tasks/20260605-runtime-control-promote-backup-e2e-gate`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺授权、缺显式 URL、缺发布包恢复集绑定、缺健康检查 URL 或缺 DCC 读回 URL 时直接失败，不提交动作。
- `是否从根因和长期维护角度解决`：是。通过可复用 Playwright 真实用户路径脚本和静态合同锁定备份服接管证据要求。
- `是否存在临时补丁或绕过`：否。本任务不调用接口绕过页面，不登录远程环境，不执行真实接管。

## 当前状态

completed

## Current Status

completed

## 完成内容

- 新增 `tests/e2e/runtime-control-promote-backup-real-flow.e2e.js`，授权后通过 Playwright 登录运行控制台、打开“上线备份服务器”、读取 `/release-packages`、校验指定发布包的 `testedRecoverySetCandidateId`、`testedRecoverySetId`、`testedRecoverySetManifestHash`、提交 `promote-backup`、等待日志成功，并验证备份服健康 URL 与 DCC 读回 URL。
- 新增 `tests/e2e/runtime-control-promote-backup-real-flow-static.spec.js`，锁定显式授权、显式 releaseTag、备份服健康 URL、DCC 读回 URL、恢复集绑定字段和无固定目标 fallback。
- 补齐 `RuntimeControlReleasePackageVO` 的测试恢复集绑定字段，前端 E2E 可直接校验发布包与恢复集绑定。
- 更新 `runtime-control-foolproof-static.spec.js` 覆盖新的 `promote-backup` real-flow 门禁。
- 对齐 `runtime-control-ops-static.spec.js` 的恢复数据边界文案，锁定当前更严格的“同一恢复集”表达。

## 验证结果

- RED：`node tests/e2e/runtime-control-promote-backup-real-flow-static.spec.js` -> FAIL，缺少 `tests/e2e/runtime-control-promote-backup-real-flow.e2e.js`。
- GREEN：`node tests/e2e/runtime-control-promote-backup-real-flow-static.spec.js` -> PASS。
- GREEN：`node --check tests/e2e/runtime-control-promote-backup-real-flow.e2e.js` -> PASS。
- GREEN：未设置 `RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_BACKUP=1` 时运行 `node tests/e2e/runtime-control-promote-backup-real-flow.e2e.js` -> FAIL-FAST，未打开浏览器或提交动作。
- GREEN：`node tests/e2e/runtime-control-foolproof-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-release-package-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-ops-static.spec.js` -> PASS。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN：`git diff --check -- tests/e2e/runtime-control-promote-backup-real-flow.e2e.js tests/e2e/runtime-control-promote-backup-real-flow-static.spec.js tests/e2e/runtime-control-foolproof-static.spec.js tests/e2e/runtime-control-ops-static.spec.js src/api/infra/runtimeControl/index.ts doc/tasks/20260605-runtime-control-promote-backup-e2e-gate` -> PASS，仅 Git 行尾提示。
- GREEN：`task_closeout.py --task-id 20260605-runtime-control-promote-backup-e2e-gate --mode preview` -> READY，delete `<none>`，blocked `<none>`，warnings `<none>`。
- GREEN：`task_closeout.py --task-id 20260605-runtime-control-promote-backup-e2e-gate --mode apply` -> APPLIED，delete `<none>`，blocked `<none>`，warnings `<none>`。

## 阻塞

- 真实 `promote-backup` 接管执行仍需用户在当前任务中明确授权；本任务仅补齐授权前的 fail-fast E2E 门禁和执行脚本。

## Cleanup Keep

- `doc/tasks/20260605-runtime-control-promote-backup-e2e-gate/task.md`
- `doc/tasks/20260605-runtime-control-promote-backup-e2e-gate/execution-log.md`
