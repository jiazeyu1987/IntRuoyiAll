# Execution Log

## 2026-07-29

- USER: 删除本地服务器“测试租户”的所有排产数据，然后从“芋道源码”导出，导入测试租户，并分析是否一致。
- READONLY: 已读取 database-schema-delivery、database-rules、local-runtime、login-access、PowerShell/编码和任务收尾相关规则。
- READONLY: 本地后端 `48081`、本地 MySQL `23306` 均在监听；后端 local 数据源指向 `127.0.0.1:23306/ruoyi-vue-pro`，凭据已脱敏。
- BDD: 本地测试租户排产数据重置导入 -> Given 本地库存在源租户“芋道源码”和目标租户“测试租户” / When 先备份并删除目标租户排产数据，再从源租户导出全量排产数据包并导入目标租户 / Then 目标租户排产数据与源租户关键表计数和业务键一致，任何缺表、缺租户或导入失败都必须阻塞。
