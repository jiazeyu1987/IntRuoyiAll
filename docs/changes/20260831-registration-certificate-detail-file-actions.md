# Request

用户要求注册证详情中的上传附件可以下载和在线查看；在线查看复用受控预览功能。

## Current Baseline Reviewed

- 详情接口已返回正式附件 ID 和原始文件名，页面仅显示文件名，没有文件操作入口。
- 注册证后端已有受控预览元数据和二进制接口，前端统一在线预览组件尚未接入注册证文件来源。
- 受控预览当前只接受正式当前有效版；详情当前投影还可能是待生效首证或待生效延续版本。
- 文件下载已有访问申请、BPM 审批、一次性授权和下载审计链路，详情不得直接绕过。

## Classification

已接受的产品行为变更：为注册证详情附件增加受控在线查看和下载入口。

## Impact

- Product: 用户在详情中可直接发起在线查看或下载操作。
- Design: 附件名称右侧增加“在线查看”“下载”图标按钮；在线查看使用弹窗承载统一受控预览，下载进入现有授权流程。
- Data: 不改表结构，不新增持久化字段。
- API: 复用既有注册证预览接口；预览文件身份校验扩展为允许详情当前投影中的 `CURRENT` 或 `PENDING_EFFECTIVE` 正式版本，旧证继续拒绝。下载接口和授权规则不变。
- Test: 增加前端详情动作合同、统一在线预览来源合同和后端待生效/旧证文件身份测试，运行类型检查及真实页面路径。
- Release: 前后端需同时更新；旧前端不会调用新增统一来源，旧后端会拒绝待生效版预览。
- Operations: 无迁移、无配置和无新增外部服务；继续依赖既有受控预览运行态。

## Decision

accept

## Required Approvals

用户当前消息已明确要求详情下载和在线查看；下载授权策略保持不变，无额外权限放宽审批。

## Downstream Skill Reruns

- backend-api-delivery
- frontend-feature-delivery
- playwright
- project-experience-consolidation
- task-closeout-cleanup

## Blockers And Next Action

暂无。下一步按 BDD + strict TDD 增加聚焦 RED 测试，再实施最小正式方案。
