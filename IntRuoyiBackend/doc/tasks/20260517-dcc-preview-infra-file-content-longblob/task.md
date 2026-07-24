# Task: DCC 预览文件存储容量修复

- Task ID: `20260517-dcc-preview-infra-file-content-longblob`
- Created: `2026-05-17`
- Status: `completed`

## Goal

修复 DCC 文件上传后点击预览时触发的 `infra_file_content.content` 截断问题，保证受控文件上传预览与后续正式提交流程在大于 16MB 的 PDF 场景下不再因为 DB 文件存储容量不足而失败。

## Previous Task Check

- Previous task directory found: `doc/tasks/20260517-restore-machinery-ledger-data/`
- Previous task status: blocked on `2026-05-17` due to this production preview failure being reprioritized.
- Boundary: this task is limited to `infra_file_content` 容量修复、相关 DCC 预览链路验证、对应 SQL 迁移与任务证据。

## BDD Scenarios

BDD: DCC upload preview can persist large PDF content in DB file storage -> Given DCC 上传预览会把原始 PDF 通过 `FileService.createFile(...)` 存入默认文件客户端 / When 上传预览文件大小超过 `MEDIUMBLOB` 上限 / Then 运行库中的 `infra_file_content.content` 仍能容纳该内容 / And 预览不再因为数据库字段截断失败。

BDD: New MySQL baseline also supports large preview payloads -> Given 新环境按仓库 MySQL 基线建库 / When DCC 预览上传把大 PDF 存入 `infra_file_content` / Then 基线表结构使用足够大的二进制字段 / And 不会把线上问题重新带回新库。

## Milestones

1. [x] 明确真实报错仓库、前置未完成任务状态，并创建当前任务文档。
2. [ ] RED: 添加基线/迁移约束测试，证明 `infra_file_content.content` 仍停留在会截断的结构。
3. [ ] GREEN: 提供运行库迁移 SQL，并同步更新 MySQL 基线表结构。
4. [ ] 运行针对性验证，确认测试通过且任务证据完整。
5. [ ] 更新任务状态并准备本任务提交。

## Expected Verification

- `mvn -pl yudao-module-infra -Dtest=InfraBaseSchemaTest test`
- `mvn -pl yudao-module-dcc -Dtest=DccControlledFileUploadApiTest test`
- 如具备本地 MySQL 运行库入口，执行新增迁移 SQL 后 `SHOW COLUMNS FROM infra_file_content LIKE 'content';` 返回 `longblob`

## Current Status

已确认 DCC 预览上传链路通过 `DBFileClient.upload()` 将整份 PDF 写入 `infra_file_content.content`，当前修复方向为将该字段从 `MEDIUMBLOB` 提升到 `LONGBLOB`，并同时覆盖存量库迁移与新库基线。

## Completed Work

- 确认真实故障仓库与调用链：`DccControlledFileUploadServiceImpl -> FileService.createFile -> DBFileClient.upload -> infra_file_content.content`。
- 新增 `InfraBaseSchemaTest`，要求 MySQL 基线和迁移脚本都把 `infra_file_content.content` 维护为 `LONGBLOB`。
- 新增幂等迁移脚本 `sql/mysql/20260517_infra_file_content_longblob.sql`，用于升级存量库字段类型。
- 同步更新 `sql/mysql/ruoyi-vue-pro.sql` 基线，避免新库继续落成 `MEDIUMBLOB`。

## Final Verification

- `mvn -pl yudao-module-infra -Dtest=InfraBaseSchemaTest test` -> PASS
- `mvn -pl yudao-module-dcc -Dtest=DccControlledFileUploadApiTest test` -> PASS
- `python -m pytest script/tests/test_infra_file_sql_scripts.py -q` -> PASS
- `python tool/verify_tdd_compliance.py --task-dir doc/tasks/20260517-dcc-preview-infra-file-content-longblob --all-changed` -> PASS
- `mvn -pl yudao-module-infra,yudao-module-dcc -DskipTests compile` -> PASS
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260517-dcc-preview-infra-file-content-longblob --mode preview` -> READY
- Runtime DB verification: not run. Local Docker MySQL was unavailable, so `SHOW COLUMNS FROM infra_file_content LIKE 'content';` was not executed in the running database during this task.
