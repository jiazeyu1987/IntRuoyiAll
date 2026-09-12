# 历史路线生产配置迁移入口变更

## Request Summary And Source

- 来源：用户在通用规程一线 PQC E2E 阻塞后明确授权开发缺失的迁移入口。
- 请求：让历史 ACTIVE 路线能够从现有生产组长正式配置创建完整候选版本，再按正式页面发布。

## Current Baseline Reviewed

- `RT000028-IDI` 当前 ACTIVE 为 `V14 / #744`。
- 页面“创建候选版本”因缺 `productionProcessConfigs` 被 `1040271035` 阻断。
- 页面只允许在候选版本维护生产配置，形成无法通过现有前端解除的闭环阻塞。
- 生产组长现有正式配置服务仍可提供逐工序超量比例、损耗、设备组和参数规则。

## Classification

- 已确认的迁移入口缺失，属于功能缺口和 E2E 前置阻塞修复。

## Impact

- Product：路线版本工作区增加显式迁移按钮。
- Design：迁移动作与普通“创建候选版本”分开，必须二次确认。
- Data：只新增 DRAFT 候选版本；不修改 ACTIVE 版本，不直接发布。
- API：候选创建请求增加可选布尔字段 `migrateLegacyProductionConfig`。
- Test：新增后端迁移成功/缺项阻断和前端入口静态合同；完成后继续真实 E2E。
- Release：需要重新构建并重启本机 int_main 后端；不涉及数据库迁移。
- Operations：迁移来源必须是当前账号正式可维护配置；完整身份不一致时不创建候选。

## Decision

- ACCEPT。
- 原因：用户明确授权，且没有其它正式前端路径可解除历史路线候选创建闭环阻塞。

## Required Approvals

- 用户已授权共享路线配置迁移和继续 E2E。
- 用户已授权本轮 int_main 后端重启。
- 未授权 Git 提交/推送。

## Downstream Skill Reruns

- backend-api-delivery
- frontend-feature-delivery
- bug-regression-fix-loop
- playwright

## Blockers And Next Action

- 若任一工序缺超量比例、设备组身份不合法、参数规则不完整或配置集合与路线节点不一致，迁移必须阻断并显示正式错误。
- 下一步：BDD/TDD 实现显式迁移入口，重启后通过页面迁移、发布、复制测试单并验证一线 PQC。
