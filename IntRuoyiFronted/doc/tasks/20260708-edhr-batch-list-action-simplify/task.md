# 任务：eDHR 批次列表操作按钮精简

## 任务目标

将 eDHR 批次执行列表行操作区从“详情、流程追踪、操作轨迹、UX检查、预检、查看归档、下载打印版PDF”等分散入口，精简为三个主入口：填写、追溯、打印。原有能力不得删除，应合并到对应主功能中。

## 经验门禁

- 已读取 `docs/powershell-memory.md`：本任务涉及 PowerShell 与中文任务文档读写，必须显式使用 UTF-8，不使用 `&&`。
- 已读取 `docs/experience-index.md`：本任务命中“前端页面 / 表格 / 样式”，必须遵循统一前端样式来源 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- 已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：列表行操作保持紧凑 inline text action，不引入无关视觉重设计。
- 已读取 `frontend-feature-delivery` 与 `frontend-contract.md`：前端行为变更需记录 BDD、RED/GREEN、入口、组件和验证证据。
- 已读取 `clear-frontend-copy` 与 `copy-standards.md`：用户可见按钮文案使用规范简体中文，术语保持一致。
- 本轮只修改本机前端源码、静态测试和任务文档；不操作服务器、不修改数据库、不改真实租户数据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；通过重新组织行操作入口，将次级能力收敛到主入口下，不删除原有弹窗和下载链路。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- BDD: 批次列表只显示三个主操作 -> Given 用户打开 eDHR 批次执行列表 / When 查看任意批次行右侧操作区 / Then 行内只暴露“填写”“追溯”“打印”三个主按钮，不再并列显示详情、流程追踪、操作轨迹、UX检查、预检、查看归档、下载打印版PDF。
- BDD: 填写主入口承载填写与检查 -> Given 用户点击批次行“填写” / When 进入批次详情页 / Then 详情页继续提供填写、UX 检查和预检相关能力。
- BDD: 追溯主入口承载追踪归档轨迹 -> Given 用户点击批次行“追溯” / When 打开追溯入口 / Then 可以继续查看流程追踪、操作轨迹和归档信息。
- BDD: 打印主入口承载打印下载 -> Given 用户点击批次行“打印” / When 触发打印入口 / Then 继续复用打印版 PDF 下载能力。

## 里程碑

1. M1：建立任务文档并记录 BDD/门禁。`DONE`
2. M2：新增 RED 静态契约覆盖三主按钮。`DONE`
3. M3：修改列表行操作模板，收敛为“填写 / 追溯 / 打印”。`DONE`
4. M4：运行聚焦静态测试和基础校验，记录 GREEN 结果。`DONE`
5. M5：只提交本次相关改动。`DONE`

## 预期验证

- RED：`node tests/e2e/edhr-batch-list-action-simplify-static.spec.js` 先失败，证明当前行操作仍并列暴露多个次级入口。
- GREEN：同一静态契约通过，确认操作区只渲染三个主按钮，并且原有追溯、预检、归档、打印能力仍被主入口引用。
- REGRESSION：既有 eDHR 归档/追溯相关静态契约保持通过或按本次新模式同步更新。

## 当前状态

`COMPLETED`：已完成 eDHR 批次执行列表行操作按钮精简，列表只保留“填写 / 追溯 / 打印”三个主入口；填写进入详情并承载预检与 UX 检查，追溯聚合流程追踪、操作轨迹和归档查看，打印复用打印版 PDF 下载。

## 完成记录

- 完成时间：2026-07-08。
- 已修改 `src/views/mes/pro/edhr-batch/BatchExecutionListPage.vue`：列表行操作列宽收窄为 180px，直接按钮精简为“填写 / 追溯 / 打印”。
- 已修改 `src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue`：详情填写链路补充 UX 检查抽屉，并统一归档打印下载文案为“下载打印版PDF”。
- 已新增 `tests/e2e/edhr-batch-list-action-simplify-static.spec.js` 并更新相关静态契约。
- 最终验证：三项聚焦静态测试通过；`NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` 通过。
- 说明：`tests/e2e/edhr-p0-p2-ux-resolution-static.spec.js` 仍因 `WorkTaskBoardPage.vue` 既有归档规则提示断言失败，失败点与本次批次列表按钮精简无关。

## Cleanup Keep

- `doc/tasks/20260708-edhr-batch-list-action-simplify/frontend-feature-evidence.md`
