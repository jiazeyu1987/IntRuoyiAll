# 注册经理直接下载注册证附件

## Request Summary

用户反馈：注册证下载时提示“注册证访问授权范围不合法”；admin 已有注册经理权限角色，注册经理需要有权限可以下载。

## Current Baseline Reviewed

- 当前注册证详情已有附件下载入口。
- 当前后端下载链路要求有效 `DOWNLOAD` grant；缺授权时在文件读取前失败。
- 当前旧证查看链路已允许注册部经理角色直接查看旧证，但下载链路未复用该角色授权。
- 当前受监管业务文件门禁要求所有文件出口必须经过服务端租户、公司、对象状态和访问模式校验。

## Classification

Accepted requirement change and bug fix: 注册部经理角色应作为正式下载授权来源。

## Impact

- Product: 注册经理可直接下载注册证附件，减少不必要的自申请审批阻塞。
- Design: 下载按钮不需要新增前端分支；服务端根据角色授权决定是否需要 grant。
- Data: 不新增迁移；注册经理直接下载不制造申请或 grant 数据。
- API: 下载接口保持不变，授权判定增强。
- Test: 新增注册经理直接下载、普通用户仍需授权、注册经理仍受公司范围约束的回归测试。
- Release: 后端行为变更，需跑注册证文件下载目标测试和授权相邻测试。
- Operations: 无新增配置和外部服务。

## Decision

Accept.

## Required Approvals

用户当前消息已明确提出业务规则：注册经理需要有权限可以下载。

## Downstream Skill Reruns

- `bug-regression-fix-loop`
- `backend-api-delivery`

## Blockers And Next Action

无阻塞；下一步补 RED 测试后实现最小服务端修复。
