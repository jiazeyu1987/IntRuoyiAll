# 20260804 当前审批节点绿色高亮

## Task Goal

将 BPM/DCC 审批详情中当前正在进行的流程节点显示为绿色，已完成节点和未开始节点保持既有语义，不引入 fallback、吞异常或权限行为变化。

## Milestones

- [x] 创建任务记录并完成规则/技能前置读取
- [x] 定位审批详情时间轴节点渲染逻辑和现有样式契约
- [x] 先补充 RED 静态契约覆盖当前节点绿色显示
- [x] 实现最小前端样式/状态映射修改
- [x] 运行 GREEN/回归验证并记录证据
- [x] cleanup preview/apply 和经验沉淀
- [x] 推送实现提交到 `origin/int_main`
- [x] 提交并推送最终收尾记录

## Expected Verification

- `node IntRuoyiFronted/tests/e2e/<target-static-spec>.js` 先 RED 后 GREEN，证明当前进行节点使用绿色状态类或绿色样式。
- 相邻审批详情静态契约通过，证明非当前节点不会被误标绿。
- 如本地运行态前置可用，再进行真实页面只读验证；若缺少运行态或登录前置，记录阻塞原因，不用 API-only 或 mock 替代。

## Current Status

completed

实现、验证、cleanup、经验沉淀、提交和推送均已完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是修正当前节点状态到时间轴 UI 的正式映射。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- Frontend Feature Delivery：必须记录 BDD、RED、GREEN、入口组件、状态和验证路径。
- 前端开发规则：保持现有 Vue/TypeScript/Element Plus 模式，不扩大权限、路由、API 或状态流。
- DCC 文控审批处理入口门禁：审批处理页必须保留正式 BPM/DCC 入口，不用 viewer、API-only 或隐藏错误替代真实流程状态。
- 前端静态契约隔离门禁：本任务使用目标静态契约验证当前节点颜色映射，`pnpm ts:check` 已通过。
- 前端 BPM 审批时间轴当前节点高亮门禁：已沉淀到 `docs/frontend-development.md` 并在 `docs/experience-index.md` 增加关键词路由。

## Final Commit And Push

- 实现和经验沉淀已随并行基线提交 `6f9ed0e83 chore: baseline existing workspace changes` 推送到 `origin/int_main`。
- 最终收尾记录单独提交并推送，未混入当前工作区其它未提交任务改动。
