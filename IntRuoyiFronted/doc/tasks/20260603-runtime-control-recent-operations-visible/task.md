# 任务：修复运行控制台最近操作不显示

## 任务目标

修复运行控制台“最近操作”列表不显示的问题。页面刷新时，最近操作接口 `/admin-api/infra/runtime-control/operations` 成功返回后必须独立更新列表；概览接口或其他辅助接口失败时，不得阻断已经成功返回的最近操作数据展示。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260603-dcc-nas-transfer-complete-dialog/task.md`
- 状态：`completed`
- 处理：本任务只修改运行控制台最近操作加载编排、对应回归测试和本任务文档，不接管其他仓库运行态改动。

## BDD 场景

- BDD: 最近操作不被概览失败阻断 -> Given 运行控制台操作记录接口成功返回真实操作记录，但概览接口失败 / When 页面执行刷新 / Then 最近操作列表仍必须更新显示成功返回的操作记录，顶部错误仍保留运维矩阵失败提示。
- BDD: 最近操作失败必须明确暴露 -> Given 最近操作接口请求失败 / When 页面执行刷新 / Then 顶部错误必须包含最近操作失败上下文，不得静默吞掉或显示默认成功状态。

## Milestones

- [x] M1：建立任务文档并确认上一前端任务已完成。
- [x] M2：先补 RED 回归测试，锁定最近操作必须独立加载。
- [x] M3：最小修改运行控制台加载编排，不引入 fallback、降级或吞异常。
- [x] M4：运行目标测试与类型检查。
- [x] M5：记录 GREEN 证据并完成收尾。

## Expected Verification

- RED：`node tests/e2e/runtime-control-recent-operations-visible-static.spec.js` 先失败，指出 `overview` 与 `operations` 仍被同一个 `Promise.all` 绑定。
- GREEN：同一命令通过。
- GREEN：`node tests/e2e/runtime-control-static.spec.js` 通过。
- GREEN：`pnpm ts:check` 通过。
- GREEN：frontend feature evidence validator 通过。
- GREEN：task-closeout-cleanup 预览通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；从请求编排边界修复，把最近操作列表作为独立数据源处理，同时保留失败提示。
- `是否存在临时补丁或绕过`：否。

## 当前状态

completed

## 已完成工作

- 已确认上一前端任务 `20260603-dcc-nas-transfer-complete-dialog` 状态为 `completed`。
- 已排查后端运行控制台状态目录，确认本机存在多条操作记录 JSON；现有后端 `RuntimeControlServiceImplTest` 通过，问题更符合前端请求编排导致列表未赋值。
- 已新增 `tests/e2e/runtime-control-recent-operations-visible-static.spec.js`，锁定最近操作请求不得被概览请求失败阻断。
- 已拆分 `loadOverview` 中概览和最近操作请求的错误处理，最近操作失败时显示 `最近操作：...` 错误上下文。
- 真实页面最终验证显示最近操作表格恢复为 34 行；该最终恢复还依赖后端同任务修复状态目录漂移。

## 验证结果

- CHECK：`mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest" test` -> PASS，35 tests，确认后端现有运行控制台服务用例通过。
- RED：`node tests/e2e/runtime-control-recent-operations-visible-static.spec.js` -> FAIL，当前 `loadOverview` 仍将 `getRuntimeControlOverview()` 与 `getRuntimeControlOperations()` 绑定到同一个 `Promise.all`。
- GREEN：`node tests/e2e/runtime-control-recent-operations-visible-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/runtime-control-hide-foolproof-error-static.spec.js` -> PASS。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN：Playwright 本机真实页面刷新 -> PASS，`/operations` 返回 34 条，`.operation-panel` 表格显示 34 行。
- GREEN：frontend feature evidence validator -> PASS。
- CLOSEOUT PREVIEW：task-closeout-cleanup 预览通过。

## 剩余阻塞

- 无。

## Cleanup Keep

- `doc/tasks/20260603-runtime-control-recent-operations-visible/frontend-feature-evidence.md`
