# 工艺排产路线关键工序列开关设置

## 任务目标

在“工艺排产路线 -> 配置”的工序配置表中，将“关键工序”列改为可直接启用/禁止的开关。打开某行即设置该工序为关键工序；关闭即取消关键工序；同一路线已有关键工序时自动替换旧关键工序。已启用路线保持现有工序主数据禁止修改规则。

## 里程碑

- [x] 创建任务目录并记录任务目标
- [x] 写入 BDD 场景与 RED 静态契约
- [x] 实现关键工序列开关、自动替换和禁用规则
- [x] 运行静态契约与前端校验
- [x] 运行真实测试租户 E2E 验证并恢复真实数据
- [x] 更新任务状态与验证证据

## 预期验证

- 配置弹窗“关键工序”列存在 `el-switch`。
- 排产用途加载时把路线工序主数据 `keyFlag` 合并到配置表行。
- 打开新关键工序时先关闭旧关键工序，再打开目标工序。
- 已启用路线、无权限或缺少工序主数据时开关不可操作，并给出可见错误。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`，中文读写使用 UTF-8 显式处理。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，本次沿用现有 Element Plus 表格和开关样式，不做额外视觉重构。
- 项目级防错 / 智能排产统计口径 / 前端统计展示：已读取 `docs/agent-memory/project-error-prevention.md`，本次只改排产路线配置 UI，不引入 mock、fallback 或静默错误。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接把关键工序主数据字段暴露到现有配置表交互中，并复用现有后端更新契约。
- 是否存在临时补丁或绕过：否。

## 当前状态

已完成。关键工序列已改为开关设置，自动替换旧关键工序；目标静态契约、相关 route-use 静态回归、ESLint、`pnpm ts:check` 与真实测试租户 E2E 均通过。真实 E2E 命中 `tenantId=122` 的路线 `ROUTE-XLSX-00002(id=922047)`，完成关键工序切换、唯一性校验和原状态恢复。

## Cleanup Keep

- doc/tasks/20260708-schedule-route-key-process-switch/frontend-feature-evidence.md
- doc/tasks/20260708-schedule-route-key-process-switch/run-real-e2e.cjs
