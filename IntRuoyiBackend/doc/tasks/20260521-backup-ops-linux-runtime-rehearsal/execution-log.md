# 执行日志：补齐 Linux 本机 backup-ops 的恢复演练能力

BDD: linux local rehearsal -> Given 测试服务器是 Linux 且已具备 backup-ops Python 本机入口 When IT 需要在服务器本机执行恢复演练 Then 系统必须不依赖 PowerShell 直接完成独立演练恢复、登录校验和文件抽样验证
RED: python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q -> FAIL, Linux 示例配置还缺独立 rehearsal 端口/验证配置，Python 本机入口也尚未实现 rehearsal
GREEN: python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q -> PASS
GREEN: ssh root@172.30.30.58 "cd /opt/intruoyi/ops/backup-ops/linux-native && python3 ./linux/backup_ops_linux.py --mode rehearsal --config ./backup-ops.linux-local.runtime.json --selected-backup-id 20260521_104400" -> PASS, Linux 本机完成真实恢复演练
GREEN: python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_linux_runtime_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_linux_runtime_rollback_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q -> PASS, 7 passed
BLOCKED: 用户切换到更高优先级发布链需求 -> FAIL, 需先完成“统一发布链同时发布 Website 并回显访问路径”的发布运维调整，本任务暂停在 RED 之后，尚未进入实现阶段
