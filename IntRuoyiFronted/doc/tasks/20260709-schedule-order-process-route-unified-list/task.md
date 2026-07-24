# Task: 工艺排产路线弹窗接入标准列表模板

## 任务目标

- 将 `/mes/pro/scheduleorder` 的“工艺排产路线”弹窗工序列表替换为 `UnifiedListTemplate` 标准列表模板。
- 工序列表保留原有展开报工明细、工序编号、工序名称、班次产能、需求数量、完成数量、状态、报工次数、最近报工时间。
- 工序列表新增额外工序内容展示：班次状态（白班/夜班）与预计完成时间。
- 不改排产算法、不自造 mock 数据；仅补齐后端 `process-list` VO 对既有工序快照字段的正式暴露。
- 完成静态契约与真实浏览器 E2E 验证。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`；中文文件读写显式 UTF-8，命令不使用 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；弹窗列表保持紧凑运维控制台风格，并接入标准列表模板。
- 登录 / Playwright 登录：已读取 `docs/login-access.md`；真实 E2E 前必须先执行官方登录预检。
- 项目级防错 / 前端统计展示：已读取 `docs/agent-memory/project-error-prevention.md`；不引入 mock、fallback 或静默错误。
- 高风险动作：本任务只修改本机前端源码、静态测试和任务文档；E2E 只做本机真实浏览器验证，不操作服务器。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；复用标准列表模板，并补齐接口 VO 对既有班次字段的正式暴露，避免前端兜底推断。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 工艺排产路线使用标准列表模板 -> Given 用户打开排产工单的工艺排产路线弹窗 / When 工序列表渲染 / Then 列表使用 `UnifiedListTemplate` 并保留展开报工明细能力。
- BDD: 工序列表展示班次状态 -> Given 工序存在白班或夜班排产属性 / When 用户查看工艺排产路线弹窗 / Then 表格展示“班次状态”列，显示白班或夜班。
- BDD: 工序列表展示预计完成时间 -> Given 工序存在计划结束时间 / When 用户查看工艺排产路线弹窗 / Then 表格展示“预计完成时间”列并格式化时间。

## 里程碑

- [x] M1：建立任务记录并读取经验门禁。
- [x] M2：补 RED 静态契约。
- [x] M3：接入标准列表模板并新增字段展示。
- [x] M4：运行静态测试、类型检查和 evidence 校验。
- [x] M5：运行真实 E2E 并完成收尾。

## 预期验证

- `node tests/e2e/mes-schedule-order-process-route-unified-list-static.spec.js`
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check`
- `node "D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs" --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/schedule-order --target-text 排产工单`
- `node tests/e2e/mes-schedule-order-process-route-unified-list-real.e2e.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-schedule-order-process-route-unified-list/frontend-feature-evidence.md`

## 当前状态

COMPLETED：已将“工艺排产路线”弹窗工序列表接入 `UnifiedListTemplate` 标准列表模板，新增班次状态与预计完成时间列；补齐后端工序快照 VO 的 `nightShiftEnabled` 字段暴露；静态契约、前端类型检查、官方登录预检和真实浏览器 E2E 均已通过。

## Cleanup Keep

- `doc/tasks/20260709-schedule-order-process-route-unified-list/frontend-feature-evidence.md`
