# Execution Log

## 2026-07-29

- USER: 删除本地服务器“测试租户”的所有排产数据，然后从“芋道源码”导出，导入测试租户，并分析是否一致。
- READONLY: 已读取 database-schema-delivery、database-rules、local-runtime、login-access、PowerShell/编码和任务收尾相关规则。
- READONLY: 本地后端 `48081`、本地 MySQL `23306` 均在监听；后端 local 数据源指向 `127.0.0.1:23306/ruoyi-vue-pro`，凭据已脱敏。
- BDD: 本地测试租户排产数据重置导入 -> Given 本地库存在源租户“芋道源码”和目标租户“测试租户” / When 先备份并删除目标租户排产数据，再从源租户导出全量排产数据包并导入目标租户 / Then 目标租户排产数据与源租户关键表计数和业务键一致，任何缺表、缺租户或导入失败都必须阻塞。
- READONLY: 租户确认：源租户 `芋道源码(id=1)`，目标租户 `测试租户(id=122)`。
- BLOCKER: 源租户 full-config 首次导出失败：`配置包引用缺失，原因：角色【edhr_route_922067_save】缺少分类`；未删除目标排产数据。
- DATA BACKUP: 已备份 5 个缺分类角色到 `doc/tasks/20260729-local-scheduler-tenant-copy/role-category-backup-before-update.json`。
- DATA FIX: 用户授权后精确修复 5 个缺分类角色：`910300 -> batch-record(5)`、`910305 -> dcc(3)`、`910309 -> dcc(3)`、`910313 -> dcc(21)`、`910403 -> menu(19)`；SQL 事务更新 5 行，剩余缺失 0。
- READONLY: 角色修复后源租户可导出旧 full-config，但当前 `48081` 运行 Jar 尚未包含 `manualReplanDataPackage`，需重启本地后端加载当前代码后继续。
