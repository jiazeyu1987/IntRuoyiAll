# 20260525 NAS 备份真实验证执行日志

## BDD

- BDD: 真实立即备份写入 NAS 根目录备份文件夹 -> Given 备份配置指向 `/mnt/nas/备份`，When 执行 `backup-now`，Then 新备份点创建在 `/mnt/nas/备份/<yyyyMMdd-HHmmss>/`。
- BDD: 旧路径不再接收新备份点 -> Given 旧路径 `/mnt/nas/int-ruoyi/backups` 存在或曾经存在，When 执行本次备份，Then 旧路径下不存在本次新备份点。
- BDD: 备份点产物结构完整 -> Given 本次备份成功，When 检查新备份点，Then `mysql`、`objects`、`deploy`、`manifest` 均存在。

## Evidence

- PRECHECK: `ssh root@172.30.30.58` -> PASS, `/mnt/nas` exists and writable; `/mnt/nas/备份` missing before this verification; old path latest entries include `20260524_180051`, `20260524_183322`, `20260525_013003`.
- BACKUP: `powershell -NoProfile -ExecutionPolicy Bypass -File script\backup-ops\scripts\backup-ops.ps1 -Mode backup-now -NonInteractive -OperatorName Codex` -> PASS, `INTBK-0000`, backup point `20260525-103432`.
- VERIFY: `ssh root@172.30.30.58` -> PASS, `/mnt/nas/备份/20260525-103432` exists with `mysql`, `objects`, `deploy`, `manifest`; `manifest.json` backupId is `20260525-103432`, status is `success`; MySQL artifact `ruoyi-vue-pro.sql.gz`; object entry `yudao`; deploy files `docker-compose.yml`, `image-tag.txt`, `runtime.env`.
- VERIFY: `ssh root@172.30.30.58` -> PASS, `/mnt/nas/int-ruoyi/backups/20260525-103432` does not exist; old path latest entries remain `20260524_180051`, `20260524_183322`, `20260525_013003`.
- CLEANUP PREVIEW: `task_closeout.py --task-id 20260525-nas-backup-real-verification --mode preview --extra-delete script\backup-ops\tmp\20260525-103432` -> PASS, delete only local temporary backup workspace, blocked/warnings empty.
- CLEANUP APPLY: `task_closeout.py --task-id 20260525-nas-backup-real-verification --mode apply --extra-delete script\backup-ops\tmp\20260525-103432 --worktree-closeout off` -> PASS, deleted `script\backup-ops\tmp\20260525-103432`; NAS backup point retained.
