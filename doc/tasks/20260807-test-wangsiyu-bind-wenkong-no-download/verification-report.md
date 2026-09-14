# 验证报告

## Result

PASS：测试服 `tenant_id=1/芋道源码` 的 `wangsiyu(id=910250)` 已绑定 `wenkong_no_download(910417)`，文控入口权限已恢复，且没有恢复下载权限。

## Database Verification

- 变更结果：`change.sql` 单事务提交成功，新增 `system_user_role.id=4236`。
- 有效角色：`approval_center_entry(910295)`、`wenkong_no_download(910417)`。
- 根菜单解析：`6800 文控中心`、`900218 电子签名`、`990200 基础数据` 均由 `wenkong_no_download` 解析。
- 无下载边界：`wenkong_no_download` 菜单数 `10`，危险菜单权限计数 `0`；角色、用户、岗位、部门链的类别/目录下载规则计数均为 `0`；活动动态授权计数 `0`。

## Cache Verification

- Redis DB 0：`user_role_ids:910250` / `user_role_ids::910250` 写前不存在，删除数 `0`。
- Redis DB 1：写前存在 `user_role_ids:910250`，删除数 `1`，删除后不存在。
- 未执行全库 Redis 清理。

## Rollback

- `rollback.sql` 已通过远端 MySQL 语法验证，未执行业务回滚。
- 如需撤销，仅软删除本任务 `creator='codex-20260807-wangsiyu-wenkong-no-download'` 创建的 `wangsiyu -> wenkong_no_download` 绑定，并再次清理精确角色缓存。

## Notes

- 未持有 `wangsiyu` 登录凭据或活动 token，因此未伪造 UI/API 已登录验收。
- `wangsiyu` 需要退出并重新登录测试服，前端才会拉取最新菜单权限。
