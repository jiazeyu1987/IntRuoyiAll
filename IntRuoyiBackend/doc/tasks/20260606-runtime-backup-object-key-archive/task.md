# 任务：修复备份恢复对特殊 MinIO 对象键的处理

## 任务目标

修复运行控制台“立即备份测试服”在 MinIO 对象键包含 Windows 绝对路径字符时失败的问题，使备份与恢复链路能完整保留真实对象键，并继续保持 fail-fast，不跳过任何对象。

## Parent Task

- 根任务：`D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260606-runtime-console-test-backup-restore\task.md`
- 失败操作号：`ecbbcba8-6b90-4ec8-9a80-aaa5cd89413b`

## Previous Task Check

- 同仓库前序任务：`doc/tasks/20260606-showroom-product-attachment-save-preview-fix/task.md`。
- 检查结果：状态为 `completed`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。修复不得跳过异常对象键，不得把备份失败标记成功。
- `是否从根因和长期维护角度解决`：是。对象备份必须保留 MinIO 原始对象键，不依赖 NAS 文件系统能直接承载对象键中的 `:`、`\` 等字符。
- `是否存在临时补丁或绕过`：否。不删除测试服对象，不改写测试服业务数据，不清理 MinIO 异常键来绕过备份。

## BDD 场景

- BDD: 特殊对象键可备份 -> Given MinIO bucket 中存在 `D:\ProjectPackage\...\file.dcc` 形式对象键 / When 执行 `backup-now` 到测试服备份目录 / Then 备份产物保留原始对象键且不因 NAS 文件名限制失败。
- BDD: 特殊对象键可恢复 -> Given 备份点包含带 Windows 绝对路径字符的对象键 / When 执行 `restore-data` 到测试服 / Then 恢复后 MinIO bucket 中对象键与备份前一致。

## 里程碑

- [x] M1：记录运行控制台失败和前置任务状态。
- [x] M2：新增 RED 回归测试覆盖特殊对象键。
- [x] M3：实现对象备份/恢复的正式修复。
- [x] M4：运行目标测试，重新从 UI 执行立即备份和恢复数据到测试服。
- [x] M5：记录最终证据并完成收尾。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py -k object`
- 运行控制台 UI 重新执行“立即备份”选择测试服并成功。
- 备份成功后运行控制台 UI 执行“恢复数据”选择测试服并成功。

## 当前状态

completed

## Current Status

completed

## 当前实现结果

- 已将远端对象备份从直接 `mc mirror` 到 NAS 目录改为：测试服 Docker volume 中镜像对象桶，再用归档镜像生成 `objects/objects-<bucket>.tar` 写入 NAS。
- `remote-object-backup.json` 现在记录 `mode=remote-nas-archive`、`remoteArchivePath` 与 `objectKeyPreservation=tar`。
- 恢复数据时先读取远端归档 marker，再在测试服用 Docker volume 解包并镜像回目标 MinIO，避免在 Windows 本机或 NAS 文件树上展开特殊对象键。

## 最终验证

- `python -X utf8 -m pytest script\tests\test_backup_ops_tooling.py -q` -> PASS，49 passed。
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "Import-Module Pester -ErrorAction Stop; Invoke-Pester -Path 'script\backup-ops\tests\DockerOps.Tests.ps1' -PassThru"` -> PASS，1 passed。
- `mvn -pl yudao-module-infra '-Dtest=RuntimeControlServiceImplTest,RuntimeRestoreCandidateServiceImplTest' test` -> PASS，61 tests，0 failures。
- UI 点击“立即备份”选择测试服 -> PASS，操作 `4c0ce2bf-36d7-47bb-9431-231ff2907e40`，备份点 `20260606-222106`，`targetEnvironment=test`，结果 `succeeded / INTBK-0000`。
- UI 点击“恢复数据到测试服” -> PASS，操作 `493b784c-8f00-479d-91fe-fa220f17dc81`，`selectedBackupId=20260606-222106`，`targetEnvironment=test`，结果 `succeeded / INTBK-0000`。
- 测试服前端、后端健康检查通过；本轮 Playwright 未提交正式服目标操作。
