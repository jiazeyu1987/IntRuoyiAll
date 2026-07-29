# 20260729 eDHR 切换工序卡片订单号展示

## Task Goal

调整 eDHR 填写页“切换工序”弹框内的工序卡片：卡片更高、字体更大；隐藏卡片内红框位置的明细说明；在截图黄框位置展示订单号。

## Milestones

- [ ] 识别当前切换工序卡片模板、订单号字段来源和既有静态合同。
- [ ] 编写聚焦静态合同，先覆盖更高卡片、更大字体、隐藏明细、展示订单号。
- [ ] 修改前端模板和样式，保持工序候选来源与切换逻辑不变。
- [ ] 运行聚焦验证和相邻回归，记录 RED/GREEN/REGRESSION。
- [ ] 完成 cleanup、经验沉淀、提交与推送，或记录阻塞项。

## Expected Verification

- `node tests/e2e/edhr-assist-process-switch-card-order-static.spec.js`
- `node tests/e2e/edhr-assist-process-switch-dialog-grid-static.spec.js`
- `node tests/e2e/edhr-assist-process-switch-all-statuses-static.spec.js`
- `pnpm ts:check`

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，沿用正式执行页上下文和工序切换模型，只调整展示字段与卡片样式。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `docs/frontend-development.md#eDHR 辅助模式当