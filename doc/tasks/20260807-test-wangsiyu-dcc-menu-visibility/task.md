# 测试服 wangsiyu 文控页签不可见诊断

## Task Goal

定位测试服务器 `172.30.30.58` 上 `wangsiyu` 账号看不到文控/DCC 页签的根因，区分用户角色绑定、角色菜单权限、租户菜单包、权限缓存或前端会话缓存问题。

## Milestones

- [x] 建立任务记录并读取服务器、登录、数据库、E2E 与 PowerShell 编码规则。
- [x] 只读核对测试服 `wangsiyu` 用户、租户、角色绑定和 DCC 菜单权限。
- [x] 必要时对比本机芋道源码同名账号或相同角色绑定。
- [x] 输出根因、影响范围和最小修复建议。

## Expected Verification

- 只读 SQL 证据覆盖 `system_users`、`system_user_role`、`system_role`、`system_role_menu`、`system_menu`、`system_tenant_package`。
- 若涉及 Redis 缓存，仅核对精确用户缓存键；不清全局缓存。
- 若需要修复，先备份并枚举候选 DCC 角色权限，排除下载/管理类旁路风险后再写入。

## Applicable Gates

- DCC 菜单恢复与无下载角色隔离门禁：恢复文控中心相关权限前，必须枚举候选角色权限并避免授予下载、目录管理、访问规则管理等危险权限，除非用户明确授权。
- 跨环境角色权限差异同步门禁：角色权限可按稳定键同步，但用户角色绑定必须单独核对，不能假设随角色定义同步。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按用户、角色、菜单、租户包、缓存逐层定位。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Final Result

根因是测试服 `wangsiyu(id=910250, tenant_id=1)` 当前有效角色只有 `approval_center_entry(910295)`，没有任何有效 DCC/文控角色；测试服 Redis DB 1 的精确缓存 `user_role_ids:910250` 也只缓存了 `[910295]`。本机同名租户 1 用户存在 `wenkong`、`wenkong_download`、`doc_control`、`dcc_action_view_independent`、`dcc_action_distribute_independent` 等文控/DCC 角色绑定，但上次跨环境同步只同步角色定义和角色菜单权限，明确保持 `system_user_role` 不变。

最小修复建议：如果只要求看到文控页签且继续禁止下载，优先给测试服 `wangsiyu` 绑定既有 `wenkong_no_download(910417)` 并只清理 `user_role_ids:910250` 精确缓存；如果要求完全按本机角色绑定平移，则会同时恢复下载相关角色，需要用户明确确认下载权限风险。
