# 20260525 NAS 根目录备份存储执行日志

## BDD

- BDD: 立即备份写入 NAS 根目录备份文件夹 -> Given 测试服务器通过已挂载 NAS 路径访问 `/mnt/nas`，When 执行备份同步，Then 备份点路径为 `/mnt/nas/备份/<yyyyMMdd-HHmmss>/`，并保留 `mysql`、`objects`、`deploy`、`manifest` 子目录。
- BDD: 恢复链路只扫描 NAS 根目录备份文件夹 -> Given 已存在备份点，When 执行恢复、回滚或演练候选扫描，Then 只从 `/mnt/nas/备份` 读取备份点。
- BDD: 保留策略只清理新命名格式备份点 -> Given 备份根目录同时存在新格式、旧格式和非备份目录，When 执行保留清理，Then 只处理符合 `yyyyMMdd-HHmmss` 的过期备份点目录。

## TDD Evidence

- RED: `python -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_tooling.py script/tests/test_backup_ops_real_integration_tooling.py` -> FAIL, 当前配置仍为 `/mnt/nas/int-ruoyi/backups`，备份 ID 仍为 `yyyyMMdd_HHmmss`，本地清理仍匹配旧下划线目录，远端清理未限制 `yyyyMMdd-HHmmss` 目录。
- GREEN: `python -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_tooling.py script/tests/test_backup_ops_real_integration_tooling.py` -> PASS, 37 passed。

## 状态

- 2026-05-25：任务文档已创建，准备更新测试断言进入 RED。
- 2026-05-25：配置、脚本和测试已完成；备份点目标路径为 `/mnt/nas/备份/<yyyyMMdd-HHmmss>/`。
- 2026-05-25：task-closeout-cleanup 预览通过，delete/blocked/warnings 均为空；任务完成，准备提交。
