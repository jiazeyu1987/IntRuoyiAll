# 工艺路线工序侧栏字段级刷新

## 任务目标

- 修复流转关系图点击不同工序时左侧详情侧栏整体出现加载遮罩和闪烁的问题。
- 保持关键工序开关、删除按钮、字段选择器、字段标题和字段卡片结构稳定，仅刷新字段值区域。
- 工序普通详情与关联设备分别完成后独立更新，快速连续切换时只允许最后选中的工序响应生效。
- 不修改后端接口、路由参数、字段可见配置、关键工序逻辑和保存流程。

## 里程碑

1. [已完成] 创建任务记录，读取 PowerShell、经验索引、前端交付、缺陷修复、Playwright 和统一前端样式门禁。
2. [已完成] 记录 BDD 场景并新增失败回归测试，锁定字段级加载和过期响应保护。
3. [已完成] 最小修改流转图详情侧栏加载状态和异步响应处理。
4. [已完成] 运行目标测试、相关回归、ESLint、TypeScript 和证据校验。
5. [已完成] 使用测试租户真实页面验证，进入清理和独立提交。

## 预期验证

- `node tests/e2e/mes-route-flow-detail-partial-refresh-static.spec.js`
- `node tests/e2e/mes-route-flow-selected-process-detail-static.spec.js`
- `node tests/e2e/mes-route-flow-detail-visible-items-static.spec.js`
- `node tests/e2e/mes-route-flow-link-return-state-static.spec.js`
- `node node_modules/eslint/bin/eslint.js src/views/mes/pro/route/RouteFlowGraphDesigner.vue tests/e2e/mes-route-flow-detail-partial-refresh-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260710-route-flow-detail-partial-refresh/frontend-feature-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260710-route-flow-detail-partial-refresh/bug-regression-evidence.md`
- 真实 Playwright：本机 `http://localhost:8081`、测试租户 `aoteman`，延迟真实详情请求但不伪造响应，验证仅字段值显示加载骨架。

## BDD 场景

- BDD: 切换工序仅刷新字段内容 -> Given 用户已选中一个工序并看到左侧详情 / When 用户点击另一个工序且详情请求尚未返回 / Then 侧栏结构、关键工序开关、删除按钮、字段选择器和字段卡片保持显示，仅字段值区域显示加载骨架。
- BDD: 普通详情和关联设备独立更新 -> Given 工序详情接口先于关联设备接口返回 / When 普通详情请求完成 / Then 普通字段立即显示新工序内容，关联设备字段继续显示字段内加载骨架直到自己的请求完成。
- BDD: 快速切换只应用最后响应 -> Given 用户快速依次点击多个工序且接口乱序返回 / When 较早选择的响应晚于最后选择返回 / Then 页面只显示最后选中工序的数据，过期响应不得覆盖当前内容。
- BDD: 当前请求失败明确可见 -> Given 当前选中工序详情接口失败 / When 请求结束 / Then 页面显示原有明确错误提示并结束对应字段加载，不使用旧值、模拟值或 fallback。

## 经验门禁

- PowerShell / UTF-8：已读取 `docs/powershell-memory.md`；中文文件显式 UTF-8，文件修改使用 `apply_patch`，命令不使用 `&&`。
- 前端页面 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本次仅调整加载反馈，不重设计页面。
- 前端交付：已读取 `frontend-feature-delivery` 和 `frontend-contract.md`；保持现有 API、路由和状态归属。
- 缺陷修复：已读取 `bug-regression-fix-loop` 和 `bug-contract.md`；先复现和新增失败测试，再修改生产代码。
- 真实 E2E：执行前读取 `docs/login-access.md` 并运行官方登录 preflight；只使用本机入口、测试租户和真实响应。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；移除侧栏级加载遮罩，按数据源维护字段级加载状态，并增加异步响应身份校验。
- 是否存在临时补丁或绕过：否。

## 当前状态

COMPLETED：工序切换已改为字段值区域骨架加载；普通详情与关联设备独立更新，并通过请求序号阻止过期响应覆盖当前选择。目标静态测试、相关回归、ESLint、TypeScript、证据校验和测试租户真实 E2E 均通过。

## Current Status

completed

## 最终验证结果

- RED: `node tests/e2e/mes-route-flow-detail-partial-refresh-static.spec.js` -> FAIL，原侧栏整体 loading 遮罩被回归测试命中。
- GREEN: 新增静态契约、工序详情、字段持久化、返回状态、流程图回归 -> PASS。
- GREEN: 目标 ESLint 与 `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check` -> PASS。
- GREEN: 前端特性证据与缺陷回归证据校验 -> PASS。
- GREEN: 官方登录 preflight -> PASS。
- GREEN: 测试租户真实 Playwright -> PASS，路线 `RT000017` 验证字段级骨架、独立更新和过期响应保护；页面、控制台和请求失败均为空。

## Cleanup Candidates

- tests/output/20260710-route-flow-detail-partial-refresh/
