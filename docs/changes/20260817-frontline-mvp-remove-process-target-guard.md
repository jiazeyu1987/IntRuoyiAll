# 一线生产 MVP 放宽新旧路线工序身份匹配

## Request Summary

- 来源：用户明确要求 MVP 不因新旧路线工序身份校验阻断，但已有生产系数、目标数量、初始分配、完成量和进度逻辑保持不变。
- 页面流程保持“选择活跃订单、选择工序、选择员工、提交”不变。

## Current Baseline Reviewed

- 一线提交授权已经不要求活跃订单逐工序快照。
- 活跃订单已有旧版本工序的生产系数和目标数量快照；当前提交携带升级后的 routeProcessId，但 processId 与订单快照一致。
- 手工分配和 FIFO 已按活跃订单内唯一 processId 解析目标快照，并使用快照自身 routeProcessId 继续完成量协调。
- 班组长手工分配、FIFO、生产进度和放行链路仍以正式目标快照为依据。

## Classification

- 产品行为变更：MVP 取消一线初始分配对当前 routeProcessId 的精确匹配，复用现有按唯一 processId 解析目标快照的规则。

## Impact

- 产品：路线升级前后的活跃订单不因 routeProcessId 变化阻断一线提交。
- 设计：一线初始分配按 activeOrderId + 唯一 processId 读取已有目标快照，并继续使用快照 routeProcessId、生产系数和目标数量。
- 数据：提交、初始分配、审计、数量片段、订单工序完成量和进度更新逻辑全部保留。
- API：请求和响应不变。
- 测试：补充当前 routeProcessId 不同但 processId 唯一且目标快照存在时提交成功，并继续调用完成量协调的回归。
- 发布：无需数据库迁移；运行包更新后生效。
- 运维：同一活跃订单内同一个 processId 存在多条快照时仍明确失败，不任取一条。

## Decision

- ACCEPT：按用户明确授权，仅移除一线初始分配对提交 routeProcessId 的精确匹配；已有目标快照及后续业务逻辑保持不变。
- 班组长手工分配、FIFO、完成量和生产放行逻辑不变。

## Explicit MVP Degradation

- 触发条件：一线提交 routeProcessId 与活跃订单冻结 routeProcessId 不同，但 processId 相同且在订单内唯一。
- 行为：读取该订单已有目标快照，使用快照工序身份继续原有分配、完成量和进度逻辑。
- 风险：同一订单重复配置相同 processId 时无法唯一映射，继续明确失败，不做任意选择。
- 移除策略：正式版完成活跃订单冻结工艺版本工序加载后，页面直接提交冻结 routeProcessId，再恢复精确身份匹配。

## Required Approvals

- 用户已在当前会话明确批准 MVP 降级范围。

## Downstream Skill Reruns

- bug-regression-fix-loop
- backend-api-delivery

## Blockers And Next Action

- 当前无阻塞；先执行跨路线版本但已有唯一工序快照的 RED，再实施最小后端修改和定向回归。
