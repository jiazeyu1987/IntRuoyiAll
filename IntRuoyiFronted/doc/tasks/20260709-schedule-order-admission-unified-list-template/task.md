# 20260709 待同步差异列表替换标准列表模板

## 任务目标

- 将排产工单页“待同步差异”弹窗内的列表替换为标准列表模板 `UnifiedListTemplate`。
- 保留原有工单编码、产品编号、入池状态、阻断原因筛选，保留汇总标签、选中工单加入排产工单池、分页、行选择、行操作、接口调用和权限判断。
- 新增独立表格标识，支持显示字段配置、列宽拖拽持久化和快速过滤入口。
- 修复标准列表模板接入后待同步差异表格列宽不可拖拽的问题，确保表格启用列拖拽边框并把持久化列宽绑定回每个业务列。

## 里程碑

- [x] M1 建立任务文档、BDD 场景、设计约束与经验门禁。
- [x] M2 编写待同步差异标准列表模板静态契约，先得到 RED。
- [x] M3 接入 `UnifiedListTemplate`，保留现有业务逻辑。
- [x] M4 运行静态契约、类型检查与前端证据校验。
- [x] M5 任务收尾预览、更新日志并按仓库状态决定是否提交。

## 预期验证

- `node tests/e2e/mes-pro-schedule-order-admission-unified-list-template-static.spec.js`
- `node tests/e2e/mes-pro-schedule-order-batch-admission-static.spec.js`
- `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js`
- `node tests/e2e/unified-list-template-static.spec.js`
- `node tests/e2e/user-table-column-config-static.spec.js`
- `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-schedule-order-admission-unified-list-template/frontend-feature-evidence.md`

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`；中文文件读写使用 UTF-8 aware 路径或 `apply_patch`，不使用默认 `Get-Content` / `Set-Content` / 重定向处理中文。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；列表页使用标准列表模板，保留紧凑操作台风格、快速过滤、显示字段、列宽拖拽、分页。
- 本任务不涉及真实 E2E、服务器写入、数据库写入、发布、备份、恢复或 worktree 合并；无需执行高风险 `experience-preflight`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接把待同步差异列表接入项目标准列表模板和现有列配置 hook。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

## 当前状态

- Status: completed
- 已完成：待同步差异列表已接入标准列表模板；列宽拖拽和持久化列宽回填已修复；静态回归、类型检查、前端证据校验已通过。

## 最终验证结果

- RED: `node tests/e2e/mes-pro-schedule-order-admission-unified-list-template-static.spec.js` -> FAIL，待同步差异列表尚未接入标准列表模板。
- GREEN: `node tests/e2e/mes-pro-schedule-order-admission-unified-list-template-static.spec.js` -> PASS。
- RED: `node tests/e2e/mes-pro-schedule-order-admission-unified-list-template-static.spec.js` -> FAIL，待同步差异表格未启用 `border`，且部分业务列未绑定持久化 `width`。
- GREEN: `node tests/e2e/mes-pro-schedule-order-admission-unified-list-template-static.spec.js` -> PASS，列宽拖拽前置条件和所有业务列宽回填契约通过。
- GREEN: `node tests/e2e/mes-pro-schedule-order-batch-admission-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/unified-list-template-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/user-table-column-config-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN: `validate_frontend_feature.py --evidence doc/tasks/20260709-schedule-order-admission-unified-list-template/frontend-feature-evidence.md` -> PASS。

## 提交状态

- Git commit -> BLOCKED，当前前端仓存在 93 个脏改路径，且本任务修改的 `src/views/mes/pro/scheduleorder/index.vue` 与 `docs/request-command-log.md` 在本轮开始前已存在未提交改动；为避免混入非本任务 hunk，未创建提交。
