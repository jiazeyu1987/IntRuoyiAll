# Word 工艺路线按产品绑定升级

## Request Summary And Source

- 来源：用户在当前任务中明确要求。
- 请求：Word 导入选择某产品后，如果该产品已经绑定工艺路线，应升级原路线；如果已有草稿，停止创建新路线或新版本。

## Current Baseline Reviewed

- 当前预检仅按正式“路线 - DCC 项目”绑定定位升级路线。
- 当正式 DCC 绑定缺失时，即使 MES 产品已绑定路线，预检仍返回新建，存在重复路线风险。
- 已有候选治理能够复用 DRAFT，并阻止 PENDING_APPROVAL / READY_TO_PUBLISH。

## Classification

- 类型：行为缺陷修复及目标识别规则变更。
- 优先级：高；错误的新建会制造同产品重复路线并割裂版本链。

## Impact

- 产品：选择 DCC 项目后，已有产品绑定路线优先成为升级目标。
- 设计：正式 DCC 绑定优先；仅在其不存在时，使用稳定编码关系 `DCC projectCode -> MES item.code -> route_product` 定位，不使用名称匹配。
- 数据：升级确认后在同一事务补齐路线与所选 DCC 项目的正式绑定；不直接 SQL 改业务数据。
- API：沿用现有预检冻结的路线、ACTIVE 版本、候选版本 ID，无新增请求字段。
- 测试：新增唯一产品绑定、DRAFT 复用、多路线阻止、异项目绑定冲突回归，并复跑原候选治理用例。
- 发布与运维：需要重新构建并确认 int_main 运行包；真实 E2E 仍依赖可见的唯一目标路线数据。

## Decision

- 结论：接受并拆分为“目标识别”和“候选治理回归”两个测试切片。
- 业务边界：
  - 已有正式 DCC 路线绑定时，以正式绑定为准。
  - 无正式绑定且产品只绑定一条路线时，升级该路线并补齐正式 DCC 绑定。
  - 产品绑定多条路线时阻止，不自动任选。
  - 唯一路线已正式绑定其他 DCC 项目时阻止，不静默改绑。
  - DRAFT 表示停止创建新路线/新版本，但允许更新同一 DRAFT。

## Required Approvals

- 用户已在当前请求中明确批准该行为变更。
- 不涉及 schema、远程发布或直接 SQL 数据修复，无额外审批。

## Downstream Skill Reruns

- behavior-driven-development：补充可观察场景。
- backend-api-delivery：完成服务层实现与数据库集成测试证据。
- independent verification：完成后端定向回归和前端静态合同；满足真实数据前置条件后再执行真实 E2E。

## Blockers And Next Action

- 当前 RT000035 已被任务外页面会话删除，无法用该真实对象完成恢复/草稿复用 E2E。
- 服务链路和数据库定向回归已完成：唯一产品绑定路线升级并补正式 DCC 绑定，DRAFT 原 ID 复用，多路线、跨 DCC、PENDING/READY 均阻止。
- 下一步只在正式页面存在唯一、可见、任务允许操作的产品绑定路线时继续真实 E2E；不得创建替代路线冒充原 RT000035 的恢复验证。
