# Database Schema Evidence

## Data Change Goal

- 为本机 `芋道源码` 租户补齐 DCC DHF/DMR 类别上传权限角色，解除真实 E2E 中 `uploadableProductRequired=0` 的权限前置阻塞。

## Affected Entities

- `system_role`：新增或复用 `dcc_dhf_dmr_uploader`。
- `system_role_menu`：绑定 `文控中心` 与 `文件上传` 菜单。
- `system_user_role`：将角色分配给本机默认 E2E 用户 `admin`。
- `dcc_file_category_permission_rule`：为 `芋道源码` active DHF/DMR 类别创建 `UPLOAD` + `ROLE` + `GLOBAL` 规则。

## Database Engine And Scope

- Engine：本机 Docker MySQL `int-ruoyi-mysql` / `ruoyi-vue-pro`。
- Tenant：`system_tenant.id=1`，`name=芋道源码`。
- Scope：本机 E2E 数据前置，不触碰远端环境。

## Schema Evidence

- `system_role` 存在 `id/name/code/category_id/data_scope/status/type/deleted/tenant_id`，`id` 自增。
- `system_role_menu` 存在 `role_id/menu_id/deleted/tenant_id`，`id` 自增。
- `system_user_role` 存在 `user_id/role_id/deleted/tenant_id`，`id` 自增。
- `dcc_file_category_permission_rule` 存在 `category_id/action_type/subject_type/subject_id/scope_type/active/deleted/tenant_id`，并有唯一键 `uk_dcc_category_permission_subject(category_id,action_type,subject_type,subject_id)`。

## BDD Scenarios

- `BDD: DCC上传角色授予类别上传 -> Given 芋道源码没有 DHF/DMR 类别 UPLOAD 角色规则 / When 创建 DCC DHF/DMR 上传员并绑定 active DHF/DMR 类别 / Then admin 重新登录后至少能看到已绑定目录的 DHF/DMR 类别。`
- `BDD: 不扩大目录绑定 -> Given 当前仅 1 个 DHF/DMR 类别绑定了提交目录 / When 创建上传角色 / Then 只改变上传授权，不补造目录绑定。`

## RED Evidence

- `RED: readonly permission preflight -> FAIL, tenant=1 active DHF/DMR=59，directory_bound=1，UPLOAD ROLE rules=0，admin 可用 DHF/DMR 上传类别=0。`

## RED Command

- `local role seed transaction attempt #1` -> FAIL, MySQL `ERROR 1267 Illegal mix of collations`，事务未提交；按数据库规则改为显式 `SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci` 后重试。

## Rollback Plan

- 删除 `dcc_file_category_permission_rule` 中 `subject_type='ROLE'` 且 `subject_id=<dcc_dhf_dmr_uploader role id>` 的本任务新增规则。
- 删除 `system_user_role` 中 `user_id=1` 且 `role_id=<dcc_dhf_dmr_uploader role id>` 的本任务新增绑定。
- 删除 `system_role_menu` 中该角色的菜单绑定。
- 将 `system_role.code='dcc_dhf_dmr_uploader'` 标记删除或物理删除；本次为本机 E2E 前置，未触碰业务文件数据。

## Blockers

- 暂无；用户已授权若无现成上传角色则创建。

## Migration

- Type：本机 E2E 权限前置数据 seed，不涉及表结构迁移。
- Applied change：新增或复用 `dcc_dhf_dmr_uploader` 角色，绑定菜单 `6800/6806`，分配给 `admin`，为 59 个 active DHF/DMR 类别写入 `UPLOAD/ROLE/GLOBAL` 规则。
- Migration tool：Docker MySQL stdin SQL，事务内执行，首次排序规则失败后确认半写入为 0，再显式 `SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci` 重试。

## Safety

- 目标限定为本机 `int-ruoyi-mysql` / `ruoyi-vue-pro` / `tenant_id=1`。
- 不修改远端环境，不修改 DCC 文件、目录绑定或业务文件记录。
- 写入均使用稳定 role code 和 `NOT EXISTS` 防重复；`dcc_file_category_permission_rule` 依赖唯一键防止重复主体规则。

## GREEN: local DCC upload role seed

- `local DCC upload role seed transaction` -> PASS。
- 回查：`role_id=910414`，`admin_user_id=1`，`role_count=1`，`role_menu_count=2`，`admin_binding_count=1`，`upload_rule_count=59`。

## Verification

- DB readback：`DCC DHF/DMR上传员 / dcc_dhf_dmr_uploader / category=文控 / assigned_users=admin`。
- DB readback：该角色 active `UPLOAD` 类别规则数为 59；已绑定目录且可上传的 DHF/DMR 类别数为 1。
- Browser read-only probe：重新登录 `芋道源码/admin` 后 `uploadableProductRequired=1`，命中 `DCC_FVM_DHF_001 / 市场调研报告 / directoryId=906469`；未发送 DCC 写请求，console error 为 0。
- Browser product-number probe：选择项目 `按压式球囊扩充压力泵 / IDI` 后，产品编号显示 `IDI`；未查询其它业务数据源选项。
