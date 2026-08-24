# 注册证补充材料上传后直接生效

## Request

用户于 2026-08-24 明确决定：T-8“延续受理单/立卷发补单”等补充材料上传后不需要文控确认；本次暂不执行自动模板校验，上传后直接生效。

## Current Baseline Reviewed

- 原始需求：T-8 到期提醒后上传补充材料，提供模板自动校验，校验后颜色恢复正常。
- 后续生命周期设计曾增加“待文控确认/确认后恢复/拒绝后补充”的流程，并取消自动模板校验。
- 当前决策登记中的 RQ-13/P3-AC5 与原始需求存在这一差异。

## Classification

需求变更/业务流程政策变更。

## Decision

**Superseded**。本记录早期版本曾保留自动模板校验；以同日的 `20260824-registration-certificate-supporting-material-direct-effective-no-template-validation.md` 为当前权威口径：

1. 上传补充材料后执行证书、版本、租户、公司范围、正式业务文件归属及文件状态校验。
2. 上述门禁通过后，材料状态直接为 `EFFECTIVE`，T-8 浅色立即恢复正常。
3. 本次不执行自动模板校验，也不把未执行的校验伪装为已通过。
4. 不创建文控确认、拒绝、审批或人工确认步骤。
5. 保留上传、生效和失败结果的审计记录。

## Impact

- Product: 行操作保留“上传补充材料”，不显示“提交文控确认”；状态显示“已生效/需补充”。
- Design: supporting-document 状态机和服务职责由人工确认改为门禁通过后直接生效；自动模板校验本次不实现。
- Data: `confirmed_by/confirmed_at` 仅作为历史兼容字段回填，不再表示人工确认事实；不新增模板校验结果字段。
- API: 保留上传接口；删除/停用 confirm、reject 作为本流程必经入口，上传接口同步返回校验结果和生效状态。
- Tests: 增加“门禁通过立即生效”“门禁失败不生效”的 BDD/TDD；模板校验留待后续独立需求。
- Release: 不改变当前已完成的平台前置能力；影响后续 SP-05/SP-08 的补充材料功能。
- Operations: 不产生固定文控确认通知；上传类通知仍按 D-012 的独立事件矩阵处理。

## Required Approvals

- 业务决策：用户已在当前会话明确批准。
- 受影响 owner：DCC 注册证生命周期、前端注册证页面、测试计划 owner。

## Downstream Skill Reruns

- product-requirements-docs：同步 PRD/需求基线的当前口径。
- system-design-docs：同步 supporting-document 状态、API 和审计设计。
- bdd-tdd-acceptance-planner：重写 T17/P3-AC5 的场景、测试数据和验证命令。
- frontend-feature-delivery 与 backend-api-delivery：后续实现前重新读取本变更并按新合同开发。

## Blockers And Next Action

无决策阻塞。后续实现前必须先更新生命周期设计、接口合同、task-state 和测试计划，确保不再引用“待文控确认”或“自动校验不做”。
