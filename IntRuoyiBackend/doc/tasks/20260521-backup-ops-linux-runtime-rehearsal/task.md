# 任务：补齐 Linux 本机 backup-ops 的恢复演练能力

## 目标

在 Linux 测试服务器本机执行 `backup_ops_linux.py` 时，继续补齐 `rehearsal`，使其不依赖 PowerShell / pwsh，也能在 Linux 上完成独立恢复演练。

## 范围

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\linux\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\config\backup-ops.linux-local.example.json`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_linux_runtime_*.py`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-backup-ops-linux-runtime-rehearsal\**`

## 非范围

- 不修改正式服务器部署方式
- 不接入真实 webhook
- 不在本任务中重写 Windows PowerShell 主链路

## 上一任务检查

- 上一任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-backup-ops-linux-runtime-rollback\task.md`
- 状态：`completed`
- 说明：上一任务已完成 Linux 本机 `rollback-app` 真实验证，本任务继续补齐 `rehearsal`

## 里程碑

- [x] M1：创建任务文档并确认扩展目标。
- [x] M2：补 `rehearsal` 的 TDD 场景与 RED 测试。
- [x] M3：实现 Linux 本机 `rehearsal`。
- [x] M4：完成测试服务器本机真实演练验证。
- [x] M5：记录结果并收尾。

## 预期验证

- `backup_ops_linux.py` 支持 `rehearsal`
- Linux 本机命令可直接触发恢复演练
- 仍保留 `backup-now / restore-data / rollback-app` 可用

## 当前状态

Completed.

## 当前进展

- 已恢复推进本任务，并完成 Linux 本机 `rehearsal`
- Linux 本机 `rehearsal` 现在会执行：
  - 选择恢复点
  - 在 `rehearsalRoot` 生成独立运行槽位
  - 恢复 MySQL / 对象文件
  - 启动独立 backend / frontend
  - 执行 `backend / frontend / login / 文件抽样` 校验
- 已在测试服务器 `172.30.30.58` 真实验证：
  - 演练备份点：`20260521_104400`
  - 演练成功

## 最终验证结果

- PASS：`python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q`
  - 结果：`2 passed`
- PASS：`python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_linux_runtime_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_linux_runtime_rollback_tooling.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q`
  - 结果：`7 passed`
- PASS：测试服务器本机 `python3 ./linux/backup_ops_linux.py --mode rehearsal --config ./backup-ops.linux-local.runtime.json --selected-backup-id 20260521_104400`
  - 备份点：`20260521_104400`
  - 日志：`/opt/intruoyi/ops/backup/logs/202605/20260521_112350_rehearsal_success.log`
  - 报告：`/opt/intruoyi/ops/backup/logs/202605/20260521_112350_rehearsal_success.report.md`

## 结论

- Linux 本机模式现在已覆盖：
  - `backup-now`
  - `backup-scheduled`
  - `restore-data`
  - `rollback-app`
  - `rehearsal`
- 到这里为止，测试服务器本机已经不再依赖 PowerShell，就能完整执行备份、恢复、回滚和演练。
