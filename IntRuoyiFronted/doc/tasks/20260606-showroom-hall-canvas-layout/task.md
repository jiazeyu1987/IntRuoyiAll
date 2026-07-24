# 任务：展柜画布布局编辑

## 任务目标

在展柜列表中为每个展柜增加“画布布局”入口。点击后进入矩形布局画布，按当前展柜产品数生成初始平均产品块，支持新增、删除、调整大小、交换产品位置，并保存布局结果。

## Previous Task Check

- 同前端仓库前序任务：`doc/tasks/20260606-showroom-product-attachment-save-preview-fix/task.md`。
- 检查结果：状态为 `completed`。
- 后端仓库存在非展厅 runtime 任务 `20260606-runtime-backup-object-key-archive` 仍为 `in_progress`，本任务不修改 runtime 范围；展厅后端前序任务 `20260606-showroom-product-attachment-save-preview-fix` 已完成。

## BDD 场景

- BDD: 打开展柜画布 -> Given 展柜列表中某展柜包含 X 个产品 / When 用户点击“画布布局” / Then 打开矩形画布并按 X 个产品生成平均布局，每个产品块显示产品名称。
- BDD: 调整产品块大小 -> Given 画布中存在相邻产品块 / When 用户拖动产品块边界 / Then 只影响相邻产品块，画布仍完整铺满且无空隙、无重叠。
- BDD: 交换产品位置 -> Given 两个产品块分别绑定不同产品 / When 用户把一个产品拖到另一个产品块 / Then 两个产品交换绑定，矩形坐标不变。
- BDD: 新增和删除产品块 -> Given 画布中存在产品块 / When 用户新增或删除产品块 / Then 系统只局部切分或填补相邻区域，并保持画布完整铺满。
- BDD: 保存画布布局 -> Given 用户完成布局调整 / When 点击保存 / Then 前端调用真实后端接口保存产品映射与归一化坐标，刷新后仍可恢复。

## 里程碑

- [x] M1：检查前序任务、现有展柜列表、产品映射接口和后端数据结构。
- [x] M2：新增 RED 测试覆盖画布入口、布局坐标契约和核心几何操作。
- [x] M3：补后端产品映射布局坐标持久化、接口契约和迁移。
- [x] M4：实现前端画布布局弹窗、拖拽调整、产品交换、新增删除和保存。
- [x] M5：运行目标测试、类型检查和证据验证，完成提交。

## 预期验证

- `node scripts/showroom-admin-hall-canvas-layout.test.mjs`
- `pnpm ts:check`
- 后端目标单测：展厅内容服务与展柜接口相关测试。
- 前端真实路径：登录测试租户后进入展柜列表，打开“画布布局”，验证显示、拖动、交换、增删和保存。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。布局保存必须走真实后端接口，不使用 localStorage 或内存缓存冒充持久化。
- `是否从根因和长期维护角度解决`：是。布局坐标作为展柜-产品关系数据持久化，前端仅负责编辑归一化矩形布局。
- `是否存在临时补丁或绕过`：否。不新增测试专用控件，不隐藏接口失败，不自动重排覆盖用户调整。

## Current Status

completed

## 进展记录

- 2026-06-06：已确认前端展柜列表在 `HallListTable.vue`，现有产品保存接口为 `/showroom/hall/update-product-mapping`；后端 `showroom_hall_product` 仅保存产品顺序，缺布局坐标字段。
- 2026-06-06：已新增 `HallCanvasLayoutDialog.vue` 和 `hall/canvasLayout.ts`，展柜列表每行提供“画布布局”入口。
- 2026-06-06：已扩展前端 API 契约，保存调用 `/showroom/hall/update-canvas-layout`，不使用本地缓存替代后端持久化。
- 2026-06-06：已将边界拖动改为正式 delta 夹取逻辑，去除 resize 过程中的吞异常分支。
- 2026-06-06：目标测试、类型检查、后端回归、SQL 契约测试和测试租户真实 Playwright E2E 均已通过；最终读回 `tenantId=122`、`hall_05`、`mappingCount=10`，首行坐标为 `x=0,y=0,width=0.28681,height=0.333333`。
