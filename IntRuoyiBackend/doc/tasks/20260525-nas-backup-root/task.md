# 20260525 NAS 根目录备份存储

## 任务目标

将备份工具的测试服务器备份点存储位置从 `/mnt/nas/int-ruoyi/backups` 调整为 NAS 根目录下的 `/mnt/nas/备份`，并将备份点名称格式从 `yyyyMMdd_HHmmss` 统一为 `yyyyMMdd-HHmmss`。

## 里程碑

- [x] M1：确认旧任务状态和当前备份工具配置、脚本、测试覆盖。
- [x] M2：按 BDD/TDD 更新测试断言，证明当前实现不满足 NAS 根目录和新命名格式。
- [x] M3：更新配置、备份点生成、清理识别和相关脚本路径断言。
- [x] M4：运行受影响测试并记录 GREEN 证据。
- [x] M5：执行 task-closeout-cleanup 预览，提交本任务直接产生的改动。

## 预期验证

- `python -m pytest script/tests/test_backup_ops_tooling.py`
- `python -m pytest script/tests/test_backup_ops_linux_runtime_tooling.py`
- `python -m pytest script/tests/test_backup_ops_real_integration_tooling.py`

## 范围与约束

- 保持现有备份点目录结构：`mysql`、`objects`、`deploy`、`manifest`。
- 不新增 DCC SMB 写入链路，不读取或复制 NAS 凭据。
- 不立即执行真实生产备份。
- 若测试服务器不存在 `/mnt/nas/备份` 或无写入权限，真实环境验证必须失败并报告，不做 fallback。

## 当前状态

状态：已完成。

最终验证：`python -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_tooling.py script/tests/test_backup_ops_real_integration_tooling.py` 通过，37 passed。

收尾预览：`task_closeout.py --task-id 20260525-nas-backup-root --mode preview` 通过，无删除项、阻塞项或警告。

旧任务检查：`doc/tasks/20260525-tenant-yudao-to-yingtai-copy` 已记录为阻塞，阻塞原因和影响已在旧任务文档中说明，本任务可以独立开始。
