# 一线生产填写页原型完全一致

## Task Goal

将一线生产页签下的生产填写页面改为与 `C:\Users\BJB110\Desktop\3\frontline-production-operator-1920.html` 的生产操作原型保持一致：去掉后台页内标题壳，核心操作屏采用固定 `1920 × 1080` 画布、相同顶部/主区/底部布局、相同“主页”按钮文案和相同主视觉尺寸，同时保留正式动态数据、提交校验和 API 链路。

## Milestones

- [ ] M1：冻结当前页面与原型差异，并建立聚焦静态合同 RED。
- [ ] M2：修改生产填写页外壳与生产操作屏样式/文案，使其匹配原型。
- [ ] M3：更新相邻静态合同，确保用户明确要求的原型一致性优先于旧“最大化”按钮口径。
- [ ] M4：运行聚焦验证、记录结果并完成任务收尾。

## Expected Verification

- `node tests/e2e/edhr-frontline-production-prototype-parity-static.spec.cjs`
- `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`
- `node tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs`
- `node src/views/mes/pro/feedback/frontline-template-render.spec.cjs`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-frontline-production-prototype-parity/frontend-feature-evidence.md`

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接对生产填写页的正式组件和静态合同做原型一致性约束。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端静态契约隔离门禁：本次新增聚焦静态合同，避免全量旧合同或运行态依赖掩盖当前视觉结构差异。
- 前端截图样式块静态契约门禁：静态合同锁定生产操作屏选择器与关键尺寸，避免宽泛正则跨块误判。
- 用户显式原型覆盖：本任务按用户要求“完全一致”覆盖旧生产页顶部按钮默认显示“最大化”的既有口径；PQC 页保持原逻辑不改。
