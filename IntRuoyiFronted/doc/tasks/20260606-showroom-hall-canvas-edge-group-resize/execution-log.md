# 执行日志：展柜画布边界组调整

- BDD: T 型交界后继续移动竖向边界 -> Given 用户先拖动横向边界形成上下不对齐的 T 型交界 / When 用户再拖动相邻竖向边界 / Then 系统自动扩展边界组，只调整局部相邻产品块，画布仍完整铺满且所有产品块保持矩形。
- BDD: 边界组拖动不影响远端产品块 -> Given 画布中存在远离目标边界组的产品块 / When 用户拖动边界组 / Then 远端产品块坐标保持不变。
- BDD: 边界组拖动受最小尺寸限制 -> Given 边界组一侧产品块接近最小尺寸 / When 用户继续拖动 / Then 系统夹取 delta，阻止产品块小于最小尺寸。

- RED: `node scripts/showroom-admin-hall-canvas-layout.test.mjs` -> FAIL，新增 T 型交界竖向边界组用例后，当前单边界 resize 报错 `画布产品块不能重叠`。
- GREEN: `node scripts/showroom-admin-hall-canvas-layout.test.mjs` -> PASS，4 个用例通过；T 型交界竖向边界组移动后画布完整、远端产品块不变，边界组 delta 夹取到 `0.31` 后相邻块保持最小宽度 `0.04`。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS；直接运行 `pnpm ts:check` 曾因 Node 默认 4GB 堆限制 OOM 退出 134，显式内存后无类型错误。
- GREEN: `node doc/tasks/20260606-showroom-hall-canvas-edge-group-resize/scripts/verify-edge-group-e2e.mjs` -> PASS，本机 `http://127.0.0.1:8081` 测试租户 `测试租户/aoteman` 登录后打开 `hall_05` 画布，横向拖动改变 4 个块，随后竖向边界组拖动改变 2 个块，保存后后端读回坐标与画布编辑结果一致。
