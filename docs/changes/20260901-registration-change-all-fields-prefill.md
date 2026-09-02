# 20260901 注册证变更全部字段可编辑并回显当前值

## Request Summary And Source

- Source: 用户基于“变更/作废”弹框截图指出，当前只有产品名称可填写，选择其它变更内容后无法编辑；并明确要求产品名称及后续其它 key 均显示注册证当前已设置的数据。
- Summary: 选择任一结构化变更内容时，显示对应可编辑字段并以当前注册证详情值初始化；提交审核后，审批通过应更新所选字段，不得只在前端显示或继续限制为产品名称、注册人名称。

## Current Baseline Reviewed

- 弹框目前仅为 `PRODUCT_NAME`、`REGISTRANT_NAME` 生成编辑字段，且未请求注册证详情，因此输入框为空。
- 注册证详情接口已返回产品名称、型号规格、结构组成、适用范围、产品技术要求、注册人名称、住所、生产地址及生产关系。
- 变更服务的即时变更能力已能处理八个结构化字段，但正式审核提交仍以 MVP 限制只允许产品名称、注册人名称，并且审批通过只投影这两项。

## Classification

- Requirement change
- Frontend usability defect
- Backend approval contract expansion

## Impact Analysis

- Product: 八个结构化变更项均可编辑；当前值作为编辑起点，避免用户重复录入或不知道原值。
- Design: 弹框打开后加载正式详情；详情未加载成功时禁止确认并显示错误。
- Data: 审批通过后仅更新本次选择并提交的结构化字段；未选择字段保持原值。
- API: 复用现有详情 GET 和变更 multipart POST，不新增 URL；扩展已有 `structuredValues` 的正式审核语义至八个结构化 key。
- Tests: 前端合同覆盖全部字段、详情预填、失败阻断；后端测试覆盖全部字段待审批、审批通过投影及生产关系。
- Release and operations: 不新增数据库表或迁移；本地实现和定向验证，不修改远程服务或业务数据。

## Decision

- Accepted. 前端可编辑字段与后端审批能力必须一致；任何“显示可编辑但提交被拒绝”或“提交后值被忽略”的实现均不符合需求。

## Required Approvals

- 当前只修改本地源码和测试，不需要数据库、远程发布或数据写入授权。

## Downstream Skill Reruns

- frontend-feature-delivery
- backend-api-delivery

## Blockers And Next Action

- Blockers: 无。
- Next action: 先建立前后端失败测试，再实现详情预填和八字段审批投影。
