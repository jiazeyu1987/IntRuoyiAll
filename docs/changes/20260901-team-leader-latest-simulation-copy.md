# Change Request: 生产组长复制最新版本模拟订单

## Request Summary And Source

- Source: 用户当前会话。
- Request: 从生产组长活跃订单复制一个仅供测试、可清理的新订单，按规则修改订单编号/名称，复制基础数据并加入活跃订单池，使新订单使用最新工艺路线和 QA 规程。

## Current Baseline Reviewed

- 活跃订单“重建”会更新原订单冻结版本，并在已有运行数据时要求删除历史后重建。
- Stage1 模拟会创建可清理测试工单，但复制来源订单已有路线/PQC快照并自动生成双100%事实。
- 正式活跃订单新增链路会实时解析当前 ACTIVE 路线和最新 PUBLISHED QA 规程。

## Classification

- Requirement change: 新增独立的测试模拟订单复制行为。

## Impact Analysis

- Product: 新增“复制测试单”和模拟副本清理入口；不改变正式订单。
- Design: 新订单需显示测试标识，确认文案说明版本和数据边界。
- Data: 复用现有模拟字段，不新增 schema；新工单、BOM、活跃订单、生产快照和 PQC 任务必须单事务创建。
- API: 新增模拟复制及受控清理接口；沿用生产组长维护权限。
- Test: 后端单元测试和前端静态合同，覆盖成功、权限/归属、正式来源缺失、清理范围和刷新失败。
- Release: 不涉及远程发布；需要后端和前端同时更新才可见。
- Operations: 模拟副本必须可追溯、可按严格标识清理，不得命中正式数据。

## Decision

- Accept.
- 采用独立新模拟订单，不刷新或覆盖来源订单。
- “其它数据复制”只包含基础工单信息；BOM和版本快照按创建时正式来源重新生成。

## Required Approvals

- 用户已确认副本仅供测试、可清理。
- 不需要数据库、远程服务或发布授权。

## Downstream Skill Reruns

- behavior-driven-development: 固定可观察行为。
- backend-api-delivery: 实现复制、最新版本锁定和清理接口。
- frontend-feature-delivery: 实现入口、确认、状态和错误展示。

## Blockers And Next Action

- Blockers: none.
- Next: RED tests, minimal implementation, targeted verification.
