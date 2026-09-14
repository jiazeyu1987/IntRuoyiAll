# 测试服务器排产工单归入智能排产页签

## Task Goal

将测试服务器 `172.30.30.58` 上的 `排产工单` 动态菜单恢复为 `智能排产` 页签下的子菜单，并确保排产员角色能通过完整父链看到该入口。

## Milestones

- [x] 创建任务台账并读取服务器、数据库、编码和收尾规则
- [x] 只读核对测试服务器菜单、角色菜单和租户套餐当前状态
- [x] 执行最小范围 SQL 修复测试服务器菜单父子关系
- [x] 复验测试服务器菜单结构、权限绑定和服务健康
- [x] 记录验证报告并更新当前状态

## Expected Verification

- 测试服务器 `system_menu.id=5580` 的 `parent_id=900120`，`permission/path/component/component_name` 保持为排产工单正式入口。
- 测试服务器 `system_menu.id=900120` 与 `5580` 均为启用、未删除、可见菜单。
- 测试服务器排产员 `mes_scheduler` 角色对 `5100/900120/5580` 的 `system_role_menu` 绑定为有效。
- 当前启用测试租户套餐包含 `900120` 与 `5580`。
- 测试服务器后端 health 可用。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接修复动态菜单正式父子关系和角色/套餐权限链。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 已读取 `docs/experience-index.md`，命中动态菜单、权限菜单和中文菜单名门禁；本任务只修改测试服务器运行库，不更改前端硬编码文案。

## Verification Evidence

- RED 只读复现：测试服务器 `system_menu.id=5580` 为 `name=排产工单池`、`parent_id=5700`、`permission=''`、`path=schedule-order`。
- GREEN SQL 修复：`system_menu.id=5580` 更新为 `name=排产工单`、`parent_id=900120`、`permission=mes:pro-schedule-order:query`、`path=/mes/pro/schedule-order`、`component=mes/pro/scheduleorder/index`。
- 复验：`tenant_id=1` 与 `tenant_id=122` 的 `mes_scheduler` 均有效绑定 `5100/900120/5580/5590`。
- 复验：测试服启用套餐 `111/114` 均包含 `900120` 与 `5580`。
- 复验：测试服后端 health 返回 `{"status":"UP"}`。
- Cleanup：`task_closeout.py --mode apply` 已删除临时 `database-schema-evidence.md`，保留 `task.md`、`execution-log.md`、`verification-report.md`。
