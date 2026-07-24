# 执行日志：NAS 本地文件夹导入 DCC 后端与数据库

BDD: 本地文件夹导入任务创建 -> Given 用户提交 `templateCategoryId`、`effectiveDate`、`rootDirectoryName`、`relativePaths[]` 和 `files[]` / When 后端创建导入任务 / Then 必须创建 `sourceType=LOCAL_FOLDER` 的任务，按相对路径写入目录项和文件项，并为文件项保存 `sourceFileId`。

BDD: 本地导入执行不访问 NAS -> Given 一个 `LOCAL_FOLDER` 任务处于等待执行 / When 后端处理任务 / Then 不得调用 `NasBrowserService.listFiles/readFile/readDirectoryAcl`，不得调用 NAS ACL 快照采集，必须直接使用 `sourceFileId` 提交受控文件。

BDD: 本地路径校验 fail fast -> Given 相对路径为空、包含 `..`、绝对路径、反斜杠逃逸、文件数量与路径数量不一致或没有文件 / When 调用本地导入接口 / Then 必须拒绝创建任务，不写入部分任务项。

BDD: 既有 NAS 转移保持不变 -> Given 用户仍调用 `/dcc/controlled-files/nas-transfer` / When 后端创建并执行任务 / Then 任务来源为 `NAS`，继续执行 NAS 目录列举、文件读取和 ACL 快照。

RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest,DccControlledFileLocalFolderImportControllerTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`DccControlledFileLocalFolderImportReqVO` 不存在，`/dcc/controlled-files/local-folder-import` endpoint 不存在，`20260613_dcc_nas_local_folder_import.sql` 迁移不存在。

GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest,DccControlledFileLocalFolderImportControllerTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，32 个 DCC NAS 转移、本地导入 controller 契约和 schema 契约测试通过。

GREEN: 本地导入执行路径单元验证 -> PASS，`LOCAL_FOLDER` 任务执行不调用 `NasBrowserService.listFiles/readFile/readDirectoryAcl`，不调用 NAS ACL snapshot capture/complete，文件项直接用 `sourceFileId` 提交受控文件。

BLOCKED: Playwright 真实路径 `http://localhost:8081/system/nas` -> FAIL，本地前端可打开但登录页请求 `http://localhost:48081/admin-api/system/tenant/get-by-website?website=localhost:8081` 返回 `net::ERR_CONNECTION_REFUSED`，无法登录测试租户并完成真实导入。

BLOCKED: `powershell -ExecutionPolicy Bypass -File .\script\deploy\restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main` -> FAIL，后端构建被非本任务 `yudao-module-showroom` 编译错误阻塞：`ShowroomPersistentContentService.java:[1079,24] 找不到符号 方法 withDefaultItemLayoutIfMissing(List<ShowroomHallItemMapping>)`。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence ruoyi-vue-pro/doc/tasks/20260613-nas-local-folder-import/backend-api-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence ruoyi-vue-pro/doc/tasks/20260613-nas-local-folder-import/database-schema-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260613-nas-local-folder-import --mode preview` -> PASS，预览无删除项、无阻塞。
