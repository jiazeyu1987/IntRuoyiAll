# 20260805 生产组长人员统一状态列表

## Task Goal

调整生产组长“人员管理”：

- 删除“未禁用 / 已禁用”状态分组筛选。
- 已禁用与未禁用人员显示在同一个列表中。
- 已禁用人员的显示名使用红色文字。

不改变新增人员、修改显示名、启用/禁用、重置签名密码、分页和后端接口契约。

## Milestones

- [x] 创建任务记录并确认现有页面和查询逻辑
- [x] 编写聚焦静态合同并取得 RED
- [x] 实现统一列表与禁用姓名红色显示
- [ ] 运行 GREEN、相邻回归和 TypeScript 检查
- [ ] 完成 evidence 校验、cleanup、提交和推送

## Expected Verification

- `workdir=IntRuoyiFronted; node tests\e2e\production-personnel-unified-status-list-static.spec.cjs`
- `workdir=IntRuoyiFronted; node tests\e2e\production-leader-remove-header-content-static.spec.js`
- `workdir=IntRuoyiFronted; node tests\e2e\production-personnel-add-dialog-static.spec.cjs`
- `workdir=IntRuoyiFronted; node tests\e2e\production-personnel-management-real.e2e.js`（仅在真实账号、运行态和测试数据前置齐备时）
- `workdir=IntRuoyiFronted; pnpm ts:check`
- `workdir=E:\IntRuoyi; git diff --check -- IntRuoyiFronted\src\views\mes\pro\processpool\TeamLeaderWorkbenchPage.vue IntRuoyiFronted\tests\e2e\production-personnel-unified-status-list-static.spec.cjs doc\tasks\20260805-production-leader-personnel-unified-status-list`

## Current Status

in_progress

- 已定位状态分组来源：前端默认 `productionPersonnelQuery.enabled = true`，列表请求透传 `enabled`，并渲染“未禁用 / 已禁用”选择器。
- 已确认目标文件当前无未提交并行改动；工作区其它文件存在并行改动，任务边界将严格限定。
- 聚焦静态合同已取得 RED，最小实现已完成，等待 GREEN 和回归验证。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，移除前端状态过滤入口和查询参数，让正式列表数据源一次返回全部关联人员。
- `是否存在临时补丁或绕过`：否

## Applicable Experience Gates

- `docs/frontend-development.md#前端静态契约隔离门禁`：新增任务专用最小静态合同，独立证明统一列表和禁用姓名样式。
- `docs/frontend-development.md#前端截图样式块静态契约门禁`：合同锁定显示名列和人员查询逻辑，不使用跨整文件的宽泛正则。
- `docs/powershell-memory.md#脏工作区基线门禁`：基线提交不得混入本任务文件，提交前复核 staged 清单。
- `docs/powershell-memory.md#共享分支并发基线提交门禁`：每次提交前复查最近提交、任务文件 diff 和并行改动归属。
- `docs/task-closeout-rules.md#技能证据文件清理前归档门禁`：validator 通过后把结论归档到保留文档，再执行 cleanup。
