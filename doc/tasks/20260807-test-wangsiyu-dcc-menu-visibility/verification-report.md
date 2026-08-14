# 验证报告

## Result

PASS：已定位测试服 `wangsiyu` 看不到文控页签的根因。测试服该用户有效角色只有 `approval_center_entry(910295)`，没有任何有效 DCC/文控角色；Redis 精确用户角色缓存也只包含 `910295`。

## Evidence

- 测试服用户：`wangsiyu(id=910250, tenant_id=1, tenantName=芋道源码, status=0, deleted=0)`。
- 测试服有效角色：仅 `approval_center_entry(910295)`；该角色 DCC 权限数为 `0`，根菜单 `6800 文控中心`、`900218 电子签名`、`990200 基础数据` 均未解析。
- 测试服历史绑定：`doc_control(910233)`、`wenkong_download(910234)` 绑定存在但 `deleted=1`，均于 `2026-08-02T23:54:20` 被软删除。
- 测试服候选安全角色：`wenkong_no_download(910417)` 存在，覆盖 `6800/900218/990200`，危险权限计数 `0`，但未绑定给 `wangsiyu`。
- 本机对比：本机租户 1 `wangsiyu(id=910250)` 有效绑定包含 `wenkong`、`wenkong_download`、`doc_control` 及多个 DCC 动作角色。
- Redis：测试服 DB 1 `user_role_ids:910250` 只缓存 `910295`；未执行缓存删除或全库清理。

## Root Cause

前一次“权限角色平移”同步的是角色定义和角色菜单权限，不同步 `system_user_role` 用户角色绑定。测试服 `wangsiyu` 仍只绑定审批中心入口角色，所以登录权限响应不会包含 DCC/文控菜单，自然看不到文控页签。

## Recommended Fix

- 只恢复文控入口且继续禁止下载：绑定 `wenkong_no_download(910417)` 到测试服 `wangsiyu`，然后只清理 `user_role_ids:910250` 精确缓存并要求重新登录。
- 完全按本机角色绑定平移：需要把本机 `wangsiyu` 的有效角色按稳定 `role.code` 绑定到测试服同名用户；该路径会恢复 `wenkong_download` 等下载能力，必须先明确授权下载权限风险。
