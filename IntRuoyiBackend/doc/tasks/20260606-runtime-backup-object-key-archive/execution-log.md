# 执行日志：修复备份恢复对特殊 MinIO 对象键的处理

BDD: 特殊对象键可备份 -> Given MinIO bucket 中存在 `D:\ProjectPackage\...\file.dcc` 形式对象键 / When 执行 `backup-now` 到测试服备份目录 / Then 备份产物保留原始对象键且不因 NAS 文件名限制失败。

BDD: 特殊对象键可恢复 -> Given 备份点包含带 Windows 绝对路径字符的对象键 / When 执行 `restore-data` 到测试服 / Then 恢复后 MinIO bucket 中对象键与备份前一致。

## Evidence

- RED: 运行控制台 UI `立即备份` 选择测试服 -> FAIL，操作 `ecbbcba8-6b90-4ec8-9a80-aaa5cd89413b`，命令参数包含 `-Mode backup-now -TargetEnvironment test`，MinIO 备份阶段失败：`mc mirror` 尝试在 `/mnt/nas/Backup/BackupPackage/20260606-135859/objects/yudao/D:\ProjectPackage\...` 创建目录，返回 `invalid argument`。
- RED: `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py -k "archives_objects_on_remote_nas or remote_nas_object_backup_plan" -q` -> FAIL，当前远端对象备份仍直接 `mc mirror` 到 NAS bucket 目录，缺少 Docker volume、tar 归档和 `remoteArchivePath`。
- GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py -k "archives_objects_on_remote_nas or remote_nas_object_backup_plan" -q` -> PASS，2 passed；远端对象备份计划改为 Docker volume + tar 归档，marker 写入 `remoteArchivePath`。
- GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py -q` -> PASS，46 passed。
- INFO: `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_tooling.py script\tests\test_backup_ops_linux_runtime_ports.py -q` -> FAIL，1 个既有静态断言仍期待 Linux 目标环境只支持 `backup-now/restore-data`，但当前代码已包含 `rollback-app`；该失败不属于本次 PowerShell 运行控制台备份/恢复路径。
- GREEN: `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py -q` -> PASS，49 passed。
- GREEN: `powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "Import-Module Pester -ErrorAction Stop; Invoke-Pester -Path 'script\backup-ops\tests\DockerOps.Tests.ps1' -PassThru"` -> PASS，1 passed。
- GREEN: `mvn -pl yudao-module-infra '-Dtest=RuntimeControlServiceImplTest,RuntimeRestoreCandidateServiceImplTest' test` -> PASS，61 tests，0 failures。
- GREEN: UI 点击“立即备份”选择测试服 -> PASS，操作 `4c0ce2bf-36d7-47bb-9431-231ff2907e40`，`targetEnvironment=test`，备份点 `20260606-222106`，结果 `succeeded / INTBK-0000`。
- GREEN: UI 点击“恢复数据到测试服” -> PASS，操作 `493b784c-8f00-479d-91fe-fa220f17dc81`，`targetEnvironment=test`，`selectedBackupId=20260606-222106`，结果 `succeeded / INTBK-0000`。
- GREEN: 测试服健康检查与正式服边界检查 -> PASS，前端/后端健康检查通过，本轮 Playwright 未提交正式服目标操作。
