# 验证报告

## Result

PASS：测试服 `tenant_id=1/zhaohaichen(id=376)` 已获得独立角色 `wenkong_no_download(id=910417)`，三个目标根菜单均已恢复，同时未获得 DCC 下载放行来源。

## Role And Menu Verification

- 有效角色：`approval_center_entry(910295)`、`wenkong_no_download(910417)`。
- 历史 `doc_control(910233)` 绑定继续保持删除，不恢复。
- 根菜单：`6800 文控中心`、`900218 电子签名`、`990200 基础数据` 均通过 `wenkong_no_download` 解析。
- 新角色菜单数：`10`；危险权限计数：`0`；活动动态授权计数：`0`。

## No-Download Verification

- 角色危险权限计数：`0`。
- 角色类别/目录下载规则计数：`0/0`。
- 用户直接类别/目录下载规则计数：`0/0`。
- 岗位：无有效岗位；岗位下载规则计数：`0/0`。
- 部门链：`124,143,226`；部门下载规则计数：`0/0`。
- 结论：按当前后端正式权限链，账号不存在目录管理、访问规则管理、显式下载或类别/目录规则放行来源。

## Operational Verification

- 测试服健康检查：`UP`。
- Redis 仅清理目标用户角色缓存候选键，删除结果为 `0`，未执行全库清理。
- 回滚脚本完成 MySQL 语法验证，未执行业务回滚；验证后无残留存储过程。
- 目标用户活动 OAuth token 为 `0`，本任务没有账号密码，未伪造已登录页面/API 结果。用户重新登录后将拉取最新菜单。

## Rollback

- [rollback.sql](rollback.sql) 只允许在角色仍为本任务专用、未分配其他用户且菜单数仍为 10 时软删除本次新增用户角色、角色菜单和角色定义。
- 回滚后必须清理目标用户角色缓存并再次运行 [verify.sql](verify.sql)。

## Closeout

- Database schema validator self-test：PASS。
- Database evidence validator：PASS。
- Experience consolidation：已合并到现有 `docs/database-rules.md` 并登记索引。
- Cleanup preview/apply：PASS；仅删除 `database-schema-evidence.md`，核心记录和三份 SQL 均保留。
- Git 实现记录提交：`17b68d156`；收尾记录提交与推送待执行。
