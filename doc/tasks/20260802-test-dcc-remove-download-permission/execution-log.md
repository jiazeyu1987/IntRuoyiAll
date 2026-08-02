# Execution Log

## User Intent

- 用户要求移除测试服务器 `芋道源码 / zhaohaichen / 赵海辰` 与 `芋道源码 / wangsiyu / 王思雨` 的 DCC 文件下载权限，并随后要求继续执行。

## BDD

- BDD: 目标账号不能下载 DCC 文件 -> Given 测试服存在 `zhaohaichen` 与 `wangsiyu` 两个启用账号且当前有 DCC 文件实际下载能力, When 移除造成下载能力的账号角色绑定并清理权限缓存, Then 两个账号不再命中后端下载判定中的目录管理或类别/目录下载规则。
- BDD: admin 下载能力保留 -> Given 测试服 `admin` 是基准管理员账号, When 移除两个目标账号的下载权限, Then `admin` 仍保留 DCC 文件实际下载能力。

## Command And Evidence Log

- 2026-08-02：读取 `docs/task-closeout-rules.md`、`docs/server-access.md`、`docs/database-rules.md`、`docs/login-access.md`、`docs/powershell-encoding.md`、`docs/release-backup-restore.md`、`docs/powershell-memory.md` 和相关经验索引。
- 2026-08-02：创建独立 worktree `D:\IntRuoyiWorktree\20260802-test-dcc-download-permission`，分支 `codex/20260802-test-dcc-download-permission`，用于记录本次测试服权限数据变更证据。
- 2026-08-02：只读核对测试服后端健康检查 `http://172.30.30.58:48081/actuator/health` 返回 `UP`。
- 2026-08-02：只读核对测试服 MySQL 容器业务库为 `ruoyi-vue-pro`，关键表 `system_users`、`system_role`、`system_user_role`、`system_role_menu`、`system_menu`、`dcc_file_category_permission_rule`、`dcc_directory_access_rule` 存在。
- 2026-08-02：只读盘点确认当前有效下载账号包括 `admin`、`zhaohaichen`、`wangsiyu`、`aoteman`、`showroomeditor`、`showroomsupervisor`、`showroomviewer`、`yingtai`；其中 `dccdownloader` 仅有下载菜单但不满足文件规则，不能实际下载。
- RED: pre-change readonly SQL -> FAIL expected reason, `zhaohaichen` 命中目录管理下载放行，`wangsiyu` 同时命中目录管理和类别/目录下载规则，两个目标账号仍具备实际下载能力。
- RED: first write SQL -> FAIL, MySQL `ERROR 1267 Illegal mix of collations`；确认目标绑定仍为未删除，未提交任何权限变更。
- GREEN: corrected write SQL with `utf8mb4_unicode_ci` temp table -> PASS, 精确更新 `system_user_role` 三条绑定：`zhaohaichen/doc_control`、`wangsiyu/doc_control`、`wangsiyu/wenkong_download`，不改共享角色、菜单、类别规则或目录规则。
- GREEN: target post-change readonly SQL -> PASS, `admin effective_can_download=1`，`zhaohaichen effective_can_download=0`，`wangsiyu effective_can_download=0`。
- GREEN: full post-change effective account SQL -> PASS, 剩余可下载账号为 `admin`、`aoteman`、`showroomeditor`、`showroomsupervisor`、`showroomviewer`、`yingtai`，目标账号已不在清单中。
- 2026-08-02：执行 Redis 精确缓存清理 `DEL user_role_ids::376 user_role_ids:376 user_role_ids::910250 user_role_ids:910250`，返回 `0`；变更前 scan 未发现目标用户显式缓存 key。
- 2026-08-02：运行 `task_closeout.py --task-id 20260802-test-dcc-remove-download-permission --mode preview`；preview 保留三份核心任务记录、无 delete 项，blocked 为主工作区 `E:\IntRuoyi` 脏状态，不能接收 ff-only merge。

## Blockers

- 数据变更与复验无阻塞；独立 worktree closeout apply、ff-only merge 和 worktree 删除被主工作区 `E:\IntRuoyi` 脏状态阻塞。
