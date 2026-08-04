# 20260804 当前审批节点绿色高亮

## Task Goal

将 BPM/DCC 审批详情中当前正在进行的流程节点显示为绿色，已完成节点和未开始节点保持既有语义，不引入 fallback、吞异常或权限行为变化。

## Milestones

- [x] 创建任务记录并完成规则/技能前置读取
- [x] 定位审批详情时间轴节点渲染逻辑和现有样式契约
- [x] 先补充 RED 静态契约覆盖当前节点绿色显示
- [x] 实现最小前端样式/状态映射修改
- [x] 运行 GREEN/回归验证并记录证据
- [ ] 收尾、经验沉淀、提交并推送

## Expected Verification

- `node IntRuoyiFronted/tests/e2e/<target-static-spec>.js` 先 RED 后 GREEN，证明当前进行节点使用绿色状态类或绿色样式。
- 相邻审批详情静态契约通过，证明非当前节点不会被误标绿。
- 如本地运行态前置可用，再进行真实页面只读验证；若缺少运行态或登录前置，记录阻塞原因，不用 API-only 或 mock 替代。

## Current Status

ready_for_closeout

实现和验证已完成，等待 cleanup preview/apply、经验沉淀、提交和推送。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是修正当前节点状态到时间轴 UI 的正式映射。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- Frontend Feature Delivery：必须记录 BDD、RED、GREEN、入口组件、状态和验证路径。
- 前端开发规则：保持现有 Vue/TypeScript/Element Plus 模式，不扩大权限、路由、API 或状态流。
- DCC 文控审批处理入口门禁：审批处理页必须保留正式 BPM/DCC 入口，不用 viewer、API-only 或隐藏错误替代真实流程状态。
- 前端静态契约隔离门禁：本任务使用目标静态契约验证当前节点颜色映射，`pnpm ts:check` 已通过。
