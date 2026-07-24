# 任务：实现 IT 友好型备份恢复脚本 V2 真实环境接线

## 目标

在已通过独立放行的 phase-1 `backup-ops` 脚手架基础上，继续实现真实环境接线能力，使脚本具备以下真实执行路径：

1. 正式服务器 MySQL 导出
2. 正式对象文件备份到测试服务器
3. 正式环境应用版本回滚
4. 正式环境数据恢复

并在完成后继续走独立 reviewer 放行闭环。

## 范围

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_tooling.py`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-backup-ops-v2-real-integration\**`

## 非范围

- 不在本任务中实现第二阶段 MySQL 专用副本
- 不在本任务中实现 binlog PITR
- 不修改业务前端系统

## 上一任务检查

- 上一任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-backup-ops-v1-review-loop\task.md`
- 状态：`completed`
- 说明：上一任务已完成 phase-1 脚手架并通过独立 reviewer 放行，本任务承接其脚本结构与 operator-facing 约定，继续做真实环境接线。

## 里程碑

- [x] M1：创建任务文档并确认 V2 范围。
- [x] M2：梳理可复用的发布脚本 SSH / MySQL / MinIO 逻辑。
- [x] M3：实现真实备份与回滚/恢复接线。
- [x] M4：运行验证并走独立 reviewer 放行。

## 预期验证

- `python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_tooling.py -q`
- 视实现范围补充的真实命令规格/静态契约测试
- `backup-now` / `rollback-app` / `restore-data` 的真实执行路径不再统一停在 phase-1 blocked 文案

## 当前状态

Completed.

## 当前进展

- 已完成真实配置文件 `backup-ops.config.json` / `backup-ops.secrets.json` 落地，并指向发布脚本中确认过的真实测试/正式服务器地址。
- 已将 `SSH`、`MySQL`、`Docker`、`对象备份`、`目录同步`、`manifest/checksums`、`operator-facing 文案` 接到真实命令路径。
- `backup-now` 已在正式 -> 测试链路上真实成功执行：
  - 正式服务器 `172.30.30.57`
  - 测试服务器 `172.30.30.58`
  - 备份点：`20260520_190020`
- `rollback-app` 已在“把 test server 当 production”临时配置下真实成功执行：
  - 目标 tag：`20260520_113715`
- `restore-data` 已在“把 test server 当 production”临时配置下真实成功执行：
  - 恢复点：`20260520_190020`
- `backup-scheduled` 已在正式 -> 测试链路上真实成功执行：
  - 备份点：`20260520_193708`
- 已补齐 `rehearsal` 独立演练配置、对象恢复到独立 bucket、登录校验、文件抽样下载校验，并完成一次真实恢复演练：
  - 演练备份点：`20260520_190020`
  - 演练报告：`D:\IntRuoyi-BackupOps\logs\202605\20260520_211007_rehearsal_success.report.md`
- 已补齐恢复点治理：
  - `manifest.validation` 新增 `rehearsalStatus / lastRehearsedAt`
  - `Get-BackupOpsRestoreCandidates` 现在要求 `mysqlDumpCreated / objectBackupCreated / checksumsGenerated`
  - `manifest` 解析失败不再 fallback 到 artifact metadata
  - 演练失败会把恢复点降级为 `pending-review`，演练成功可恢复为 `verified`
- 已补齐计划任务注册入口：
  - `script/backup-ops/actions/Register-BackupOpsScheduledTasks.ps1`
  - `-PlanOnly` 可输出 daily backup 与 weekly rehearsal 的任务定义
- 已补齐通知模块真实 webhook 通道支持与状态可见性：
  - `disabled / pending / unsupported / failed / sent` 都会进入结构化结果和日志
  - 已补齐失败/blocked 路径通知、`restore-data` 开始通知、`backup-scheduled` 清理任务独立通知
  - 当前 runtime 配置仍为 `notify.enabled=false`、`channel=pending`，等待真实通知渠道参数
- 已记录放行条件变更：
  - 用户于 `2026-05-21` 明确批准“无 webhook 也允许试运行，并视为最终放行”
  - 变更记录见 `docs/changes/20260521-backup-ops-webhook-waiver.md`

## 最终验证结果（当前）

- PASS：`python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_real_integration_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_rehearsal_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_manifest_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_scheduling_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_notification_flow_tooling.py -q`
  - 结果：`39 passed`
- PASS：`backup-now` 真实执行成功
  - 结果：`INTBK-0000`
  - 备份点：`20260520_190020`
- PASS：测试服务器只读核对 `/backup/int-ruoyi/backups/20260520_190020`
  - 已存在 `deploy/manifest/mysql/objects/yudao`
  - 远端 `manifest.json` 中 `syncedToTestServer=true`
- PASS：`rollback-app` 在 test server 临时联调配置下真实执行成功
  - 结果：`INTBK-0000`
  - IMAGE_TAG：`20260520_113715`
- PASS：`restore-data` 在 test server 临时联调配置下真实执行成功
  - 结果：`INTBK-0000`
  - 恢复点：`20260520_190020`
- PASS：`backup-scheduled` 真实执行成功
  - 结果：`INTBK-0000`
  - 备份点：`20260520_193708`
- PASS：`rehearsal` 真实执行成功
  - 结果：`INTBK-0000`
  - 备份点：`20260520_190020`
  - 报告：`D:\IntRuoyi-BackupOps\logs\202605\20260520_211007_rehearsal_success.report.md`
- PASS：`Register-BackupOpsScheduledTasks.ps1 -PlanOnly`
  - 输出 daily backup 与 weekly rehearsal 两个计划任务定义
- PASS：故障演练闭环
  - `20260520_193708` 在失败 run 后被标记为 `pending-review`
  - 手工恢复为 `verified/success` 后重新进入恢复候选列表
- PASS：通知模块 webhook / disabled / pending 契约测试
  - 结果：`4 passed`
- PASS：通知流程覆盖测试
  - 覆盖失败/blocked 路径通知、`restore-data` 开始通知、`backup-scheduled` 清理通知
  - 结果：`3 passed`

## 放行决定

- 结论：`final pass`
- 依据：
  - 代码与测试验证已完成
  - 真实 `backup-now / backup-scheduled / rollback-app / restore-data / rehearsal` 已完成验证
  - 用户已明确批准 webhook 不再作为最终放行前置

## 已知风险

- 当前 runtime 仍未提供真实 `notify.webhook.url`
- 在 webhook 补齐前，IT 需要人工查看日志、任务计划结果和演练报告
