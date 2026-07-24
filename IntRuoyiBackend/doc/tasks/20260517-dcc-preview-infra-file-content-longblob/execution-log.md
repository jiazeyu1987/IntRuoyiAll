# Execution Log: DCC 预览文件存储容量修复

BDD: DCC upload preview can persist large PDF content in DB file storage -> Given DCC 上传预览会把原始 PDF 通过 `FileService.createFile(...)` 存入默认文件客户端 / When 上传预览文件大小超过 `MEDIUMBLOB` 上限 / Then 运行库中的 `infra_file_content.content` 仍能容纳该内容 / And 预览不再因为数据库字段截断失败。

BDD: New MySQL baseline also supports large preview payloads -> Given 新环境按仓库 MySQL 基线建库 / When DCC 预览上传把大 PDF 存入 `infra_file_content` / Then 基线表结构使用足够大的二进制字段 / And 不会把线上问题重新带回新库。

RED: `mvn -pl yudao-module-infra -Dtest=InfraBaseSchemaTest test` -> FAIL, MySQL baseline still declared `infra_file_content.content` as `mediumblob` and the longblob migration script was missing.

GREEN: `mvn -pl yudao-module-infra -Dtest=InfraBaseSchemaTest test` -> PASS

GREEN: `mvn -pl yudao-module-dcc -Dtest=DccControlledFileUploadApiTest test` -> PASS

GREEN: `python -m pytest script/tests/test_infra_file_sql_scripts.py -q` -> PASS

GREEN: `python tool/verify_tdd_compliance.py --task-dir doc/tasks/20260517-dcc-preview-infra-file-content-longblob --all-changed` -> PASS

GREEN: `mvn -pl yudao-module-infra,yudao-module-dcc -DskipTests compile` -> PASS

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260517-dcc-preview-infra-file-content-longblob --mode preview` -> READY, keep `task.md` / `execution-log.md`, delete none, blocked none.

BLOCKED VERIFICATION: local runtime MySQL verification was not executed because Docker Desktop / local Docker MySQL was unavailable in this environment, so `SHOW COLUMNS FROM infra_file_content LIKE 'content';` remains to be run against the live database after applying the new migration script.
