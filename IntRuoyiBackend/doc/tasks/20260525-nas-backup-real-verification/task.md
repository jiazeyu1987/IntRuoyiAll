# 20260525 NAS 备份真实验证

## 任务目标

执行一次真实 `backup-now`，验证新备份点写入 NAS 根目录 `/mnt/nas/备份`，且旧路径 `/mnt/nas/int-ruoyi/backups` 不再产生新的备份点。

## 里程碑

- [x] M1：记录备份前旧路径和新路径状态。
- [x] M2：执行一次真实立即备份。
- [x] M3：验证新 NAS 路径存在新备份点并包含 `mysql`、`objects`、`deploy`、`manifest`。
- [x] M4：验证旧路径未产生本次新备份点。
- [x] M5：记录证据、执行 task-closeout-cleanup 预览并提交任务文档。

## 预期验证

- 测试服务器 `/mnt/nas/备份` 存在并可访问。
- 执行 `backup-now` 成功，返回 `INTBK-0000`。
- 新备份点名称符合 `yyyyMMdd-HHmmss`。
- 新备份点包含 `mysql`、`objects`、`deploy`、`manifest` 子目录。
- 旧路径 `/mnt/nas/int-ruoyi/backups` 不包含本次新备份点。

## 当前状态

状态：已完成。

最终验证：真实 `backup-now` 成功，返回 `INTBK-0000`，新备份点为 `/mnt/nas/备份/20260525-103432`；旧路径 `/mnt/nas/int-ruoyi/backups/20260525-103432` 不存在。

收尾结果：`task_closeout.py --task-id 20260525-nas-backup-real-verification --mode apply --extra-delete script\backup-ops\tmp\20260525-103432 --worktree-closeout off` 通过，已删除本次本地中转目录，无阻塞或警告。

## Current Status

completed
