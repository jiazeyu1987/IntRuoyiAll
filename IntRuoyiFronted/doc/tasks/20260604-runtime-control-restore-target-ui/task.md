# 任务：补全运行控制台恢复数据前端操作

## 任务目标

补全运行控制台“恢复数据”前端操作内容，使操作员能够在前端选择恢复目标环境、选择恢复候选、填写原因并看到正式服务器不受影响的风险提示。仅修改前端，不直接调用接口绕过页面；后端若仍不接收 `restore-data` 的目标环境，记录为联调阻塞。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260603-runtime-control-recent-operations-visible/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只修改运行控制台恢复数据前端、前端静态回归测试和任务证据，不接管其它既有改动。

## BDD 场景

- BDD: 恢复数据可从前端选择目标环境 -> Given 操作员打开运行控制台恢复数据弹窗 / When 查看目标环境区域 / Then 只能选择测试服或备份服务器，不出现正式服作为恢复目标。
- BDD: 恢复数据提交携带前端目标环境 -> Given 操作员选择恢复候选、目标环境和原因 / When 点击确认执行 / Then 前端请求必须携带 `targetEnvironment` 与 `selectedBackupCandidateId`，不得要求操作员直接调用接口。
- BDD: 恢复数据正式服隔离提示明确 -> Given 操作员准备执行恢复数据 / When 查看预期结果和风险提示 / Then 页面必须说明仅覆盖所选测试/备份目标环境，并禁止影响正式服务器程序和数据。

## Milestones

- [x] M1：建立任务文档并确认上一前端任务已完成。
- [x] M2：新增 RED 静态回归测试，锁定恢复数据目标环境 UI 与提交契约。
- [x] M3：补全运行控制台恢复数据前端 UI、状态、校验、提交参数和文案。
- [x] M4：运行目标测试、静态回归、类型检查和 frontend evidence 校验。
- [x] M5：执行 task-closeout-cleanup 预览并提交本任务改动。

## Expected Verification

- RED：`node tests/e2e/runtime-control-restore-target-static.spec.js` 先失败，指出恢复数据没有目标环境选择或提交参数。
- GREEN：同一命令通过。
- GREEN：`node tests/e2e/runtime-control-ops-static.spec.js` 通过。
- GREEN：`pnpm ts:check` 通过。
- GREEN：frontend feature evidence validator 通过。
- GREEN：task-closeout-cleanup 预览通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少目标环境或恢复候选时前端直接阻止提交并提示。
- `是否从根因和长期维护角度解决`：是。把恢复数据目标环境作为前端表单状态、校验和请求契约的一部分，不靠人工直接调接口。
- `是否存在临时补丁或绕过`：否。本任务不绕过后端接口；如后端尚未接收该字段，记录联调阻塞。

## 当前状态

completed

## 验证结果

- VERIFY：上一前端任务 `doc/tasks/20260603-runtime-control-recent-operations-visible/task.md` 状态为 `completed`。
- RED：`node tests/e2e/runtime-control-restore-target-static.spec.js` -> FAIL，原因：`RuntimeControlTargetEnvironment` 缺少 `backup`，恢复数据弹窗缺少目标环境选择与提交契约。
- GREEN：`node tests/e2e/runtime-control-restore-target-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-ops-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-release-package-static.spec.js` -> PASS。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN：`RUNTIME_CONTROL_E2E_BASE_URL=http://localhost:8081 node tests/e2e/runtime-control-restore-data.e2e.js` -> PASS，只打开恢复数据弹窗验证恢复目标和候选选择，未提交恢复动作。
- GREEN：frontend feature evidence validator -> PASS。
- CLOSEOUT PREVIEW：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260604-runtime-control-restore-target-ui --mode preview` -> READY，delete `<none>`，blocked `<none>`，warnings `<none>`。

## 剩余阻塞

- 后端当前若仍只允许 `backup-now` 携带 `targetEnvironment`，真实恢复数据提交会被服务端拒绝；这需要后端按正式契约支持或明确阻塞，前端不做绕过。

## Cleanup Keep

- `doc/tasks/20260604-runtime-control-restore-target-ui/frontend-feature-evidence.md`
