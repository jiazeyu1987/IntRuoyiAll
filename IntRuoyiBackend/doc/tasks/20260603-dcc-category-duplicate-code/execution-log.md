# Execution Log

BDD: 重复分类编码不触发数据库唯一键异常 -> Given 已存在 DCC 文件分类编码 / When 再次执行会写入相同编码的分类流程 / Then 服务层按既有业务规则处理重复编码，不触发 `SQLIntegrityConstraintViolationException`。

BDD: 本地后端重启前应用 DCC code 唯一键迁移 -> Given 本机运行库仍保留旧 `uk_dcc_file_category_code(code)` / When 启动本机后端 / Then 重启脚本必须先应用既有 `20260530_dcc_tenant_scoped_code_indexes.sql`，确认 DCC 分类和审批岗位 code 唯一键均为租户维度。

BDD: 本地后端重启读取持久加密配置 -> Given DCC 下载加密变量已写入 Windows User 或 Machine 环境 / When 从当前 shell 触发本机后端重启 / Then 脚本应导入这些显式持久配置；若三层环境均缺失才 fail-fast。

ROOT_CAUSE: 当前源码已在 `3e32eedca1` 移除旧 `NASCAT-*` 分类生成和 `categoryMapper.insert(created)` 路径；本机 48081 仍运行 11:29 runtime jar，早于该修复提交。

ROOT_CAUSE: Read-only MySQL -> `dcc_file_category` 仍为 `uk_dcc_file_category_code(code)`，报错 code `NASCAT-5F0BD34779C93F398B82B1AF3F7F43CB5284E7FB` 存在于 `tenant_id=1/source=NAS_TRANSFER/deleted=1`。

RED: `python -m pytest script/tests/test_restart_int_ruoyi_local_schema.py -q` -> FAIL，缺少 `20260530_dcc_tenant_scoped_code_indexes.sql` 本地启动迁移检查。

GREEN: `python -m pytest script/tests/test_restart_int_ruoyi_local_schema.py -q` -> PASS，2 passed。

GREEN: PowerShell parser check for `script/deploy/restart-int-ruoyi-local.ps1` -> PASS。

GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File script\tests\test_dcc_download_encryption_runtime_config.ps1` -> PASS。

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileNasTransferServiceTest#processWaitingTasks_usesSelectedCategoryInsteadOfCreatingDirectoryCategory" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，1 test。

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldScopeDccCodeUniquenessByTenant" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，1 test。

BLOCKER: 本机存在仍在运行的 `publish-int-ruoyi.ps1 ... Environment backup ... ServerHost 172.30.30.59` 进程；等待 3 分钟仍未结束。为避免并发打包/发布抢占同一 target/runtime，未执行本机后端重启或运行库 ALTER。

GREEN: `powershell -NoProfile -ExecutionPolicy Bypass -File script\deploy\restart-int-ruoyi-local.ps1 -Component backend` -> PASS，本机 backend 已切换到 `backend-runtime-control-20260603-172529.jar`。

GREEN: `GET http://127.0.0.1:48081/actuator/health` -> PASS，返回 `{"status":"UP"}`。

GREEN: Read-only MySQL -> `dcc_file_category.uk_dcc_file_category_tenant_code(tenant_id,code)` 与 `dcc_approval_position.uk_dcc_approval_position_tenant_code(tenant_id,code)` 已存在，旧全局 code 唯一键不再存在。

GREEN: bug regression evidence validator -> PASS。

GREEN: task-closeout-cleanup preview -> PASS，blocked `<none>`，warnings `<none>`。
