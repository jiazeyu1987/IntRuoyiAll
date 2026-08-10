# 20260810-pqc-leader-review-progress

## Task Goal
为 PQC 组长的 PQC 管理列表每行增加“审核”按钮；审核通过后，后端更新对应活跃订单的检验进度，并让前端刷新展示正式结果。

## Milestones
- [ ] 定位 PQC 组长管理列表、审核接口和活跃订单检验进度数据链路。
- [ ] 记录 BDD 场景并先补 RED 测试，锁定按钮、接口调用和进度更新行为。
- [ ] 实现最小范围前端按钮/API 包装与后端审核更新逻辑。
- [ ] 运行目标 GREEN、相关回归和证据校验。
- [ ] 收尾前更新验证报告与任务状态。

## Expected Verification
- 前端静态合同：PQC 管理列表每行存在审核按钮，点击调用正式审核接口，成功后刷新列表。
- 后端单元/合同测试：PQC 提交审核通过时按对应活跃订单重算检验进度，缺失正式活跃订单关系时失败而非默认成功。
- 技能证据校验：frontend-feature-delivery 与 backend-api-delivery evidence validator 通过。
- 结构检查：git diff --check。

## Current Status
in_progress

## Applicable Experience Gates
- 已读取 docs/experience-index.md；适用关键字包括 PQC、活跃订单、检验进度、前端写入成功与列表刷新失败分层门禁、确认提交上下文来源门禁。
- 设计要求：审核成功必须走正式后端数据链路更新活跃订单检验进度，不得用前端缓存或默认 100% 冒充成功。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，目标是补齐 PQC 组长审核到活跃订单检验进度的正式链路。
- 是否存在临时补丁或绕过：否。
