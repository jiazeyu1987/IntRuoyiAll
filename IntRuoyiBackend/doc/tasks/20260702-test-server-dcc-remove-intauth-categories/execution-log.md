# 执行日志：测试服务器清理 INTAUTH 文控权限类别编码

## 2026-07-02

- BDD: 清理测试服 INTAUTH 文控类别 -> Given 测试服务器文控权限类别中存在 code LIKE 'INTAUTH%' 的历史类别 / When 执行授权的数据清理 / Then 活动类别中不再存在 INTAUTH 起始编码，DCC_ 起始类别仍保留。
- BDD: 关联规则不悬挂 -> Given 被删除类别存在权限、分发、培训或目录绑定等关联数据 / When 清理类别 / Then 关联规则随同清理或软删除，回查不留下指向已清理 INTAUTH 类别的活动规则。
- GREEN: experience-preflight -> PASS，已读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`docs/server-access.md`、`docs/login-access.md`、`database-schema-delivery` 与 `task-closeout-cleanup`；用户当前任务明确要求操作测试服务器。
- RED: remote-select-intauth -> FAIL，测试服 `dcc_file_category` 仍存在活动 `code LIKE 'INTAUTH%'` 类别 48 条；活动 `DCC_%` 类别 120 条。
- GREEN: relation-precheck -> PASS，删除前活动关联数据为：权限规则 186 条、分发规则 1 条、培训规则 1 条、目录绑定 0 条、审批路线 50 条、查看矩阵 0 条、受控文件 0 条、受控文件主数据 0 条、上传策略 0 条。
- GREEN: recovery-export -> PASS，已导出删除前 `INTAUTH%` 类别与直接关联记录到 `pre-delete-intauth-export.tsv`。
- GREEN: remote-delete-intauth -> PASS，事务内软删除活动 `INTAUTH%` 类别 48 条，并同步软删除权限规则 186 条、分发规则 1 条、培训规则 1 条、审批路线 50 条；断言 `assert_ok=1`。
- GREEN: remote-readback -> PASS，活动 `INTAUTH%` 类别 0 条，已删除 `INTAUTH%` 类别 48 条，活动 `DCC_%` 类别 120 条。
- GREEN: relation-readback -> PASS，活动权限规则、分发规则、培训规则、目录绑定、审批路线、查看矩阵、受控文件、受控文件主数据、上传策略均不再引用 `INTAUTH%` 类别。
- GREEN: database-evidence-validation -> PASS，`validate_database_schema.py --evidence doc/tasks/20260702-test-server-dcc-remove-intauth-categories/database-schema-evidence.md` 通过。
- GREEN: task-closeout-cleanup-preview -> PASS，已运行 `task_closeout.py --task-id 20260702-test-server-dcc-remove-intauth-categories --mode preview`；恢复证据和数据库证据列入保留范围。
