# PQC组长页签角色可见性收敛

## Task Goal

- 只有拥有 `PQC组长权限角色`（角色编码 `pqc_leader_permission`）的用户可以看到 `PQC组长` 主导航页签。
- `PQC组长` 菜单使用专属菜单权限 `mes:pro-process-pool-pqc-leader:query`，不再复用通用 `mes:pro-process-pool-team-leader:query` 作为可见菜单权限。
- 保留 PQC 组长页面所需的通用后端接口权限，通过隐藏按钮权限授权，不把 `生产组长` 页签授权给 PQC 角色。

## Milestones

- [x] 创建任务目录和初始任务文档。
- [x] 核对现有 PQC 组长路由、菜单 SQL、角色菜单绑定和权限解析链路。
- [x] 补 RED 静态合同，证明当前 PQC 组长页签可见性仍过宽。
- [x] 实现专属 PQC 组长菜单权限与角色菜单绑定收敛。
- [x] 运行定向静态合同、数据库迁移合同和格式检查。
- [x] 更新验证报告并记录剩余阻塞。

## Expected Verification

- 前端 `PQC组长` 静态路由 meta permission 为 `mes:pro-process-pool-pqc-leader:query`。
- 正式菜单 `900435 / PQC组长` 的 permission 为 `mes:pro-process-pool-pqc-leader:query`。
- `900435` 仅授权给 `pqc_leader_permission` 角色，非该角色的有效绑定被软删除。
- `pqc_leader_permission` 角色拥有 `900435` 以及页面运行所需的通用查询、维护、复核按钮权限。
- 不新增 fallback、CSS 隐藏或前端空数据绕过。

## Experience Gate

- 已读取 `docs/experience-index.md`，匹配 `PQC组长`、`leaderType`、动态菜单、`system_role_menu` 和 tenant 1 admin 授权门禁。
- 适用门禁：`docs/frontend-development.md#前端角色内容页签拆分口径门禁`。
- 适用门禁：`docs/database-rules.md#系统角色菜单授权-tenant-1-admin-门禁`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；从动态菜单权限和角色菜单绑定两层收敛。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

- 已将前端静态路由和动态菜单 `900435 / PQC组长` 收敛为专属权限 `mes:pro-process-pool-pqc-leader:query`。
- 已新增迁移 `20260806_mes_pqc_leader_role_permission_tab.sql`，创建/恢复 `pqc_leader_permission`，新增隐藏查询权限 `900439`，并软删除非 PQC 组长角色对 `900435` 的有效绑定。
- 本机 Docker MySQL 已应用迁移并复核：非 `pqc_leader_permission` 对 `900435` 的有效绑定数为 `0`，tenant `1` admin 拥有 PQC 组长角色。
- 发布迁移总门禁被既有无关 SQL `20260805_erp_nas_table_auto_sync.sql` 的 `type=schema,job` 元数据阻塞；本次定向 SQL/前端合同均已通过。
