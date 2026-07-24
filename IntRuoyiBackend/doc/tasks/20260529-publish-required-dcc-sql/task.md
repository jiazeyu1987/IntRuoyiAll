# 任务：发布脚本强制携带并执行 DCC 必需 SQL

## 任务目标

- 优化统一发布脚本，防止测试服/正式服发布后再次出现 DCC 必需表缺失或模板类别缺失。
- 发布包必须包含已验证的 DCC 必需 SQL，测试服验证包和正式服上线包使用同一份 SQL 内容。
- 发布时无论是否同步本地数据库，都必须在后端启动前执行必需 SQL；缺少 SQL 文件或旧发布包缺少必需 SQL 时必须失败。

## BDD 场景

- BDD: 代码发布补齐 DCC 数据库前置条件 -> Given 目标环境数据库缺少 DCC ACL 快照表或“其他”模板类别 / When 使用发布脚本进行代码发布 / Then 远端 MySQL 启动后、后端启动前执行必需 SQL 并补齐前置条件。
- BDD: 带数据发布在导入后补齐 DCC 前置条件 -> Given 发布包包含数据库 dump 但 dump 缺少新表或种子数据 / When 发布脚本重置并导入数据库 / Then 必需 SQL 在导入完成后再次执行，避免导入覆盖前置修复。
- BDD: 测试服和正式服使用同一发布包 -> Given 先 build-release 上传 NAS 发布包 / When 后续 deploy-release 发布到测试服或正式服 / Then 发布包中包含同一份必需 SQL，旧包缺少时直接失败。
- BDD: 缺少必需 SQL 快速失败 -> Given 本地仓库或 NAS 发布包缺少任一必需 SQL / When 执行构建或发布 / Then 脚本报出缺失文件路径并停止，不静默跳过。

## 里程碑

- [x] M1：补充发布脚本测试，先复现当前脚本不会强制携带/执行必需 SQL。
- [x] M2：修改发布脚本，固定必需 SQL 清单、打包、校验、远端复制、执行与清理。
- [x] M3：运行脚本测试、SQL 校验、PowerShell 语法校验和证据校验。
- [x] M4：更新任务文档和执行日志，收尾预览并提交本任务改动。

## 预期验证

- RED: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py -q` 在新增校验处失败。
- GREEN: `python -X utf8 -m pytest script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_dcc_nas_acl_snapshot_restore_sql.py -q` 通过。
- GREEN: `node script\tests\dcc-other-template-sql.test.mjs` 通过。
- GREEN: PowerShell 解析 `script\deploy\publish-int-ruoyi.ps1` 无语法错误。
- GREEN: CI/CD 与数据库证据文件通过对应 validator。

## 当前状态

completed

status: completed

## Current Status

completed

## 完成记录

- 发布脚本新增固定 DCC 必需 SQL 清单，只执行经过确认的两份 SQL，不扫描执行整个 `sql/mysql` 目录。
- `direct` 与 `build-release` 会把必需 SQL 复制到发布包 `required-sql/`。
- `deploy-release` 会校验 NAS 发布包是否包含 `required-sql/`；旧包缺失时直接失败。
- 远端 MySQL ready 后、后端启动前执行必需 SQL；带数据库同步时在 dump 导入后执行，避免导入覆盖修复。
- 验证通过：发布脚本测试、ACL SQL 测试、“其他”类别 SQL 测试、PowerShell 解析、证据 validator。
