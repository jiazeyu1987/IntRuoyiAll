# 执行日志：展柜画布布局编辑

- BDD: 打开展柜画布 -> Given 展柜列表中某展柜包含 X 个产品 / When 用户点击“画布布局” / Then 打开矩形画布并按 X 个产品生成平均布局，每个产品块显示产品名称。
- BDD: 调整产品块大小 -> Given 画布中存在相邻产品块 / When 用户拖动产品块边界 / Then 只影响相邻产品块，画布仍完整铺满且无空隙、无重叠。
- BDD: 交换产品位置 -> Given 两个产品块分别绑定不同产品 / When 用户把一个产品拖到另一个产品块 / Then 两个产品交换绑定，矩形坐标不变。
- BDD: 新增和删除产品块 -> Given 画布中存在产品块 / When 用户新增或删除产品块 / Then 系统只局部切分或填补相邻区域，并保持画布完整铺满。
- BDD: 保存画布布局 -> Given 用户完成布局调整 / When 点击保存 / Then 前端调用真实后端接口保存产品映射与归一化坐标，刷新后仍可恢复。

- RED: `node scripts/showroom-admin-hall-canvas-layout.test.mjs` -> FAIL，展柜列表缺少“画布布局”入口、前端 API 缺少 `updateHallCanvasLayout`，且缺少画布几何 helper。
- GREEN: `node scripts/showroom-admin-hall-canvas-layout.test.mjs` -> PASS，3 个用例覆盖入口/弹窗 wiring、分割/删除/交换/resize 几何完整性、10/23 产品默认平均布局无重叠。
- RED: `node scripts/showroom-admin-hall-canvas-layout.test.mjs` -> FAIL，新增越界拖动夹取用例后返回 `TypeError: clampCanvasBoundaryDelta is not a function`。
- GREEN: `node scripts/showroom-admin-hall-canvas-layout.test.mjs` -> PASS，`clampCanvasBoundaryDelta` 将过大 delta 夹到最小产品块尺寸，组件不再依赖 `catch { return }` 吞掉 resize 异常。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS，`vue-tsc --noEmit -p tsconfig.relaxed.json` 无类型错误。
- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-hall-canvas-layout-final open http://127.0.0.1:8081` + `run-code --filename doc/tasks/20260606-showroom-hall-canvas-layout/scripts/verify-hall-canvas-layout.mjs` -> PASS，真实登录测试租户 `测试租户/aoteman`，打开 `hall_05` 画布，完成相邻边界 resize、产品绑定交换、新增/删除产品块、保存和重开恢复。
- GREEN: Playwright 已登录会话认证读回 `/admin-api/showroom/hall/page?pageNo=1&pageSize=20` -> PASS，`tenantId=122`、`hallCode=hall_05`、`mappingCount=10`，首三个坐标为 `(0,0,0.28681,0.333333)`、`(0.28681,0,0.21319,0.333333)`、`(0.5,0,0.25,0.333333)`。
