# 批次执行列表替换标准列表模板

## 任务目标

将批次执行列表接入标准列表模板 `src/components/UnifiedListTemplate/index.vue`，保留现有查询条件、快速过滤、字段显示配置、列宽持久化、分页、业务按钮与表格操作逻辑。

## 经验门禁

- 前端列表改造必须遵循 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` 的统一列表风格。
- “标准列表模板”固定指 `yudao-ui-admin-vue3/src/components/UnifiedListTemplate/index.vue`。
- 只替换列表承载结构，不改后端接口、权限、分页、排序或业务按钮逻辑。
- PowerShell 命令必须先按 `docs/powershell-memory.md` 处理 UTF-8 与 Windows 命令约束。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，复用标准列表模板承载通用快速过滤、字段配置、列宽和分页能力。
- `是否存在临时补丁或绕过`：否。

## 里程碑

- [x] 建立任务文档和 BDD/RED 静态契约。
- [x] 将批次执行列表接入标准列表模板。
- [x] 运行静态契约、字段配置契约与类型检查。
- [x] 记录最终验证、提交状态与阻塞项。

## 预期验证

- 新增批次执行标准列表模板静态契约先失败后通过。
- 用户表格字段配置静态契约通过，确认批次执行列表由标准模板承载。
- 演练预检、业务操作相关静态契约通过，确认业务按钮与表格操作未丢失。
- TypeScript 检查通过，或失败时明确记录真实阻塞原因。

## 验证结果

- `RED: node tests/e2e/edhr-batch-execution-unified-list-template-static.spec.js -> FAIL`，预期失败：批次执行列表尚未导入并使用 `UnifiedListTemplate`。
- `GREEN: node tests/e2e/edhr-batch-execution-unified-list-template-static.spec.js -> PASS`。
- `GREEN: node tests/e2e/user-table-column-config-static.spec.js -> PASS`。
- `GREEN: node tests/e2e/edhr-rehearsal-readiness-panel-static.spec.js -> PASS`。
- `GREEN: node tests/e2e/edhr-readiness-business-action-static.spec.js -> PASS`。
- `GREEN: node tests/e2e/unified-list-template-static.spec.js -> PASS`。
- `GREEN: node tests/e2e/table-quick-filter-static.spec.js -> PASS`。
- `GREEN: NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check -> PASS`。
- `GREEN: python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-edhr-batch-execution-unified-list-template/frontend-feature-evidence.md -> PASS`。
- `GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260709-edhr-batch-execution-unified-list-template --mode apply -> PASS`，已删除临时前端证据文件。

## 提交状态

- BLOCKED：当前前端仓已有大量前置未提交改动，且本任务依赖的 `src/components/UnifiedListTemplate/index.vue` 为前置任务遗留未跟踪文件；同时 `src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue`、`tests/e2e/user-table-column-config-static.spec.js` 与既有脏改存在文件级重叠。为避免混入非本轮内容，未创建提交。

## Cleanup Candidates

- `doc/tasks/20260709-edhr-batch-execution-unified-list-template/frontend-feature-evidence.md`

## Current Status

completed: 批次执行列表已接入标准列表模板，验证通过；Git 提交因前置脏工作树阻塞。

## 当前状态

已完成：批次执行列表已接入标准列表模板，验证通过；Git 提交因前置脏工作树阻塞。
