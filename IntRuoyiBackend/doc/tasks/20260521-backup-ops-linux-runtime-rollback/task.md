# 任务：补齐 Linux 本机 backup-ops 的回滚能力

## 目标

在已完成 Linux 本机 `backup-now / restore-data` 直执行的基础上，继续为 `backup_ops_linux.py` 增加 `rollback-app` 支持，使测试服务器本机不依赖 PowerShell 也能执行应用版本回滚。

## 范围

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\linux\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\backup-ops\config\backup-ops.linux-local.example.json`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_linux_runtime_*.py`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-backup-ops-linux-runtime-rollback\**`

## 非范围

- 本任务不补 Linux 本机 `rehearsal`
- 不替换现有 PowerShell 主链路
- 不接入真实 webhook

## 上一任务检查

- 上一任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-backup-ops-linux-runtime-direct-execution\task.md`
- 状态：`completed`
- 说明：上一任务已完成 Linux 本机 `backup-now / restore-data` 真实验证，本任务承接其 Python 入口继续补齐 `rollback-app`

## 里程碑

- [x] M1：创建任务文档并确认扩展目标。
- [ ] M2：补 `rollback-app` 的 TDD 场景与 RED 测试。
- [ ] M3：实现 Linux 本机 `rollback-app`。
- [ ] M4：完成测试服务器本机真实回滚验证。
- [ ] M5：记录结果并收尾。

## 预期验证

- `backup_ops_linux.py` 支持 `rollback-app`
- 至少一条 Linux 本机命令能直接触发 `rollback-app`
- 保留现有 Linux 本机 `backup-now / restore-data` 可用

## 当前状态

Completed.

## 当前进展

- 已为 `backup_ops_linux.py` 增加 `rollback-app`
- Linux 本机 `rollback-app` 会执行：
  - 从备份点 `deploy/image-tag.txt` 收集回滚候选
  - 备份当前 `.env`
  - 更新 `IMAGE_TAG`
  - `docker compose up -d backend frontend`
  - 后端/前端健康检查
- 已在测试服务器 `172.30.30.58` 真实验证：
  - 当前 tag 从 `20260521_092448` 回滚到 `20260520_113715`
  - 回滚后 `backend` 与 `frontend` 均恢复健康

## 最终验证结果

- PASS：`python -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_backup_ops_linux_runtime_rollback_tooling.py -q`
  - 结果：`2 passed`
- PASS：测试服务器本机 `python3 ./linux/backup_ops_linux.py --mode rollback-app --config ./backup-ops.linux-local.runtime.json --selected-image-tag 20260520_113715`
  - 日志：`/opt/intruoyi/ops/backup/logs/202605/20260521_110936_rollback-app_success.log`
- PASS：回滚后测试服务器状态核对
  - `IMAGE_TAG=20260520_113715`
  - `backend` 健康：`UP`
  - `frontend` 响应：`HTTP/1.1 200 OK`
- PASS：全量回归
  - `44 passed`

## 结论

- Linux 本机模式现在已覆盖：
  - `backup-now`
  - `backup-scheduled`
  - `restore-data`
  - `rollback-app`
- 当前尚未覆盖 `rehearsal`，但测试服务器本机的日常备份、恢复、版本回滚已经具备直接执行能力。
