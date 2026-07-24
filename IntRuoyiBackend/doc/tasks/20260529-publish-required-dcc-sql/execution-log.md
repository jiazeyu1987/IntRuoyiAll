# 执行日志：发布脚本强制携带并执行 DCC 必需 SQL

## BDD

- BDD: 代码发布补齐 DCC 数据库前置条件 -> Given 目标环境数据库缺少 DCC ACL 快照表或“其他”模板类别 / When 使用发布脚本进行代码发布 / Then 远端 MySQL 启动后、后端启动前执行必需 SQL并补齐前置条件。
- BDD: 带数据发布在导入后补齐 DCC 前置条件 -> Given 发布包包含数据库 dump 但 dump 缺少新表或种子数据 / When 发布脚本重置并导入数据库 / Then 必需 SQL 在导入完成后再次执行，避免导入覆盖前置修复。
- BDD: 测试服和正式服使用同一发布包 -> Given 先 build-release 上传 NAS 发布包 / When 后续 deploy-release 发布到测试服或正式服 / Then 发布包中包含同一份必需 SQL，旧包缺少时直接失败。
- BDD: 缺少必需 SQL 快速失败 -> Given 本地仓库或 NAS 发布包缺少任一必需 SQL / When 执行构建或发布 / Then 脚本报出缺失文件路径并停止，不静默跳过。

## 记录

- 2026-05-29：创建任务文档，开始补充发布脚本回归测试。
- RED: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> FAIL, expected reason: 当前发布脚本缺少 `$requiredDatabaseSqlScripts`、必需 SQL 打包、远端执行与旧包校验逻辑。
- 2026-05-29：修改 `script/deploy/publish-int-ruoyi.ps1`，新增固定 DCC 必需 SQL 清单、发布包复制、NAS 包校验、远端复制、远端执行和临时文件清理。
- GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, `19 passed`。
- GREEN: PowerShell parser check for `script\deploy\publish-int-ruoyi.ps1` -> PASS, `PowerShell parser OK`。
- GREEN: `python -X utf8 -m pytest script\tests\test_dcc_nas_acl_snapshot_restore_sql.py -q` -> PASS, `2 passed`。
- GREEN: `node script\tests\dcc-other-template-sql.test.mjs` -> PASS, `pass 1`。
- GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_dcc_nas_acl_snapshot_restore_sql.py -q` -> PASS, `21 passed`。
- GREEN: `python C:\Users\BJB110\.codex\skills\ci-cd-environment-delivery\scripts\validate_cicd_environment.py --evidence doc\tasks\20260529-publish-required-dcc-sql\ci-cd-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260529-publish-required-dcc-sql\database-schema-evidence.md` -> PASS。
- GREEN: `git diff --check -- script\deploy\publish-int-ruoyi.ps1 script\tests\test_publish_int_ruoyi_to_test_tooling.py doc\tasks\20260529-publish-required-dcc-sql` -> PASS，只有 Git CRLF 工作区提示。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260529-publish-required-dcc-sql --mode preview` -> PASS，预览仅清理任务内证据明细文件。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260529-publish-required-dcc-sql --mode apply` -> PASS，保留 `task.md` 与 `execution-log.md`。
