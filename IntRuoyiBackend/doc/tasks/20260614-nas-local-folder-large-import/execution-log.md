# 执行日志：NAS 本地大文件夹分批导入 DCC 后端与数据库

BDD: 创建本地导入上传会话 -> Given 用户提交模板类别、DCC 产品、生效日期、根目录名、预计文件数和预计总大小 / When 后端创建本地导入会话 / Then 必须创建 `sourceType=LOCAL_FOLDER`、`status=UPLOADING` 的任务，并记录 expected/uploaded 进度，后台调度不得处理该任务。

BDD: 分批上传追加任务项 -> Given 本地导入会话处于 `UPLOADING` / When 前端提交一批 `relativePaths[]` 和 `files[]` / Then 后端必须校验路径归属同一根目录、拒绝重复路径，保存文件到 infra_file，并为目录/文件追加等待项。

BDD: 完成上传触发后台任务 -> Given 已上传文件数等于 expectedFileCount 且没有失败 / When 前端调用完成接口 / Then 后端必须把任务状态切换为 `WAITING` 并触发后台 DCC 导入。

BDD: 分批导入 fail fast -> Given 任务不存在、非本人任务、非 `UPLOADING` 状态、文件数不匹配、路径非法或重复 / When 调用批次上传或完成接口 / Then 必须拒绝请求，不写入部分完成状态，不静默回退到旧一次性接口。

RED: mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest,DccControlledFileLocalFolderImportControllerTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, 缺少 DccControlledFileLocalFolderImportSessionCreateReqVO、session/batch/complete endpoints、进度字段和迁移脚本。

GREEN: mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest,DccControlledFileLocalFolderImportControllerTest,DccBaseSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 新增会话/批次/完成接口、UPLOADING 状态、上传进度字段与幂等迁移脚本均通过后端合同和 schema 验证。

GREEN: python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence ruoyi-vue-pro\doc\tasks\20260614-nas-local-folder-large-import\backend-api-evidence.md -> PASS

GREEN: python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence ruoyi-vue-pro\doc\tasks\20260614-nas-local-folder-large-import\database-schema-evidence.md -> PASS

GREEN: node script/tests/test_dcc_nas_local_folder_large_import_sql.mjs -> PASS, 验证新增迁移脚本、MySQL 基线 schema 与 H2 测试 schema 均包含本地大文件夹上传进度字段，且迁移脚本不包含破坏性 DCC SQL。

INFO: 2026-06-14T22:45:23+08:00 用户追加授权测试服 `172.30.30.58` 直接处理；本次必须通过 NAS 管理真实导入本机目录 `E:\Downloads\1. QMS documents\`，记录并解决服务器端遇到的问题。

BDD: 测试服真实导入 QMS documents -> Given 测试服务器前后端可访问且当前浏览器登录到授权测试账号 / When 用户在测试服 NAS 管理选择 `E:\Downloads\1. QMS documents\` 并确认导入 / Then 后端必须完成 `LOCAL_FOLDER` 导入任务，失败条目为 0，并可通过任务表和 DCC 文件表只读核验。
