# 执行日志：发布当前代码到测试服务器并验证备份恢复任务

BDD: test server deploy scope decision -> Given 当前工作区包含 backup-ops 相关改动和大量无关本地改动 When 选择发布当前代码到测试服务器 Then 必须先明确发布边界，不能在未确认的情况下把无关改动一起部署到测试环境
GREEN: deploy scope decision -> PASS, 用户于 2026-05-21 明确批准仅发布已提交版本，并指定以提交 35748935db 作为隔离发布源
GREEN: publish backup-ops bundle to test server -> PASS, 已将提交 35748935db 的 `script/backup-ops` 发布到 `/opt/intruoyi/ops/backup-ops/35748935db/backup-ops`
RED: ssh root@172.30.30.58 "cd /opt/intruoyi/ops/backup-ops/35748935db/backup-ops && pwsh -NoProfile -ExecutionPolicy Bypass -File ./scripts/backup-ops.ps1 -Mode backup-now -ConfigPath ./config/backup-ops.config.json -SecretsPath ./config/backup-ops.secrets.json -NonInteractive" -> FAIL, 测试服务器本机缺少 `pwsh`，错误原文：`bash: pwsh: 未找到命令`
GREEN: powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\scripts\backup-ops.ps1 -Mode backup-now -ConfigPath D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\tmp\backup-ops-test-runtime.config.json -SecretsPath D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\config\backup-ops.secrets.json -NonInteractive -> PASS, 目标测试服务器 172.30.30.58 生成真实备份点 20260521_100616
GREEN: ssh root@172.30.30.58 "find /backup/int-ruoyi/backups/20260521_100616 -maxdepth 4 | sort" -> PASS, 备份点包含 deploy / manifest / mysql / objects
GREEN: powershell -NoProfile -ExecutionPolicy Bypass -File D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\scripts\backup-ops.ps1 -Mode restore-data -ConfigPath D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\tmp\backup-ops-test-runtime.config.json -SecretsPath D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\config\backup-ops.secrets.json -SelectedBackupId 20260521_100616 -NonInteractive -> PASS, 目标测试服务器 172.30.30.58 完成真实恢复
