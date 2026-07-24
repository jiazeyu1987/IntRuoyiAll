# 任务：补全运行控制台回滚版本目标环境前端操作

## 任务目标

补全运行控制台“回滚版本”前端操作，使操作员可以在页面选择测试服或备份服务器作为回滚目标、选择回滚候选、填写原因并提交动作；不得要求操作员直接调用接口，不得出现正式服作为本目标的回滚目标。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260604-runtime-control-restore-target-ui/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只修改运行控制台回滚版本前端、前端静态回归测试和任务证据。

## BDD 场景

- BDD: 回滚版本可选择目标环境 -> Given 操作员打开“回滚版本”弹窗 / When 查看目标环境区域 / Then 只能选择测试服或备份服务器。
- BDD: 回滚版本提交携带目标环境 -> Given 操作员选择目标环境、版本候选和原因 / When 点击确认执行 / Then 请求必须携带 `targetEnvironment` 与 `selectedImageCandidateId`。
- BDD: 回滚版本生产隔离提示明确 -> Given 操作员准备回滚应用版本 / When 查看预期结果 / Then 页面说明只回滚应用版本，不恢复数据，不影响正式服务器程序和数据。

## Milestones

- [x] M1：建立任务文档并确认上一前端任务已完成。
- [x] M2：新增 RED 静态回归测试。
- [x] M3：实现前端 UI、状态、校验、提交参数和文案。
- [x] M4：运行前端验证并记录证据。
- [x] M5：收尾预览并提交前端改动。

## Expected Verification

- RED/GREEN：`node tests/e2e/runtime-control-rollback-target-static.spec.js`
- GREEN：`node tests/e2e/runtime-control-ops-static.spec.js`
- GREEN：`pnpm ts:check`
- GREEN：frontend feature evidence validator
- GREEN：task-closeout-cleanup 预览

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少目标环境或候选时前端直接阻止提交。
- `是否从根因和长期维护角度解决`：是。把回滚目标环境作为前端表单状态、校验和请求契约的一部分。
- `是否存在临时补丁或绕过`：否。不通过接口绕过页面操作。

## 当前状态

completed

## 验证结果

- VERIFY：上一前端任务 `doc/tasks/20260604-runtime-control-restore-target-ui/task.md` 状态为 `completed`。
- RED：`node tests/e2e/runtime-control-rollback-target-static.spec.js` -> FAIL，原因：前端缺少 `rollbackTargetEnvironmentOptions`，回滚版本弹窗没有目标环境选择契约。
- GREEN：`node tests/e2e/runtime-control-rollback-target-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-restore-target-static.spec.js` -> PASS，确认新增回滚目标环境没有破坏恢复数据目标环境契约。
- GREEN：`node tests/e2e/runtime-control-ops-static.spec.js` -> PASS。
- GREEN：`node --check tests/e2e/runtime-control-rollback-app.e2e.js` -> PASS。
- GREEN：`$env:RUNTIME_CONTROL_E2E_BASE_URL='http://localhost:8081'; node tests/e2e/runtime-control-rollback-app.e2e.js` -> PASS，只打开“回滚版本”弹窗验证回滚目标和候选选择，未提交回滚动作。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN：frontend feature evidence validator -> PASS。
- GREEN：`git diff --check` -> PASS，仅有 Windows 行尾规范化提示。
- CLOSEOUT PREVIEW：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-runtime-control-rollback-target-ui --mode preview` -> READY，delete `<none>`，blocked `<none>`，warnings `<none>`。

## 剩余阻塞

- 暂无。

## Cleanup Keep

- `doc/tasks/20260604-runtime-control-rollback-target-ui/frontend-feature-evidence.md`
- `doc/tasks/20260604-runtime-control-rollback-target-ui/execution-log.md`
