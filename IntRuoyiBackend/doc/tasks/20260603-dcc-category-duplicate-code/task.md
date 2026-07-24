# 任务：修复 DCC 文件分类编码重复插入错误

## 任务目标

定位并修复 DCC 文件分类写入时触发 `dcc_file_category.uk_dcc_file_category_code` 唯一键冲突的问题，确保重复分类编码在服务层以明确、可维护的业务逻辑处理，不暴露原始数据库唯一键异常。

## Previous Task Check

- 上一个后端任务：`doc/tasks/20260603-showroom-client-downloads/task.md`
- 状态：`completed`
- 当前后端仓库已有 unrelated dirty changes；本任务只触碰 DCC 分类修复、对应测试与本任务文档。

## BDD 场景

- BDD: 重复分类编码不触发数据库唯一键异常 -> Given 已存在 DCC 文件分类编码 / When 再次执行会写入相同编码的分类流程 / Then 服务层按既有业务规则处理重复编码，不触发 `SQLIntegrityConstraintViolationException`。
- BDD: 本地后端重启前应用 DCC code 唯一键迁移 -> Given 本机运行库仍保留旧 `uk_dcc_file_category_code(code)` / When 启动本机后端 / Then 重启脚本必须先应用既有 `20260530_dcc_tenant_scoped_code_indexes.sql`，确认 DCC 分类和审批岗位 code 唯一键均为租户维度。
- BDD: 本地后端重启读取持久加密配置 -> Given DCC 下载加密变量已写入 Windows User 或 Machine 环境 / When 从当前 shell 触发本机后端重启 / Then 脚本应导入这些显式持久配置；若三层环境均缺失才 fail-fast。

## Milestones

- [x] M1：建立任务文档并确认上一任务已完成。
- [x] M2：定位重复编码生成与插入路径，确认根因。
- [x] M3：新增 RED 回归测试复现本地重启脚本未应用必要 schema/配置的缺口。
- [x] M4：实现最小根因修复，不引入 fallback、降级或吞异常。
- [x] M5：运行目标测试与相关回归，记录 GREEN 证据。
- [x] M6：运行任务收尾清理预览。

## Expected Verification

- RED：目标 DCC 分类测试先失败，能复现重复编码导致的错误路径。
- GREEN：同一目标测试通过。
- GREEN：相关 DCC 分类回归测试通过。
- GREEN：bug regression evidence validator 通过。
- GREEN：task-closeout-cleanup 预览通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；先定位重复编码来源，再在拥有该行为的服务边界修复。
- `是否存在临时补丁或绕过`：否。

## 根因定位

- 当前源码中旧 `NASCAT-*` 分类生成与 `categoryMapper.insert(created)` 路径已在 `3e32eedca1` 移除，目标测试确认当前源码会使用用户选择的文件类别，不再插入 NAS 目录名分类。
- 本机 48081 后端仍运行 `E:\Int\CacheData\IntRuoyi\runtime\backend-20260603-112956.jar`，该包早于 13:14 的修复提交。
- 本机运行库仍保留旧唯一键 `dcc_file_category.uk_dcc_file_category_code(code)` 与 `dcc_approval_position.uk_dcc_approval_position_code(code)`。
- 报错中的 `NASCAT-5F0BD34779C93F398B82B1AF3F7F43CB5284E7FB` 在本机运行库存在，`tenant_id=1`、`source=NAS_TRANSFER`、`deleted=1`，旧代码再次插入同 code 会撞唯一键。

## 当前状态

completed

## 已完成工作

- 已确认上一后端任务 `20260603-showroom-client-downloads` 状态为 `completed`。
- 已确认当前源码层面的 NAS 分类插入路径已移除，且目标测试通过。
- 已修改 `script/deploy/restart-int-ruoyi-local.ps1`：本机后端启动前会用 SQL probe 检查并应用 `20260530_dcc_tenant_scoped_code_indexes.sql`。
- 已修改本机重启脚本：DCC 下载加密配置从 Process/User/Machine 三层显式环境读取，三层均缺失时仍 fail-fast。
- 已执行本机 backend 重启，启动当前源码打包后的 `backend-runtime-control-20260603-172529.jar`。
- 已确认本机 MySQL 中 `dcc_file_category` 与 `dcc_approval_position` 唯一键均迁移为 `(tenant_id, code)`。

## 验证结果

- RED：`python -m pytest script/tests/test_restart_int_ruoyi_local_schema.py -q` -> FAIL，缺少 `20260530_dcc_tenant_scoped_code_indexes.sql` 本地启动迁移检查。
- GREEN：`python -m pytest script/tests/test_restart_int_ruoyi_local_schema.py -q` -> PASS，2 passed。
- GREEN：PowerShell parser check for `script/deploy/restart-int-ruoyi-local.ps1` -> PASS。
- GREEN：`powershell -NoProfile -ExecutionPolicy Bypass -File script\tests\test_dcc_download_encryption_runtime_config.ps1` -> PASS。
- GREEN：`mvn -pl yudao-module-dcc "-Dtest=DccControlledFileNasTransferServiceTest#processWaitingTasks_usesSelectedCategoryInsteadOfCreatingDirectoryCategory" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，1 test。
- GREEN：`mvn -pl yudao-module-dcc "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldScopeDccCodeUniquenessByTenant" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，1 test。
- GREEN：`powershell -NoProfile -ExecutionPolicy Bypass -File script\deploy\restart-int-ruoyi-local.ps1 -Component backend` -> PASS。
- GREEN：`GET http://127.0.0.1:48081/actuator/health` -> `{"status":"UP"}`。
- GREEN：本机 MySQL `dcc_file_category.uk_dcc_file_category_tenant_code(tenant_id,code)`、`dcc_approval_position.uk_dcc_approval_position_tenant_code(tenant_id,code)` 已存在。
- GREEN：bug regression evidence validator -> PASS。
- CLOSEOUT PREVIEW：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-dcc-category-duplicate-code --mode preview` -> PASS，blocked `<none>`、warnings `<none>`。

## 剩余阻塞

- 无。

## Cleanup Keep

- `doc/tasks/20260603-dcc-category-duplicate-code/bug-regression-evidence.md`
