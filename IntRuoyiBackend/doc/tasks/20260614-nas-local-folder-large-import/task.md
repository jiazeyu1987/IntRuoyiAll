# 20260614 NAS 本地大文件夹分批导入 DCC 后端与数据库实施

## 任务目标

为 DCC 本地文件夹导入新增大目录分批上传合同：先创建 `LOCAL_FOLDER` 上传会话任务，再分批上传文件并追加任务项，最后显式完成上传并触发后台转移。目标支持 `E:\Downloads\2.DHF` 这类约 79GB、1.5 万文件目录，不再依赖单次 multipart 请求承载整个目录。

## 前置任务检查

- 最近相关后端任务 `20260613-nas-local-folder-import/task.md` 状态为 `BLOCKED_E2E`，阻塞原因是旧一次性导入无法验证大目录真实路径。
- 本任务是对该阻塞的正式升级：保留小目录旧接口兼容，但大目录通过明确的新接口完成，不扩大全局请求体上限作为临时绕过。
- 本任务限定在 `yudao-module-dcc`、DCC schema、SQL 迁移、测试和任务证据文件。

## 里程碑

1. M1 文档与审计：确认现有 task/item 模型、状态机、schema、API 和测试。
2. M2 RED：新增后端 service/controller/schema 测试，要求 `UPLOADING` 任务、批次追加、完成触发和失败校验。
3. M3 GREEN：实现 create-session、upload-batch、complete 三段合同和必要 schema 字段。
4. M4 REGRESSION：运行 DCC NAS 转移目标 Maven 测试和证据校验。
5. M5 收尾：记录验证证据，运行 task-closeout-cleanup 预览；验证通过后按项目策略提交本任务改动。

## 预期验证

- `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest,DccControlledFileLocalFolderImportControllerTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence ruoyi-vue-pro/doc/tasks/20260614-nas-local-folder-large-import/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence ruoyi-vue-pro/doc/tasks/20260614-nas-local-folder-large-import/database-schema-evidence.md`
- `node script/tests/test_dcc_nas_local_folder_large_import_sql.mjs`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；上传会话未完成不得被后台处理，批次失败不得创建完成任务，不回退到 NAS 读取或旧一次性导入。
- `是否从根因和长期维护角度解决`：是；新增明确任务状态与上传进度字段，按批次保存源文件并复用既有后台 DCC 转移流程。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：COMPLETED。
- 已完成：M1-M4 完成；已新增本地大文件夹导入 `session + batch + complete` 后端合同、`UPLOADING` 状态、进度字段和幂等迁移脚本。
- 验证结果：`mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest,DccControlledFileLocalFolderImportControllerTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过；`node script/tests/test_dcc_nas_local_folder_large_import_sql.mjs` 通过；backend/database evidence validator 通过。
- 补充说明：未执行服务器发布或远程联调；本任务按当前用户请求完成本机代码支持。

## Cleanup Keep

- `doc/tasks/20260614-nas-local-folder-large-import/backend-api-evidence.md`
- `doc/tasks/20260614-nas-local-folder-large-import/database-schema-evidence.md`
