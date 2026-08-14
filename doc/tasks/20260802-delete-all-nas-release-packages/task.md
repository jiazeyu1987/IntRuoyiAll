# 删除 NAS 全部发布包

## Task Goal

根据用户明确确认，删除 NAS 发布仓库 `\\172.30.30.4\IT共享\Backup\ReleasePackage` 下当前确认的 210 个顶层发布包目录。

## Scope

- 仅删除 `Backup/ReleasePackage` 下的顶层发布包目录。
- 不删除 `Backup/ReleasePackage` 根目录。
- 不删除 NAS 其它共享目录、备份包目录或根目录中的非发布包文件。
- 不操作测试、正式、备用服务器运行目录和容器。

## Milestones

- [x] M1：读取服务器、发布恢复、PowerShell 编码、Git 与任务收尾规则。
- [x] M2：记录用户不可逆删除确认，保存脏工作区基线。
- [x] M3：删除前重新核对目标路径、顶层目录数量、文件数量和占用空间。
- [x] M4：删除 210 个顶层发布包目录。
- [x] M5：验证发布包顶层目录数量为 0，发布根目录仍存在。
- [x] M6：完成任务收尾记录、提交并推送。

## Expected Verification

- 删除前 NAS 根路径必须精确解析为 `\\172.30.30.4\IT共享\Backup\ReleasePackage`。
- 删除前顶层目录数量必须仍为 210；若数量变化则停止，不扩大用户确认范围。
- 每个删除目标必须是发布根目录的直接子目录，且不得是重解析点。
- 删除后发布根目录仍存在，顶层目录数量为 0。
- 记录删除前后的文件数量、字节数和执行时间。

## Applicable Experience Gates

- Trigger：删除 NAS 共享发布存储中的全部发布包。
- Preflight check：读取 `docs/server-access.md`、`docs/release-backup-restore.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md` 和 `docs/task-closeout-rules.md`；精确核对 NAS 服务、共享名、发布根目录和顶层目录数量。
- Blocker：目标路径不匹配、顶层目录数量不是 210、出现重解析点、凭据缺失、目录不可读或无法证明目标是发布根目录直接子目录时立即停止。
- Verification：删除前后分别统计顶层目录、递归文件数、总字节数，并确认发布根目录保留。
- Forbidden action：禁止删除 NAS 共享根、`Backup/BackupPackage`、其它目录、根目录非发布包文件，禁止切换 NAS、共享、账号或路径。
- Evidence：用户明确回复“确认删除全部210个发布包”；删除前只读统计为 210 个目录、126070 个文件、228875800864 字节。

## Rollback And Recovery

- 本次操作为直接永久删除，不提供回收站、移动暂存或兼容性回退。
- 用户在获知发布、回滚、灾备取包和审计影响后明确确认删除全部 210 个发布包。
- 删除完成后如需恢复，只能重新构建发布包或从项目外部已存在的独立备份恢复；本任务不创建备份。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按用户明确目标清理发布仓库存量，并使用精确路径与数量门禁避免扩大删除范围。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Cleanup Keep

- doc/tasks/20260802-delete-all-nas-release-packages/delete-nas-release-packages.ps1

## Previous Status

`in_progress`
