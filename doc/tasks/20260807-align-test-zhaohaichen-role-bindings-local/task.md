# 测试服 zhaohaichen 角色与菜单授权对齐本机

## Task Goal

在测试服务器 `172.30.30.58` 的 `tenant_id=1` 中，将 `zhaohaichen / 赵海辰` 的业务入口与本机同租户账号对齐：保留 `approval_center_entry`，分配测试服独立 `wenkong_no_download` 角色，并使该角色包含 `基础数据`、`电子签名`、`文控中心` 三个根菜单；同时继续禁止该账号获得 DCC 文件下载能力。

## Milestones

- [x] M1 明确用户批准范围和无下载约束。
- [x] M2 完成规则、经验、真实 schema、当前权限、Git 脏工作区基线和回滚前置核对。
- [x] M3 记录 BDD 与 RED 证据，确认直接绑定现有 `wenkong` 会恢复下载能力。
- [x] M4 在单事务内创建并分配独立 `wenkong_no_download` 角色，补齐安全菜单并清理精确用户角色缓存。
- [x] M5 独立复验角色、三个菜单入口、动态授权和 DCC 下载能力。
- [ ] M6 完成 evidence validator、任务清理、提交和推送。

## Expected Verification

- 测试服目标用户有效角色业务码集合为 `approval_center_entry, wenkong_no_download`；这是在保留本机三个业务入口的同时满足无下载约束的正式角色映射。
- 测试服目标用户通过有效角色可获得根菜单 `6800/900218/990200`。
- `doc_control` 绑定保持删除，不恢复此前移除的下载能力。
- 按当前 DCC 下载判定口径复验 `zhaohaichen effective_can_download=0`。
- 事务写入前后均记录精确行快照、影响行数和回滚 SQL 所需主键。
- 数据库 evidence validator、cleanup preview/apply、Git 提交和 `origin/int_main` 推送通过。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；只读 RED 证明测试服现有 `wenkong` 同时拥有目录管理与显式下载权限。用户要求继续后，采用独立 `wenkong_no_download` 正向授权角色，恢复三个入口和必要安全子菜单，同时从权限模型上排除下载放行来源。
- `是否存在临时补丁或绕过`：否。

## Data Safety And Rollback

- 仅允许在测试服 `tenant_id=1` 创建 `wenkong_no_download`，分配给 `zhaohaichen`，并绑定经过白名单核对的菜单 `6800/6806/6807/6814/6818/900218/900418/990200/990210/990216`。
- 不恢复 `doc_control`，不修改角色定义、菜单定义、DCC 类别权限规则、目录权限规则或其他用户绑定。
- 写入前记录目标 `system_user_role`、`system_role_menu` 行的主键、删除标记、审计字段和当前有效下载判定；失败时事务回滚。
- 写入后若任一验收断言失败，按任务角色业务码精确软删除本次用户角色、角色菜单和角色定义，并重新复验下载能力。

## Experience Gate Summary

- 角色、菜单和用户关系必须通过标准 `system_role -> system_role_menu -> system_user_role` 正向授权链核对，不能用前端隐藏、管理员旁路或宽泛角色授权代替。
- 非 admin 账号的角色数据变更后必须清理精确 `user_role_ids:{userId}` 缓存，并重新读取登录权限响应。
- 测试服 MySQL 使用 SSH stdin 进入 `intruoyi-mysql/ruoyi-vue-pro`，不在本地或日志展开数据库密钥；中文结果使用 UTF-8 安全通道。
- DCC 下载能力不能只看下载菜单；后端 `hasDirectoryManagementPermission` 命中目录管理或访问规则管理权限后，会在下载判定中直接放行类别与目录校验。

## Approved Formal Solution

- 用户在收到现有 `wenkong` 会恢复下载能力的阻塞说明后回复“继续”，批准继续采用独立无下载角色方案。
- `wenkong_no_download` 只包含三个根入口、文件上传、受控浏览、审批任务只读入口、文控日志、我的签名、DCC 项目代码查询和 DCC 产品目录。
- 角色不得拥有 `dcc:controlled-file:directory:manage`、`dcc:controlled-file:access-rule:manage`、`dcc:controlled-file:category:manage`、`dcc:controlled-file:download`，也不得成为类别或目录下载规则主体。

## Cleanup Keep

- doc/tasks/20260807-align-test-zhaohaichen-role-bindings-local/task.md
- doc/tasks/20260807-align-test-zhaohaichen-role-bindings-local/execution-log.md
- doc/tasks/20260807-align-test-zhaohaichen-role-bindings-local/verification-report.md
- doc/tasks/20260807-align-test-zhaohaichen-role-bindings-local/change.sql
- doc/tasks/20260807-align-test-zhaohaichen-role-bindings-local/verify.sql
- doc/tasks/20260807-align-test-zhaohaichen-role-bindings-local/rollback.sql
