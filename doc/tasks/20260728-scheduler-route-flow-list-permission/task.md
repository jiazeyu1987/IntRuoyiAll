# 20260728-scheduler-route-flow-list-permission

## Task Goal

让排产员具备工艺流程列表页的可操作列表权限，确保页面“操作”列按正式权限展示可用操作。

## Milestones

- [ ] 确认工艺流程列表权限、菜单权限和排产员角色绑定的正式来源。
- [ ] 先用回归用例复现排产员缺少工艺流程列表操作权限的问题。
- [ ] 实施最小权限补齐方案，不引入 fallback、降级或吞异常。
- [ ] 运行目标验证并记录 RED/GREEN/REGRESSION 证据。
- [ ] 完成收尾记录、清理和提交推送。

## Expected Verification

- 静态或后端回归测试覆盖排产员角色必须包含工艺流程列表操作所需权限。
- 如涉及 SQL/菜单权限，核对 `system_menu` 权限来源和目标角色绑定来源。
- 运行受影响测试命令并记录结果。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是补齐正式菜单/角色权限链路。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 权限/菜单变更必须同时核对前端组件、`system_menu` 路径/组件/权限、目标角色菜单绑定和登录后权限响应。
- 写 SQL 或迁移前必须先从当前迁移文件、Mapper、夹具或真实 schema 核对目标表结构；不得凭记忆编写菜单权限 SQL。
- PowerShell 和中文文档读写必须显式 UTF-8；不得使用默认 `Set-Content` / `Add-Content` / `Out-File` 写中文。
