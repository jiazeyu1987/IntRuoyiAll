# 20260806 PQC Order Picker Production Layout

## Task Goal

将一线 PQC 顶部“生产订单”选择弹框的整体大小、布局，以及内部订单子卡片的大小、布局，同一线生产点击“工序”后的选择弹框保持一致。

## Milestones

- [x] Milestone 1: 定位生产订单弹框与一线生产工序弹框
- [x] Milestone 2: 补充 RED 静态合同锁定布局一致性
- [x] Milestone 3: 最小化同步弹框和子卡片样式
- [x] Milestone 4: 运行目标验证并记录结果
- [ ] Milestone 5: 收尾状态确认

## Expected Verification

- `node tests\e2e\mes-frontline-pqc-order-picker-production-layout-static.spec.cjs`
- `node tests\e2e\edhr-frontline-production-pixel-parity-static.spec.cjs`
- `git diff --check`

## Applicable Gates

- 前端静态契约隔离门禁：使用任务专用静态合同覆盖当前弹框布局，不扩大修改无关大合同。
- 前端截图样式块静态契约门禁：静态合同必须锁定目标选择器和目标状态块，避免跨块误判。
- 严格 no-fallback：不通过隐藏弹框、降级为 toast、替换真实选择控件或跳过正式选择行为来达成视觉一致。

## Current Status

blocked

## Verification Status

- PASS：`node tests\e2e\mes-frontline-pqc-order-picker-production-layout-static.spec.cjs`。
- PASS：`node tests\e2e\edhr-frontline-production-pixel-parity-static.spec.cjs`。
- PASS：`node tests\e2e\edhr-frontline-production-fullscreen-toggle-static.spec.cjs`。
- PASS：`node tests\e2e\mes-frontline-pqc-active-order-switching-static.spec.js`。
- PASS：task-owned `git diff --check`，仅有 CRLF 工作区提示，无空白错误。

## Blocker

- 实现和定向验证已完成，但当前共享工作区存在大量本任务外未提交改动。按任务所有权规则，未执行会混入无关改动的基线提交、实现提交或推送。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；复用现有一线生产工序弹框布局 token。
- `是否存在临时补丁或绕过`：否。
