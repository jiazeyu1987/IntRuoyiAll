# 任务：角色管理三分改名与导航重组菜单迁移

## 任务目标

- 将 `system_menu` 中原 `角色管理` 改造成目录父菜单，并在其下提供 `权限角色`、`组织角色`、`审批角色` 3 个子菜单。
- 保持原权限码、接口路径、组件路径和表结构不变，仅迁移菜单结构、角色菜单绑定和必要的租户套餐菜单集合。
- 保证已有绑定旧 `101 / 104 / 6804` 菜单的角色在重组后仍能看到对应新入口，不因父子关系变化丢菜单树。

## 当前状态

COMPLETED

## Current Status

COMPLETED

## 阻塞与影响

- 无。

## 上一任务检查

- 后端上一相关任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-mes-feedback-attribution-inline-edit\task.md`
- 当前状态：`COMPLETED`
- 处理说明：上一任务已完成，本次继续保持与其提交边界隔离，仅处理菜单重组相关 SQL、契约测试与任务文档。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 适用强制门禁：
  - 本轮只做本机菜单 SQL、静态结构验证和证据回写，不做远端数据库写入、发布或真实登录验证。
  - 若后续追加真实库写入或长链路 E2E，必须先在 `execution-log.md` 记录 `GREEN: experience-preflight -> PASS`。
  - 菜单迁移必须同步补齐 `system_role_menu` 和需要的 `system_tenant_package.menu_ids`，不能只改 `system_menu` 结构。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。不存在旧菜单可见性兜底、重复菜单并存或静默保留双结构。
- `是否从根因和长期维护角度解决`：是。通过一次性迁移父子菜单、角色绑定和套餐菜单，保证新旧导航与授权树口径一致。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 原角色管理改为目录父菜单 -> Given system_menu 中存在 101 号角色管理菜单 / When 执行迁移 SQL / Then 101 必须变为目录型父菜单并保留在系统管理原排序位置。`
- `BDD: 旧角色管理权限迁移到新权限角色子菜单 -> Given 角色已经绑定旧 101 菜单或其按钮权限 / When 执行迁移 SQL / Then 系统新增权限角色子菜单并把原角色可见性迁移到新子菜单，不丢失菜单树。`
- `BDD: 组织角色和审批角色收拢到角色管理下 -> Given 角色已经绑定旧 104 或 6804 菜单 / When 执行迁移 SQL / Then 两个菜单迁移到 101 号父菜单下，且其绑定角色自动补齐父目录 101。`
- `BDD: 租户套餐补齐新权限角色菜单 -> Given system_tenant_package.menu_ids 已包含旧 101 菜单 / When 执行迁移 SQL / Then 套餐菜单集合补齐新的权限角色子菜单编号，避免后续套餐同步遗漏该入口。`

## 里程碑

1. M1：阻塞旧任务并创建后端任务包。已完成。
2. M2：补菜单迁移静态 RED 契约与证据骨架。已完成。
3. M3：实现增量 SQL 与基线 SQL 更新。已完成。
4. M4：运行 GREEN 静态验证并完成 schema evidence 校验。已完成。

## Cleanup Keep

- `doc/tasks/20260626-role-management-split-rename-navigation/database-schema-evidence.md`

## 预期验证

- `node ..\yudao-ui-admin-vue3\tests\e2e\role-management-split-static.spec.js`
- `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-role-management-split-rename-navigation\database-schema-evidence.md`
