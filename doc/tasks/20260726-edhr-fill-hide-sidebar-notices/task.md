# Task: 隐藏 eDHR 填写页左侧提示

## Task Goal

隐藏 eDHR 填写工作台左侧栏截图红框中的“关闭前可修改”说明和“金手指测试权限”说明，同时保留待保存变更、保存草稿、提交执行、最大化以及真实错误和锁定告警。

## Milestones

- [x] 定位截图区域与现有渲染条件。
- [x] 建立聚焦回归合同并完成 RED。
- [x] 实现最小修复并完成 GREEN。
- [x] 完成相关静态合同和类型检查回归。
- [ ] 完成清理、提交和推送。

## Expected Verification

- `node tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js`
- `node tests/e2e/edhr-fill-workspace-static.spec.js`
- `pnpm ts:check`
- `pnpm build:local`
- 可用真实本地运行态下的 Playwright 页面验证

## Current Status

blocked

阻塞项：`pnpm build:local` 在 Vite/Rollup 构建产物生成后返回 1，错误为 `TypeError: Cannot set property code of  which has only a getter`。影响：不能宣称本地生产构建验证通过；本次左侧提示隐藏的聚焦静态合同、相关静态合同和 `pnpm ts:check` 已通过。

## 经验门禁

- 已读取 `docs/experience-index.md`。
- 命中 `docs/frontend-development.md#前端静态契约隔离门禁`：先使用聚焦静态合同锁定本次两条提示不渲染，不修改无关大契约来绕过历史问题。
- 页面错误、字段审计门禁和版本锁定告警必须继续真实暴露，不得因隐藏说明性提示而一并删除。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接移除左侧栏中不应展示的说明性提示节点，保留其业务状态计算供其他逻辑使用。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260726-edhr-fill-hide-sidebar-notices/bug-regression-evidence.md
