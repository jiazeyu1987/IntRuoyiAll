# 任务：展柜画布边界组调整

## 任务目标

在展柜画布中，当用户已经拖动过横向边框导致竖向边界上下不完全对齐时，仍允许用户继续拖动相关竖向边界。系统通过自动扩展边界影响组，保持每个产品仍为单个矩形块，画布仍完整铺满、无空隙、无重叠。

## Previous Task Check

- 前序任务：`doc/tasks/20260606-showroom-hall-canvas-layout/task.md`。
- 检查结果：状态为 `completed`。

## BDD 场景

- BDD: T 型交界后继续移动竖向边界 -> Given 用户先拖动横向边界形成上下不对齐的 T 型交界 / When 用户再拖动相邻竖向边界 / Then 系统自动扩展边界组，只调整局部相邻产品块，画布仍完整铺满且所有产品块保持矩形。
- BDD: 边界组拖动不影响远端产品块 -> Given 画布中存在远离目标边界组的产品块 / When 用户拖动边界组 / Then 远端产品块坐标保持不变。
- BDD: 边界组拖动受最小尺寸限制 -> Given 边界组一侧产品块接近最小尺寸 / When 用户继续拖动 / Then 系统夹取 delta，阻止产品块小于最小尺寸。

## 里程碑

- [x] M1：确认前序画布任务已完成并检查当前几何实现。
- [x] M2：新增 RED 测试覆盖 T 型交界后的竖向边界组拖动。
- [x] M3：实现边界组扩展、delta 夹取和 resize 应用逻辑。
- [x] M4：运行前端目标测试、类型检查和真实路径验证。
- [x] M5：完成任务收尾、清理和提交。

## 预期验证

- `node scripts/showroom-admin-hall-canvas-layout.test.mjs`
- `pnpm ts:check`
- Playwright 真实路径：登录测试租户后进入展柜列表，打开“画布布局”，先拖横向边界再拖竖向边界，保存后读回。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。非法边界组必须明确失败；可移动边界组通过正式约束计算和 delta 夹取处理。
- `是否从根因和长期维护角度解决`：是。把单边界 resize 升级为边界组 resize，不通过局部补丁绕过 T 型交界。
- `是否存在临时补丁或绕过`：否。不改变后端数据结构，不把一个产品拆成多个矩形块，不新增测试专用控件。

## Current Status

completed

## 进展记录

- 2026-06-06：已确认当前 `resizeCanvasBoundary` 基于单个 shared edge 计算相邻块，T 型交界场景需要升级为边界组。
- 2026-06-06：已新增 T 型交界竖向边界组回归测试，覆盖局部相邻块移动、远端块不变、边界组最小尺寸 delta 夹取。
- 2026-06-06：已实现沿同一 x/y 轴扩展 resize 影响范围，`clampCanvasBoundaryDelta` 与 `resizeCanvasBoundary` 共用边界组计算，保持单产品单矩形与完整铺满约束。
- 2026-06-06：验证已通过：`node scripts/showroom-admin-hall-canvas-layout.test.mjs`、`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`、一次性 Playwright 验证脚本 `node doc/tasks/20260606-showroom-hall-canvas-edge-group-resize/scripts/verify-edge-group-e2e.mjs`。
- 2026-06-06：收尾清理已执行，任务目录下的一次性 Playwright 验证脚本已删除，保留 `task.md` 与 `execution-log.md` 作为证据记录。

## Final Verification Result

- PASS：几何回归、类型检查与本机真实 Playwright 路径均已通过。
- PASS：未引入 fallback/降级/吞异常，未改变后端数据结构，未拆分产品为多矩形。
