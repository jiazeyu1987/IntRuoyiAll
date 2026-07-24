# 任务：eDHR 详情页放行改为最后虚拟工序

## 任务目标

将 eDHR 批次执行详情页底部“收尾/放行归档”区域移除，改为左侧工序列表中的最后一个虚拟工序“放行”。选中“放行”后，中间区域展示放行/收尾状态摘要，右侧当前工序摘要栏展示原收尾按钮，作为放行工序参数和操作入口。

## 经验门禁

- 已读取 `docs/powershell-memory.md`：本任务涉及 PowerShell 与中文任务文档读写，必须显式 UTF-8，不使用 `&&`。
- 已读取 `docs/experience-index.md`：命中“前端页面 / 表格 / 样式”，必须遵循统一前端样式来源 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- 已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：详情页保持蓝/中性运维控制台风格，按钮迁移到右侧参数栏，不做无关视觉重设计。
- 已读取 `frontend-feature-delivery` 与 `frontend-contract.md`：前端行为变更需记录 BDD、RED/GREEN、入口、组件与验证证据。
- 本轮只修改本机前端源码、静态测试和任务文档；不操作服务器、不修改数据库、不改真实租户数据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；通过虚拟工序统一收尾/放行的页面归属，不复制功能、不新增后端契约。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 放行作为最后一个虚拟工序 -> Given 用户打开 eDHR 批次执行详情页 / When 查看左侧工序列表 / Then 列表末尾始终展示名为“放行”的虚拟工序，且不依赖后端工序任务数据。
- BDD: 选中放行展示收尾状态摘要 -> Given 用户点击左侧“放行” / When 中间区域切换内容 / Then 中间不再显示普通工序空表单提示，而展示批次状态、预检摘要、放行状态、归档状态等放行摘要。
- BDD: 放行参数栏承载原收尾按钮 -> Given 用户选中“放行” / When 查看右侧当前工序摘要栏 / Then 右侧展示“终态处理、归档打印、放行检查、UX检查、放行审批、追溯记录”六个操作，并保持原抽屉和动作绑定。
- BDD: 底部收尾区不再重复展示 -> Given 用户查看详情页 / When 页面渲染 / Then 不再存在底部“收尾/放行归档”操作区。

## 里程碑

1. M1：建立任务文档与 RED 静态契约。`DONE`
2. M2：实现放行虚拟工序与选中状态。`DONE`
3. M3：迁移收尾按钮到右侧参数栏并移除底部区。`DONE`
4. M4：更新既有静态契约并运行聚焦验证。`DONE`
5. M5：记录证据并提交本次相关改动。`DONE`

## 预期验证

- RED：`node tests/e2e/edhr-release-virtual-process-static.spec.js` 先失败，证明当前没有左侧“放行”虚拟工序，按钮仍在底部收尾区。
- GREEN：新增静态契约、收尾按钮契约、详情融合契约通过。
- REGRESSION：`NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` 通过。

## 当前状态

`COMPLETED`：已实现放行虚拟工序、右侧放行参数栏和底部收尾区移除；静态契约与 TypeScript 校验均已通过。

## 验证结果

- RED：`node tests/e2e/edhr-release-virtual-process-static.spec.js` -> FAIL，失败原因符合预期：页面尚未定义 `RELEASE_VIRTUAL_PROCESS`、`selectReleaseProcess` 和放行虚拟工序。
- GREEN：`node tests/e2e/edhr-release-virtual-process-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/edhr-closing-action-groups-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/edhr-closing-actions-compact-copy-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js` -> PASS。
- GREEN：`NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` -> PASS。

## Cleanup Keep

- `doc/tasks/20260708-edhr-release-virtual-process/frontend-feature-evidence.md`
