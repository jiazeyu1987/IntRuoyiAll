# 20260611-dcc-restore-chain-preflight

## 任务目标

实现阶段 3：恢复链 preflight。恢复数据前必须先校验 DCC backup manifest 与对象 inventory 的链完整性，缺 baseline、缺 previous、checksum 错、object-store 对象缺失、schemaVersion 不匹配时直接阻断，不能进入 MySQL 导入或对象恢复阶段后才失败。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。任一恢复链前置条件缺失必须 fail fast，不允许回退到旧整桶对象目录或跳过 DCC 链校验。
- 是否从根因和长期维护角度解决：是。恢复入口统一依赖 manifest 契约和对象仓库引用，不以脚本执行到某一步的错误作为链完整性判断。
- 是否存在临时补丁或绕过：否。本阶段只补恢复前门禁，不访问正式服务器，不修改正式数据。

## BDD 场景

- BDD: 缺 baseline 阻断恢复 -> Given 备份点 DCC manifest 缺少 fullBaseline.restorePointId 或 baseline checksum / When 运维选择该备份点恢复 / Then restore-data 在 MySQL 导入前失败，并输出修复 baseline manifest 的动作建议。
- BDD: 缺 previous 阻断增量恢复 -> Given 增量 DCC manifest 缺少 previousBackupId 或 previousRestorePointId / When 运维选择该备份点恢复 / Then restore-data 在对象回放前失败，不允许跳过前序恢复点。
- BDD: checksum 错阻断恢复 -> Given full baseline 或 incremental segment checksum 非 sha256 / When 恢复 preflight 执行 / Then 返回 checksum 诊断，不继续恢复。
- BDD: object-store 缺失阻断恢复 -> Given object inventory 引用的 repositoryPath 在 object-store 中不存在 / When 恢复 preflight 执行 / Then 返回缺失对象路径，不回退到备份点旧对象目录。
- BDD: schemaVersion 不匹配阻断恢复 -> Given DCC manifest schemaVersion 不是当前支持版本 / When 恢复 preflight 执行 / Then 返回 schemaVersion 不兼容诊断，不进入恢复动作。

## 里程碑

- [x] M1：审查现有恢复入口和 DCC 链校验覆盖范围。
- [x] M2：补 RED 测试覆盖 restore-data 入口的 baseline、previous、checksum、object、schemaVersion 阻断。
- [x] M3：最小实现 GREEN，恢复前统一执行链 preflight。
- [x] M4：更新执行日志、长期备份经验和收尾验证。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_backup_ops_linux_runtime_ports.py -q`
- `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q`
- `git diff --check`
- `task-closeout-cleanup --mode preview`

## 当前状态

completed

## Verification Result

- `python -X utf8 -m pytest script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，40 passed。
- `python -X utf8 -m pytest script/tests/test_backup_ops_tooling.py script/tests/test_backup_ops_linux_runtime_ports.py -q` -> PASS，130 passed。
- 阶段经验已写入 root worktree `docs/release-backup-restore.md`。
