# 测试服文控角色菜单解绑验证报告

## Scope

- 环境：测试服务器 `172.30.30.58`。
- 数据库：容器 `intruoyi-mysql`，业务库 `ruoyi-vue-pro`。
- 租户：`tenant_id=1`。
- 角色：`system_role.id=910233`，`code=doc_control`，`name=文控`。
- 用户影响面：`zhaohaichen / 赵海辰` 通过 `doc_control` 继承菜单。
- 目标菜单：`101 角色管理`、`900183 权限角色`、`6804 审批角色`。

## Verification Result

PASS。

## Evidence

- 写入前目标绑定数：`3` 行。
- 写入事务结果：`UPDATED_ROWS=3`，`TX_RESULT=COMMITTED`。
- 写入后目标活动绑定数：`0` 行。
- 写入后目标行状态：主键 `905348/904555/905535` 均 `deleted=1`，`updater=20260730-remove-doc-control-role-menus-test`。
- 用户菜单继承复验：`zhaohaichen` 按启用角色链路查询 `101/6804/900183` 无返回行。
- 缓存核对：登录权限信息接口的角色菜单读取路径未缓存 `getRoleMenuListByRoleId(Collection<Long>)`，不需要额外清 Redis；已登录浏览器侧可能需要刷新或重新登录以重新拉取菜单树。

## Non-Scope

- 未删除 `system_menu` 菜单定义。
- 未修改 `system_tenant_package`。
- 未修改 `system_users`、`system_user_role` 或其它角色绑定。
- 未修改正式服务器、备用服务器或本地运行库。
