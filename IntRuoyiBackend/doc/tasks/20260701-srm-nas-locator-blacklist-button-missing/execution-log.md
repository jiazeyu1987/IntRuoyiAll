BDD: 页面按钮受 config 权限控制 -> Given NAS定位 页面源码存在黑名单按钮 / When 用户未拥有 `srm:nas-locator:config` / Then 按钮应被前端指令隐藏。
BDD: 环境未落菜单 SQL 时缺口必须可读 -> Given system_menu 中缺少 991104 / When 查询菜单与套餐 / Then 能明确指出菜单未安装或套餐未扩展。
BDD: 当前账号缺权限时能定位到角色菜单绑定 -> Given 菜单 991104 已存在 / When 查询当前用户角色与 role_menu / Then 能判断按钮是否因角色未绑定而隐藏。
INFO: previous-task-completed -> PASS，后端上一任务已完成，可直接开始本轮权限排查。
GREEN: menu-sql-audit -> PASS，`20260701_srm_t6_nas_locator_blacklist_config.sql` 使用 `id=991104` 作为 NAS 黑名单菜单，但当前真实库 `system_menu.id=991104` 已被 `文控管理员 / dcc:controlled-file:category:manage` 占用，形成 ID 冲突。
GREEN: runtime-role-audit -> PASS，测试租户 `122` 中 `aoteman` 已绑定 `super_admin` 与 `srm_admin`，二者仅持有 `991100~991103`，没有任何 `srm:nas-locator:config` 菜单记录，因此按钮被前端权限指令隐藏是符合当前库状态的。
