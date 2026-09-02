# 20260901 注册证变更改为列表弹框提交

## Request Summary And Source

- Source: 用户在当前对话明确指出，注册证列表的“变更”应与“延续”一致，打开填写变更内容的弹框；确认后提交变更审核，而不是跳转详情页填写。
- Summary: 将当前列表“变更”入口由详情页 `mode=change` 改为列表行弹框。弹框包含现有变更批件字段和作废证书操作；底部“确认”提交变更审核。

## Current Baseline Reviewed

- 列表当前的“变更”按钮跳转注册证详情页的 `mode=change`。
- 详情页的操作面板承载变更批件字段、文件选择、变更提交、作废证书及已有校验。
- 后端变更提交接口、审批申请、文件绑定和履历记录已经存在。

## Classification

- Requirement change
- Frontend interaction and entry-point correction

## Impact Analysis

- Product: 用户可在列表中完成与“延续”一致的提交动作，不再被带到详情页。
- Design: 将变更/作废表单抽为独立弹框；列表行必须传递证件 ID 与行版本。
- Data and API: 复用现有提交接口和 FormData，不变更审批、文件、履历或更新证件显示信息的后端契约。
- Tests: 更新原跳转详情的静态合同，新增弹框、字段、确认提交和列表刷新合同。
- Release and operations: 仅本地前端代码和定向验证，不涉及发布、迁移、菜单或业务数据写入。

## Decision

- Accepted. 该调整与用户给出的交互图及“与延续按钮类似”的明确要求一致，且可复用已有正式变更审核链路。

## Required Approvals

- 当前范围只改前端页面和测试，不需数据库、远程服务或发布授权。

## Downstream Skill Reruns

- frontend-feature-delivery

## Blockers And Next Action

- Blockers: 无。
- Next action: 先建立变更弹框的失败合同，再实现最小前端调整和定向验证。
