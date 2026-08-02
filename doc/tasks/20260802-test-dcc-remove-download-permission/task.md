# 20260802 测试服 DCC 下载权限移除

## Task Goal

在测试服务器 `172.30.30.58` 仅针对租户 `芋道源码` 的 `zhaohaichen / 赵海辰` 与 `wangsiyu / 王思雨` 移除 DCC 文件实际下载能力，并保留可审计的 SQL 前置核对、变更范围和复验结果。

## Milestones

- [x] 创建独立任务 worktree 与任务记录。
- [x] 只读核对测试服健康、业务库名、目标表结构和目标账号当前有效下载来源。
- [x] 执行最小权限数据变更。
- [x] 清理精确用户权限缓存并复验目标账号已无实际下载能力。
- [x] 记录验证报告与收尾状态。

## Expected Verification

- 只读 SQL 证明目标用户在变更前具备 DCC 文件实际下载能力。
- 写入 SQL 仅影响目标用户的 `system_user_role` 绑定，不改动其他用户、角色定义、菜单定义、类别规则或目录规则。
- 复验 SQL 证明 `zhaohaichen` 与 `wangsiyu` 不再具备 DCC 文件实际下载能力。
- 复验 SQL 证明 `admin` 仍具备 DCC 文件实际下载能力。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；当前系统没有用户级下载 deny 规则，本次按账号移除造成实际下载能力的角色绑定，避免改共享角色定义影响其他账号。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 测试服远端 MySQL 查询必须以真实容器和业务库 `ruoyi-vue-pro` 为准，使用 `docker exec -i intruoyi-mysql sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" ruoyi-vue-pro'` 通过 stdin 传 SQL。
- 角色、菜单、租户基线不能凭历史记忆硬编码；写入前必须只读核验真实库中的角色编码、启用状态、菜单权限、账号归属和影响行数。
- 菜单/权限 SQL 需要精确锁定稳定业务键；不得扩大范围、不得删除共享角色定义、不得用 mock 或空结果冒充权限已移除。

## Cleanup Keep

- doc/tasks/20260802-test-dcc-remove-download-permission/task.md
- doc/tasks/20260802-test-dcc-remove-download-permission/execution-log.md
- doc/tasks/20260802-test-dcc-remove-download-permission/verification-report.md
