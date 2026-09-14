# PQC组长权限角色分配给角色数小于 2 的用户

## Task Goal

- 在本机 `芋道源码` 租户中创建或复用 `PQC组长权限角色`。
- 将该角色赋予随机 2 个有效权限角色数小于 2 的用户。
- 角色只绑定 PQC 组长入口及必要父级菜单链路。

## Milestones

- [x] 创建任务目录和初始任务文档。
- [x] 只读核对租户、候选用户数量、现有角色和 PQC 组长菜单。
- [x] 创建或复用 PQC组长权限角色并绑定菜单。
- [x] 随机选择 2 个有效角色数小于 2 的用户并绑定角色。
- [x] 复核角色、菜单、用户绑定和回滚边界。

## Expected Verification

- 候选用户数量至少为 2。
- `PQC组长权限角色` 存在，角色编码为 `pqc_leader_permission`。
- 角色绑定菜单 `5100,900220,900435`。
- 本次正好新增 2 条用户角色绑定。
- 2 名目标用户绑定后均拥有 PQC组长角色，且有效角色数为 2。

## Experience Gate

- 已读取 `docs/experience-index.md`，匹配 `system_role` / `system_role_menu` / `system_user_role` / 菜单权限角色门禁。
- 已按 `docs/database-rules.md#系统角色菜单授权-tenant-1-admin-门禁` 收敛本次角色、菜单、用户角色链路；本次用户要求只授权随机 2 个角色数小于 2 的用户，未扩展 admin 授权范围。

## Verification Result

- Role: `910439 / PQC组长权限角色 / pqc_leader_permission` 已存在于 tenant `1`。
- Role menus: `5100,900220,900435`，计数 `3`。
- Assigned users: `617 jiangdan 蒋丹`、`1467 majing 马静`。
- Post-bind role count: 两名用户有效角色数均为 `2`，均拥有 PQC组长角色。
- Invalid selected users: `0`；duplicate bindings: `0`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；使用正式 `system_role`、`system_role_menu`、`system_user_role` 链路。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

- 本机数据库写入、独立复核、证据校验和任务清理已完成。
- 收尾清理保留 `task.md`、`execution-log.md`、`verification-report.md`，删除临时 `database-schema-evidence.md`。
