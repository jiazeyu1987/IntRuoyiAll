# eDHR 左侧工序卡片等高与名称显示

## 任务目标

- 修复批次详情左侧普通工序卡片在长列表中被纵向压缩、文字和状态被遮挡的问题。
- 特殊节点、普通工序和放行节点使用一致的 48px 卡片高度，超出可视区域时由列表滚动承载。
- 普通工序卡片主文本只显示工序名称，不显示工序编码，并为当前工序名称与状态提供足够宽度。

## 上一任务检查

- `doc/tasks/20260710-edhr-batch-process-order-layout/` 已标记 `completed`。
- 当前工作区存在其他任务改动；本任务仅修改 `BatchExecutionDetailPage.vue`、目标回归测试和本任务记录，不覆盖无关改动。

## 经验门禁

- PowerShell / UTF-8：已读取根目录 `docs/powershell-memory.md`，中文文档使用 UTF-8。
- 前端样式：遵循 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md` 的固定可预测尺寸和紧凑操作台样式。
- 缺陷修复：先以用户截图复现 flex 子项被压缩及编码误显示，再新增失败回归测试，最后做最小实现。
- 前端契约：不修改后端接口、任务排序、权限或状态，仅调整左侧导航的展示文本与尺寸约束。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；统一卡片尺寸变量并禁止列表子项 flex 收缩，由滚动容器承载超出内容。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 所有节点卡片等高完整显示 -> Given 左侧工序数量超过可视高度 / When 列表进入内部滚动 / Then 特殊节点、普通工序和放行节点保持统一高度且不被压缩遮挡。
- BDD: 普通工序显示业务名称 -> Given 普通工序同时具有编码和名称 / When 用户查看左侧导航 / Then 卡片主文本只显示工序名称，不显示工序编码。
- BDD: 当前工序名称完整显示 -> Given 工序名称与状态同时展示 / When 用户查看桌面端左侧导航 / Then 当前真实工序名称不出现横向省略或遮挡。

## 里程碑

1. [已完成] 建立任务文档、经验门禁和 BDD 场景。
2. [已完成] 新增等高、防压缩和名称显示 RED 回归测试。
3. [已完成] 最小修复左侧导航模板与尺寸样式。
4. [已完成] 运行目标回归、类型检查和真实页面验证。
5. [已完成] 更新证据、提交任务改动并完成收尾。

## 预期验证

- `node tests/e2e/edhr-batch-process-item-uniform-name-static.spec.js`
- `node tests/e2e/edhr-batch-process-order-layout-static.spec.js`
- `node tests/e2e/edhr-batch-process-card-density-static.spec.js`
- `node tests/e2e/edhr-batch-process-display-sort-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm.cmd ts:check`
- 本机真实页面核对全部左侧卡片高度一致、滚动时不压缩，普通工序显示名称而非编码。

## Current Status

completed

## Cleanup Candidates

- `doc/tasks/20260710-edhr-batch-process-item-uniform-name/bug-regression-evidence.md`
- `doc/tasks/20260710-edhr-batch-process-item-uniform-name/frontend-feature-evidence.md`
- `tests/output/20260710-edhr-batch-process-item-uniform-name/`

## Cleanup Keep

- `doc/tasks/20260710-edhr-batch-process-item-uniform-name/batch-detail-performance-diagnostic.cjs`

## Closeout Evidence

- 实现提交：`615da58ac`。
- `task-closeout-cleanup` preview/apply 均通过。
- 已清理本任务临时缺陷证据、前端证据和真实 E2E 输出目录。
- 已保留 `task.md`、`execution-log.md`、`verification-report.md` 及并行诊断脚本。
