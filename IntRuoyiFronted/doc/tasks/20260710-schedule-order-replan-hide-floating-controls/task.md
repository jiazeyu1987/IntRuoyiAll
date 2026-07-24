# 排产前检查 / 手动重排隐藏浮动列控件

## 任务目标

- 修复 `/mes/pro/schedule-order` 的“排产前检查 / 手动重排”抽屉中，诊断表格右上方自动浮出“显示字段 / 重置”控件的问题。
- 两张诊断表仅展示业务内容，不提供浮动列配置入口。
- 保留排产工单主列表、同步工单列表现有的显式“显示字段”能力。

## 上一任务检查

- 当前隔离 worktree 中最新已提交任务 `doc/tasks/20260710-scheduler-workbench-dynamic-replan-explanation/` 状态为 `completed`，不阻塞本任务。
- 主工作区存在其他并行任务的未提交改动；本任务使用独立前端 worktree，禁止读取、覆盖或提交那些改动。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`，命令显式使用 UTF-8，PowerShell 5.1 不使用 `&&`。
- 前端样式：遵循 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，保持紧凑操作台和现有抽屉布局，不做无关重设计。
- 缺陷回归：先用静态契约复现浮动控件注入条件并记录 RED，再做最小模板修复并记录 GREEN。
- 全局列配置：复用 `UserTableColumnGlobalEnhancer` 已有的显式表格排除契约，不修改全局增强器行为。
- 无 fallback：不增加降级、兼容分支、异常吞噬、模拟成功或默认占位结果。
- worktree：只修改前端仓；分支为 `codex/20260710-schedule-order-replan-hide-floating-controls`，路径为 `D:\ProjectPackage\Int\IntRuoyiWorktrees\r260710rfc\yudao-ui-admin-vue3`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；目标表格缺少全局增强器的显式排除标记，修复复用既有正式契约。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- `BDD: 排产前检查表不显示浮动列控件 -> Given 用户打开排产前检查 / 手动重排抽屉且预检存在问题列表 / When 全局表格列增强器扫描页面表格 / Then 预检问题表必须被识别为显式配置表格，不注入显示字段和重置浮窗。`
- `BDD: 重排问题表不显示浮动列控件 -> Given 用户完成重排预览且存在问题列表 / When 全局表格列增强器扫描页面表格 / Then 重排问题表必须被识别为显式配置表格，不注入显示字段和重置浮窗。`
- `BDD: 主列表显示字段能力保持不变 -> Given 用户查看排产工单主列表或同步工单列表 / When 使用页面标题栏的显示字段入口 / Then 原有列显示、重置和宽度持久化能力继续保留。`

## 里程碑

1. [已完成] 建立隔离 worktree、任务记录和经验门禁。
2. [已完成] 新增浮动控件回归静态测试并记录 RED。
3. [已完成] 为两张诊断表补充显式排除标记。
4. [已完成] 运行目标测试、相关回归、ESLint 和 TypeScript 检查。
5. [部分完成] 实现已提交并融合；融合提交在干净 worktree 上完成真实复验，主工作区运行态和 task-closeout 清理仍被并行脏改阻塞。

## 预期验证

- 预检问题表和重排问题表均包含 `data-user-table-column-explicit`。
- 抽屉内不再出现自动浮动的“显示字段 / 重置”控件。
- 页面主列表和同步工单列表原有显式显示字段入口不变。
- 目标静态测试先 RED 后 GREEN，相关列配置回归、ESLint、TypeScript 均通过。
- 真实浏览器打开目标抽屉后，页面截图中不存在红框所示浮动控件。

## 运行态规划

- 前端 worktree：`D:\ProjectPackage\Int\IntRuoyiWorktrees\r260710rfc\yudao-ui-admin-vue3`
- 前端分支：`codex/20260710-schedule-order-replan-hide-floating-controls`
- 前端端口：`8086`
- 后端依赖：`http://127.0.0.1:48081`
- 数据库、Redis、文件服务：沿用经运行态归属校验确认的本机后端依赖；未完成归属校验前不执行真实 E2E。

## Cleanup Candidates

- `doc/tasks/20260710-schedule-order-replan-hide-floating-controls/verify-real-e2e.cjs`
- `doc/tasks/20260710-schedule-order-replan-hide-floating-controls/bug-regression-evidence.md`
- `doc/tasks/20260710-schedule-order-replan-hide-floating-controls/frontend-feature-evidence.md`
- `output/playwright/20260710-schedule-order-replan-hide-floating-controls/`
- `.runtime/`

## Cleanup Keep

- `doc/tasks/20260710-schedule-order-replan-hide-floating-controls/task.md`
- `doc/tasks/20260710-schedule-order-replan-hide-floating-controls/execution-log.md`
- `doc/tasks/20260710-schedule-order-replan-hide-floating-controls/verification-report.md`
- `tests/e2e/mes-schedule-order-replan-hide-floating-controls-static.spec.js`

## Current Status

ready_for_closeout

## 当前阻塞

- 实现提交 `7b8f807e7` 已快进融合到前端 `int_main`。
- 融合后目标静态测试、列配置回归、工具栏回归和排产范围 TypeScript 检查通过。
- 主工作区现有 `8081` Vite 进程与新启 `8087` 进程均出现 HTTP / 页面加载超时；该工作区同时存在大量其他任务未提交改动，无法把主工作区运行态作为本任务稳定真实 E2E 证据。
- 同一融合提交在干净任务 worktree 上使用 `8086` 再次完成官方登录和真实 Playwright 验证，结果通过。
- `task-closeout-cleanup` preview 因主工作区脏改且当前分支已先行融合而阻塞；按门禁保留 worktree，不执行 apply、不删除目录、不标记 `completed`。
