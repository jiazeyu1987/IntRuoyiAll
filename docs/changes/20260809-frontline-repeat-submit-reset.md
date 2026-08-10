# 一线生产连续报工会话复位变更

## Request Summary And Source

- 来源：用户在当前任务中明确提出“一线生产要支持反复提交，不同的人不同的工序，所以提交之后要可以恢复到原样”。
- 请求：正式提交成功后不再永久锁定设备端填写页，而是结束本次填写会话并允许下一位员工或下一道工序继续独立报工。

## Current Baseline Reviewed

- 产品基线：`docs/product/production-role-system-operations.md` 要求设备端选择工艺路线、工序和实际员工后形成唯一签名提交。
- 验收基线：`docs/acceptance/production-line-process-pool/bdd-scenarios.md` R17 明确“同一工序允许多人、多次、分片填写”，R08 要求每次提交具有唯一电子签名。
- 幂等基线：`docs/acceptance/production-execution-main-loop/scope-contract.md` 要求区分用户的一次提交与网络/浏览器重复提交，不能只依赖前端禁用按钮。
- 实现基线：`FrontlineFixedTemplatePanel.vue` 当前以 `formalSubmitResult` 形成持久 `isProductionSubmitted` 状态，永久禁用输入、重填和再次提交；只有切换上下文时才清除状态并轮换 `productionSubmitDraftKey`。
- 活跃任务基线：`doc/tasks/20260809-frontline-submit-success-button-style/` 仍以“保留提交成功后的防重复提交约束”为目标，本次用户请求明确替代该页面永久锁定口径。
- 路线图/发布基线：当前相关正式材料以生产执行主闭环 readiness gate 为准，未发现独立发布计划要求页面永久锁定。

## Classification

- 类型：产品行为需求变更。
- 范围：一线生产设备端正式提交成功后的前端会话生命周期；不改变正式提交 API、后端事务、签名、报工事实或历史修改规则。

## Impact Analysis

- 产品：设备端可连续完成多员工、多工序、多次报工，符合 R17。
- 设计：取消持久成功锁定态；成功通过 toast 反馈，页面清空本次业务输入并恢复可操作状态。
- 数据：每次明确成功后生成新的客户端草稿键，从而形成新的幂等键；失败时保留原键，避免不确定写入被当成新报工。
- API：不新增或修改接口；每次确认动作仍只调用一次正式提交接口。
- 测试：新增成功复位、幂等键轮换和失败保留合同；更新现有正式提交静态合同，移除永久锁定断言。
- 发布：前端行为变更，影响一线生产报工入口；需通过聚焦回归和类型检查后方可交付。
- 运维：不涉及端口、服务、数据库、远程环境或发布操作。

## Decision

- 决策：接受。
- 理由：请求人与当前任务用户一致；行为与既有正式验收 R17 一致，并可在不削弱后端幂等和正式事实不可修改性的前提下实现。

## Required Approvals

- 当前用户已明确批准行为变更；无需额外发布、数据库或远程操作批准。

## Downstream Skill Reruns

- 使用 `frontend-feature-delivery` 完成前端 BDD/TDD、状态边界实现和聚焦验证。
- 完成前使用 `project-experience-consolidation` 更新已有一线生产正式提交经验，删除“页面永久锁定”旧口径并记录会话级幂等边界。
- 使用 `task-closeout-cleanup` 仅预览/清理本任务附属产物。

## Blockers And Next Action

- 当前无 blocker。
- 下一步：新增聚焦 RED 合同，证明现有永久锁定与成功后复位要求不一致。
