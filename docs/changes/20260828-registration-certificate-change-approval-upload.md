# 20260828 国内注册证变更批件上传闭环

## Request Summary And Source

- Source: 用户在当前对话提出国内注册证“可随时上传变更批件”的业务需求，并追问当前逻辑是否符合。
- Summary: 变更批件必须由用户上传正式文件，填写批准日期，选择一个或多个变更内容；产品名称、注册人名称、生产地址等结构化变更需要同步更新当前证件显示信息；“其他内容”必须填写说明，可与结构化变更并入同一次批件。

## Current Baseline Reviewed

- 当前注册证模块已有国内注册证列表、详情、变更/作废面板、注册证业务文件表、变更履历表、文件下载授权和 DCC 权限模型。
- 旧基线缺口：前端仍存在“变更批件业务文件 ID”手填口径，后端变更接口可接收缺少真实上传文件的请求，不符合“上传变更批件”的业务动作。

## Classification

- Requirement change
- Compliance/data traceability need
- Frontend and backend contract update

## Impact Analysis

- Product: 用户从“手填文件 ID”改为“选择变更批件文件并提交”，交互更贴合文控上传动作。
- Design: 变更内容支持多选；生产地址变更增加委托生产、自行生产和受托企业校验。
- Data: 每次变更需要生成变更主记录、变更明细、生命周期事件，并把变更批件文件绑定为受监管业务文件。
- API: 变更提交接口从 JSON 请求收敛为 multipart 表单，文件为必填。
- Tests: 需要前端静态合同和后端服务/控制器定向测试覆盖上传文件、结构化多选、其他内容、生产方式校验和作废相邻行为。
- Release: 属于注册证变更链路行为修正，不应扩大到续证审批、下载申请或提醒规则。
- Operations: 缺少文件、缺少变更项、生产方式不完整时必须显式失败，不能默认成功或静默降级。

## Decision

- Accepted.
- Reason: 用户需求明确，且现有“手填业务文件 ID”与受监管业务文件上传、履历可追溯要求不一致，必须按正式上传闭环修正。

## Required Approvals

- 当前任务仅修改本地实现与测试，不涉及生产数据、远程服务、发布、迁移执行或权限分配；无需额外运维审批。
- 若后续要在测试服或生产库执行菜单/权限/数据库迁移，需要按项目发布和数据库规则另行授权。

## Downstream Skill Reruns

- backend-api-delivery
- frontend-feature-delivery

## Blockers And Next Action

- Blockers: 暂无。
- Next action: 完成后端 multipart 文件绑定验证、前端合同验证、`git diff --check`，并更新任务证据。
