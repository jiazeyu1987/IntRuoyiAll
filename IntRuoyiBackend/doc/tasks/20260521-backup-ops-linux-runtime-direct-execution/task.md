# 任务：让 Linux 测试服务器本机可直接执行 backup-ops

## 目标

在保持现有 `backup-ops` 行为契约基本不变的前提下，新增一条 **Linux 测试服务器本机可直接执行** 的运行路径，不再要求远端安装 `PowerShell / pwsh`。

## 范围

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_*.py`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-backup-ops-linux-runtime-direct-execution\**`

## 非范围

- 不修改正式服务器部署方式
- 不接入真实 webhook
- 不在本任务中处理与 backup-ops 无关的其他模块改动

## 上一任务检查

- 上一任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-test-server-backup-ops-deploy-verify\task.md`
- 状态：`completed`
- 说明：上一任务已验证“Windows 运维机执行、目标指向测试服务器”成功，并确认“Linux 测试服务器本机直接执行 PowerShell 脚本”失败，根因是远端缺少 `pwsh`。

## 里程碑

- [x] M1：创建任务文档并确认改造目标。
- [x] M2：评估 Linux 直接执行方案并确定实现路径。
- [x] M3：实现 Linux 本机执行入口。
- [x] M4：完成测试服务器本机真实执行验证。
- [x] M5：记录结果并收尾。

## 预期验证

- 至少一条 Linux 本机执行命令能够直接触发 `backup-now`
- 至少一条 Linux 本机执行命令能够直接触发 `restore-data` 或等效恢复验证
- 保留现有 Windows 运维机执行模式可用
- 补充相应 BDD/TDD 证据与回归测试

## 当前状态

Completed.

## 当前进展

- 已确定实现路径：
  - 不重写现有 PowerShell 主链路
  - 新增一条 `python3 + bash + docker + curl` 的 Linux 本机入口
  - 先覆盖 `backup-now / backup-scheduled / restore-data`
- 已新增 Linux 本机入口文件：
  - `script/backup-ops/linux/backup_ops_linux.py`
  - `script/backup-ops/linux/backup-ops-linux.sh`
- 已新增 Linux 本机示例配置：
  - `script/backup-ops/config/backup-ops.linux-local.example.json`
- 已在测试服务器 `172.30.30.58` 真实验证：
  - Linux 本机 `backup-now` 成功，备份点 `20260521_104400`
  - Linux 本机 `restore-data` 成功，恢复点 `20260521_104400`

## 最终验证结果

- PASS：`python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_linux_runtime_tooling.py -q`
  - 结果：`3 passed`
- PASS：`python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_real_integration_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_rehearsal_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_manifest_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_scheduling_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_notification_flow_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_linux_runtime_tooling.py -q`
  - 结果：`42 passed`
- PASS：测试服务器本机 `python3 ./linux/backup_ops_linux.py --mode backup-now --config ./backup-ops.linux-local.runtime.json`
  - 备份点：`20260521_104400`
  - 日志：`/opt/intruoyi/ops/backup/logs/202605/20260521_104400_backup-now_success.log`
- PASS：测试服务器本机 `python3 ./linux/backup_ops_linux.py --mode restore-data --config ./backup-ops.linux-local.runtime.json --selected-backup-id 20260521_104400`
  - 恢复点：`20260521_104400`
  - 日志：`/opt/intruoyi/ops/backup/logs/202605/20260521_104508_restore-data_success.log`

## 结论

- 当前系统已具备两条可用执行路径：
  - Windows 运维机执行 PowerShell 主链路
  - Linux 测试服务器本机执行 Python 直执行链路
- “测试服务器不是 Windows 系统，可以不用 PowerShell 吗” 这个问题，现在答案是：**可以**。
