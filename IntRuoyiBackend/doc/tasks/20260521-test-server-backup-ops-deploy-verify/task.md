# 任务：发布当前代码到测试服务器并验证备份恢复任务

## 目标

将当前代码发布到测试服务器 `172.30.30.58`，随后在测试服务器环境上执行一次真实备份/恢复验证，并判断是否成功；如果失败，必须记录失败原因与影响。

## 范围

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\deploy\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-test-server-backup-ops-deploy-verify\**`

## 非范围

- 不修改正式服务器
- 不在本任务中接入真实 webhook
- 不处理与备份恢复验证无关的其他业务需求

## 上一任务检查

- 上一任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-backup-ops-v2-real-integration\task.md`
- 状态：`completed`
- 说明：上一任务已完成 `backup-ops` 真实接线并按用户批准收口为最终放行；本任务基于该结果继续做测试服务器发布与联调验证。

## 里程碑

- [x] M1：创建任务文档并确认验证目标。
- [x] M2：确认发布边界并选择发布源。
- [x] M3：发布到测试服务器。
- [x] M4：执行真实备份/恢复验证。
- [x] M5：记录结果并完成收尾。

## 预期验证

- 测试服务器发布脚本执行成功
- 真实执行一次 `backup-now` 或等效备份验证
- 真实执行一次 `restore-data` 或等效恢复验证
- 成功则记录结果；失败则记录原因、影响、日志和下一步建议

## 当前状态

Completed.

## 当前进展

- 用户已于 `2026-05-21` 明确选择：
  - 只发布**最新已提交**的 `backup-ops` 版本
  - 以提交 `35748935db` 作为测试服务器发布源
- 本任务将使用隔离发布源，避免把当前工作区中的无关本地改动带到测试服务器。
- 已将提交 `35748935db` 对应的 `script/backup-ops` 目录发布到测试服务器固定目录：
  - `/opt/intruoyi/ops/backup-ops/35748935db/backup-ops`
- 已尝试在测试服务器本机执行 `backup-now`，但远端环境缺少 `pwsh` / `powershell`，因此无法直接运行当前 PowerShell 脚本。
- 已改用**当前 Windows 运维机执行、目标指向测试服务器**的支持模式进行真实验证：
  - `backup-now` 成功
  - `restore-data` 成功
  - 这说明测试服务器不需要安装 PowerShell，也能完成真实备份/恢复任务

## 最终验证结果

- PASS：隔离发布源
  - 发布源提交：`35748935db`
- PASS：发布到测试服务器
  - 远端目录：`/opt/intruoyi/ops/backup-ops/35748935db/backup-ops`
- FAIL（已分析）：测试服务器本机直接执行
  - 原因：Linux 测试服务器缺少 `pwsh` / `powershell`
  - 结论：当前实现不支持“测试服务器本机直接运行 PowerShell 脚本”
- PASS：Windows 运维机执行 `backup-now`，目标指向测试服务器
  - 备份点：`20260521_100616`
  - 日志：`D:\IntRuoyi-BackupOps\logs\202605\20260521_100615_backup-now_success.log`
- PASS：Windows 运维机执行 `restore-data`，目标指向测试服务器
  - 恢复点：`20260521_100616`
  - 日志：`D:\IntRuoyi-BackupOps\logs\202605\20260521_100808_restore-data_success.log`

## 结论

- 如果要求“测试服务器本机直接执行 PowerShell 版 backup-ops”，当前结果是不成功，根因是缺少 PowerShell 运行时。
- 如果按当前设计的推荐模式“Windows 运维机执行，通过 SSH 操作测试服务器”，本次真实备份与恢复验证都成功。
