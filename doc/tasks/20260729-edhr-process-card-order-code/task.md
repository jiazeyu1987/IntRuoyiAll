# 20260729 eDHR 切换工序卡片订单号展示

## Task Goal

调整 eDHR 填写页“切换工序”弹框内的工序卡片：卡片更高、字体更大；隐藏卡片内红框位置的明细说明；在截图黄框位置展示订单号。

## Milestones

- [x] 识别当前切换工序卡片模板、订单号字段来源和既有静态合同。
- [x] 编写聚焦静态合同，先覆盖更高卡片、更大字体、隐藏明细、展示订单号。
- [x] 修改前端模板和样式，保持工序候选来源与切换逻辑不变。
- [x] 运行聚焦验证和相邻回归，记录 RED/GREEN/REGRESSION。
- [ ] 完成 cleanup、经验沉淀、提交与推送，或记录阻塞项。

## Expected Verification

- `node tests/e2e/edhr-assist-process-switch-card-order-static.spec.js`
- `node tests/e2e/edhr-assist-process-switch-dialog-grid-static.spec.js`
- `node tests/e2e/edhr-assist-process-switch-all-statuses-static.spec.js`
- `pnpm ts:check`

## Current Status

ready_for_closeout

## Verification Evidence

- RED: `node tests/e2e/edhr-assist-process-switch-card-order-static.spec.js` -> FAIL，预期原因：缺少 `assistProcessSwitchOrderCode`，卡片仍显示二级说明，且样式仍为旧高度/字号。
- GREEN: `node tests/e2e/edhr-assist-process-switch-card-order-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/edhr-assist-process-switch-dialog-grid-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/edhr-assist-process-switch-all-statuses-static.spec.js` -> PASS。
- TYPECHECK: `pnpm ts:check` -> PASS。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，沿用正式执行页上下文和工序切换模型，只调整展示字段与卡片样式。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `docs/frontend-development.md#eDHR 辅助模式当前工序 assistRows 路由门禁`：本任务只调整卡片展示，必须保持工序切换候选来自当前批次全部普通工序任务，点击仍走现有正式切换链路。
- `docs/frontend-development.md#前端静态契约隔离门禁`：使用任务专用最小静态合同覆盖本次卡片高度、字体、明细隐藏和订单号展示需求，避免被无关宽合同影响。
- `docs/powershell-memory.md#脏工作区基线门禁`：当前存在并行任务脏文件，必须先独立基线提交并确保本任务文档/实现不混入基线。
- `docs/powershell-memory.md#PowerShell 分号串联测试退出码门禁`：验证命令逐条运行并记录退出码。

## Cleanup Keep

- doc/tasks/20260729-edhr-process-card-order-code/frontend-feature-evidence.md
