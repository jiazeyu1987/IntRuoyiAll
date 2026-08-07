# Database Schema Evidence

## Data Change Goal And Affected Entities

- 环境：测试服务器 `172.30.30.58`。
- 数据库：MySQL，容器 `intruoyi-mysql`，schema `ruoyi-vue-pro`。
- 目标实体：`system_users`、`system_role`、`system_user_role`、`system_menu`、`system_role_menu`；只读核对 `system_entitlement_grant`、DCC 类别/目录权限来源。
- 目标：通过独立 `wenkong_no_download` 对齐 `tenant_id=1/zhaohaichen` 的三个根菜单和必要安全功能，同时维持 DCC 下载禁用。

## Engine And Change Mechanism

- Engine: MySQL 8 compatible runtime.
- Change mechanism: 测试服受控单事务数据修复；不修改 schema，不发布代码，不生成 fallback。

## Data Safety Analysis

- 仅使用稳定业务键 `tenant_id + username`、`tenant_id + role.code` 和固定已核验菜单白名单。
- 写入前锁定目标行并断言账号、角色、菜单定义唯一且启用。
- 不恢复 `doc_control`，不绑定高权限 `wenkong`，不修改共享角色或菜单定义，不影响其他用户。
- 写入前后使用与既有 DCC 下载权限移除任务一致的有效下载判定口径复验。

## Rollback Or Recovery Plan

- 事务内断言失败直接 `ROLLBACK`。
- 提交后验收失败时，按角色业务码精确软删除本次新增的 `wenkong_no_download` 用户角色、角色菜单和角色定义，不触碰任何既有共享角色关系。
- 回滚后重新清理目标用户角色缓存并复验角色、菜单和下载判定。

## BDD Scenarios

- BDD: 安全角色对齐 -> Given 现有 `wenkong` 会恢复下载 / When 测试服创建独立无下载角色 / Then 三个入口可见且高风险权限不进入角色。
- BDD: 菜单对齐 -> Given 三个启用根菜单 / When 通过 `wenkong_no_download` 解析 / Then 三个入口均可见。
- BDD: 下载禁用保持 -> Given `doc_control` 已移除 / When 添加无下载规则的 `wenkong_no_download` / Then 所有正式下载放行来源计数均为 0。

## RED Command And Expected Failure

- RED: 测试服只读差异 SQL -> FAIL as expected：缺少 `wenkong` 用户角色绑定、缺少 `900218` 角色菜单绑定、三个根菜单未全部解析到目标用户。
- RED: 测试服 `wenkong` 权限清单与后端下载判定核对 -> FAIL as expected：角色包含 `dcc:controlled-file:directory:manage` 和 `dcc:controlled-file:download`；直接绑定会恢复下载能力。

## GREEN Command And Passing Result

- GREEN: `change.sql` 经 UTF-8 SSH stdin 在测试服 MySQL 执行 -> PASS：`COMMITTED user_id=376 role_id=910417 role_menu_count=10`。
- GREEN: `verify.sql` 只读复验 -> PASS：有效角色为 `approval_center_entry(910295)`、`wenkong_no_download(910417)`；三个根菜单 `6800/900218/990200` 均由新角色解析；危险权限计数、活动动态授权计数均为 `0`。
- GREEN: 全量下载来源只读复验 -> PASS：角色危险权限、角色类别/目录下载规则、用户直接类别/目录下载规则、岗位类别/目录下载规则、部门链类别/目录下载规则均为 `0`。
- GREEN: 精确 Redis 角色缓存删除 -> PASS：两个候选缓存键删除返回 `0`，不存在待失效键；未执行全库缓存清理。

## Migration Verification

- 本任务不修改 schema；以可重复执行、断言驱动的单事务数据变更代替迁移文件。
- 新角色 `wenkong_no_download(910417)` 有效菜单数为 `10`，白名单精确为 `6800/6806/6807/6814/6818/900218/900418/990200/990210/990216`。
- `doc_control` 用户角色关系继续保持软删除，未绑定测试服共享高权限 `wenkong`。
- `rollback.sql` 已完成 MySQL 存储过程语法验证，未执行业务回滚；验证结束后数据库无残留回滚过程。
- 测试服服务健康检查为 `UP`。

## Blockers

- 无数据变更阻塞。
- 目标用户活动 OAuth token 为 `0`，且本任务没有账号密码，因此无法执行已登录 UI/API 复验；该限制不改变数据库角色、菜单和下载权限链的只读验收结果，也未以 mock 或匿名请求替代。
