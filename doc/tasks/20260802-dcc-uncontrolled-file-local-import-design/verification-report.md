# Verification Report

## Summary

已完成 DCC 未受控文件本地下载与自动归类的文档设计、BDD 场景、严格 TDD 顺序、真实 E2E 计划、测试数据设计、M7 schema 明细切片、M11 files page API 切片、M13 确定性预识别后端切片、M14 import-selected/local-write 文档门禁优化、M15 import-selected 任务快照 schema 切片、M16 后续实现门禁加固、M17 import-selected 服务契约/legacy processor 隔离切片、M18 服务级 import-selected 原子创建、M19 服务级幂等并发保护、M20 import-selected controller 契约、M21 content binary download、M22 local-write-result、M23 归档元数据缺失显式阻塞、M25 前端静态合同/最小 UI/API 集成、M26 正式归档元数据来源阻塞门禁，以及 M27 文档一致性审计。M21 在隔离 worktree `D:\IntRuoyiWorktree\dcc-uncontrolled-import-m21-verify-20260803` 完成 targeted GREEN 与相邻回归，验证内容包括二进制 `ResponseEntity<byte[]>` controller、content 快照绑定、跨任务/过期签名拒绝和不变更 local-write/archive 状态。M22 在主工作区完成 targeted GREEN 与 M17-M22 相邻回归，验证内容包括 `local-write-result` controller、`@Valid @RequestBody` 快照体、`LOCAL_WRITTEN` 状态回写、重复成功回放幂等和冲突终态拒绝。M23 在主工作区完成 targeted RED/GREEN 与 M17-M23 相邻回归，验证内容包括 `MATCHED + LOCAL_WRITTEN` 缺正式归档元数据时写入 `archiveStatus=FAILED`、`archiveErrorCode=ARCHIVE_METADATA_REQUIRED`，且不触发 NAS 读取、原始文件上传、workflow submit、受控文件创建或 ACTIVE NAS source 写入。设计复用当前 NAS 管理、未受控统计、NAS transfer、DCC 项目代码、文件分类树和分类规则能力；无法唯一识别项目代码、item 或分类时，统一进入正式 `未分类/待处理` 状态，不引入默认归类或降级路径。M17 补齐无旧字段请求 VO、服务入口签名和 `NAS_UNCONTROLLED_IMPORT` waiting processor 跳过逻辑；M18 补齐服务级 import-selected 原子创建、task/item 快照和 audit 绑定；M19 补齐 canonical request hash 复用/冲突、重复 audit id 前置拒绝和事务内二次幂等检查；M20 补齐 `/import-selected` 路由、写权限组合和 `@Valid @RequestBody` 请求体；M21 补齐内容下载快照绑定服务和二进制 controller 实现入口；M22 补齐本地写入结果快照回写入口；M23 补齐缺元数据归档阻塞入口；M24 补齐处理项级正式归档元数据快照、创建受控文件、workflow submit 与 ACTIVE NAS 来源映射成功路径；M25 补齐前端静态合同和最小 UI/API 入口；M26 固化缺少正式归档元数据快照时必须阻塞的门禁；M27 补齐 `tdd-plan.md` 与 `test-data.md` 中关键状态和源类型标记，保证后续实施能直接检索到本地目录写入、未分类待处理和归档元数据阻塞门禁。不代表真实浏览器 E2E 已完成。

## Files Verified

- `doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/task.md`
- `doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/execution-log.md`
- `doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/design.md`
- `docs/acceptance/bdd-scenarios.md`
- `docs/acceptance/tdd-plan.md`
- `docs/acceptance/e2e-plan.md`
- `docs/acceptance/test-data.md`
- `docs/task-closeout-rules.md`
- `docs/e2e-rules.md`
- `docs/experience-index.md`
- `doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/database-schema-evidence.md`
- `doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/backend-api-evidence.md`
- `IntRuoyiBackend/sql/mysql/20260803_dcc_nas_uncontrolled_import_task_snapshot.sql`
- `IntRuoyiBackend/script/tests/test_dcc_nas_uncontrolled_import_task_snapshot_sql.py`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFileNasTransferTaskDO.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFileNasTransferTaskItemDO.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccNasControlAuditFileDO.java`
- `IntRuoyiBackend/yudao-module-dcc/src/test/resources/sql/create_tables.sql`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccNasControlAuditServiceImpl.java`
- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccNasControlAuditServiceImplTest.java`
- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/DccBaseSchemaTest.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccNasUncontrolledImportSelectedReqVO.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/vo/DccNasUncontrolledImportLocalWriteResultReqVO.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccNasControlAuditController.java`
- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccNasControlAuditControllerTest.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccNasUncontrolledImportController.java`
- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccNasUncontrolledImportControllerTest.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileNasTransferService.java`
- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileNasTransferServiceImpl.java`
- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileNasTransferServiceTest.java`

## Verification Evidence

- `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS，`BDD/TDD acceptance plan validation passed.`
- `node -e "<doc consistency marker scan>"` -> PASS，8 个任务/验收核心文档均包含 `ARCHIVE_METADATA_REQUIRED`、`未分类/待处理`、`showDirectoryPicker`、`LOCAL_WRITTEN` 和 `NAS_UNCONTROLLED_IMPORT`。
- `node -e "<utf8/trailing whitespace check>"` -> PASS，`UTF8_TRAILING_CHECK_PASS files=5`。
- `git diff --check -- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/task.md doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/execution-log.md doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/verification-report.md docs/acceptance/tdd-plan.md docs/acceptance/test-data.md` -> PASS，仅存在 Git 行尾转换 warning，无 whitespace error。
- `project-experience-consolidation` -> PASS，已将验收文档关键标记扫描门禁合并到 `docs/e2e-rules.md#规划型 E2E 前置与业务 RED 分离门禁`，并在 `docs/experience-index.md` 增加关键词路由。
- `rg -n "doc consistency marker scan|关键业务标记扫描|ARCHIVE_METADATA_REQUIRED" docs\experience-index.md docs\e2e-rules.md` -> PASS，长期经验索引和目标门禁均可检索。
- `node -e "<utf8 read check>"` -> PASS，任务文档、验收文档和经验文档均可按 UTF-8 读取，`contains_replacement=false`。
- `git -C E:\IntRuoyi diff --check -- docs/acceptance/bdd-scenarios.md docs/acceptance/tdd-plan.md docs/acceptance/e2e-plan.md docs/acceptance/test-data.md docs/task-closeout-rules.md docs/experience-index.md doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/task.md doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/execution-log.md doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/design.md doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/verification-report.md` -> PASS，仅存在 Git 行尾转换 warning，无 whitespace error。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260802-dcc-uncontrolled-file-local-import-design --mode preview` -> PASS，delete/blocked/warnings 均为 none。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260802-dcc-uncontrolled-file-local-import-design --mode apply` -> PASS，deleted_paths 为 none。
- `project-experience-consolidation` -> PASS，已把 cleanup apply 状态格式经验合并到现有 `docs/task-closeout-rules.md` 和 `docs/experience-index.md`。
- `task_closeout.py --task-id 20260802-dcc-uncontrolled-file-local-import-design --mode preview/apply` -> PASS，本轮优化后 cleanup apply 无删除项、无 blocked、无 warnings。
- `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS，本轮二次优化后 `BDD/TDD acceptance plan validation passed.`。
- `node -e "<utf8 read check>"` -> PASS，本轮二次优化触达 5 个文档均可按 UTF-8 读取，`contains_replacement=false`。
- `git -C E:\IntRuoyi diff --check -- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/design.md docs/acceptance/bdd-scenarios.md docs/acceptance/tdd-plan.md docs/acceptance/e2e-plan.md docs/acceptance/test-data.md` -> PASS，仅存在 Git 行尾转换 warning，无 whitespace error。
- `project-experience-consolidation` -> PASS，浏览器本地目录写入门禁已合并到 `docs/e2e-rules.md`，关键词索引已写入 `docs/experience-index.md`。
- `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS，长期经验合并后 `BDD/TDD acceptance plan validation passed.`。
- `node -e "<utf8 read check>"` -> PASS，10 个任务、验收和经验文档均可按 UTF-8 读取，`contains_replacement=false`。
- `git -C E:\IntRuoyi diff --check -- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/task.md doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/execution-log.md doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/design.md doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/verification-report.md docs/acceptance/bdd-scenarios.md docs/acceptance/tdd-plan.md docs/acceptance/e2e-plan.md docs/acceptance/test-data.md docs/e2e-rules.md docs/experience-index.md` -> PASS，仅存在 Git 行尾转换 warning，无 whitespace error。
- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_nas_control_audit_file_sql.py -q` -> PASS，2 passed in 3.18s。
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileDetails" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS，本轮文档补强后 `BDD/TDD acceptance plan validation passed.`。
- `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260802-dcc-uncontrolled-file-local-import-design\database-schema-evidence.md` -> PASS，`Database schema evidence is valid.`。
- `node -e "<utf8 read check>"` -> PASS，本任务 9 个任务、验收和 schema 证据文档均可按 UTF-8 读取，`contains_replacement=false`。
- `git diff --check -- <本任务文档与schema切片文件>` -> PASS，仅存在 Git 行尾转换 warning，无 whitespace error。

- python -X utf8 C:/Users/BJB110/.codex/skills/bdd-tdd-acceptance-planner/scripts/validate_acceptance_plan.py --root E:/IntRuoyi -> PASS, M10 doc strengthening after user request.
- python -X utf8 -c utf8_read_check -> PASS, 8 task/acceptance docs contain no replacement characters.
- git diff --check -- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/design.md docs/acceptance/bdd-scenarios.md docs/acceptance/tdd-plan.md docs/acceptance/e2e-plan.md docs/acceptance/test-data.md -> PASS, only Git LF-to-CRLF warnings.
- `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS，M12 文档优化后 `BDD/TDD acceptance plan validation passed.`
- PowerShell UTF-8 read check for `design.md`, `bdd-scenarios.md`, `tdd-plan.md`, `e2e-plan.md`, and `test-data.md` -> PASS，全部 `contains_replacement=False`。
- `git diff --check -- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/design.md docs/acceptance/bdd-scenarios.md docs/acceptance/tdd-plan.md docs/acceptance/e2e-plan.md docs/acceptance/test-data.md` -> PASS，仅存在 Git LF-to-CRLF warning。
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_marksProjectAndCategoryWhenUnique" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_marksProjectAndCategoryWhenUnique,DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_marksPendingWhenProjectOrCategoryMissing,DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_marksAmbiguousWhenProjectOrCategoryHasMultipleCandidates,DccNasControlAuditServiceImplTest#recognizeUncontrolledFileDetails_doesNotRewriteImportedOrArchivedSnapshots" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 4, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileDetails,DccBaseSchemaTest#mysqlSchemaShouldSupportDccNasControlAuditFileRecognitionSnapshot,DccNasControlAuditControllerTest,DccNasControlAuditServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 9, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_nas_control_audit_file_sql.py -q` -> PASS，2 passed in 0.17s。
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportNasUncontrolledImportTaskSnapshots" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_dcc_nas_uncontrolled_import_task_snapshot_sql.py IntRuoyiBackend/script/tests/test_dcc_nas_control_audit_file_sql.py -q` -> PASS，4 passed in 1.49s。
- `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260802-dcc-uncontrolled-file-local-import-design\database-schema-evidence.md` -> PASS，`Database schema evidence is valid.`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS，`BDD/TDD acceptance plan validation passed.`
- UTF-8 read check for M15 task/evidence files -> PASS，全部 `contains_replacement=False`。
- Scoped `git diff --check` for M15 tracked files -> PASS，无 whitespace error。
- Trailing whitespace check for new SQL contract file -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS，M16 文档加固后 `BDD/TDD acceptance plan validation passed.`
- UTF-8 read check for M16 task and acceptance docs -> PASS，8 个文档全部 `contains_replacement=False`。
- `git -C E:\IntRuoyi diff --check -- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/task.md doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/execution-log.md doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/design.md doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/verification-report.md docs/acceptance/bdd-scenarios.md docs/acceptance/tdd-plan.md docs/acceptance/e2e-plan.md docs/acceptance/test-data.md` -> PASS，仅存在 Git LF-to-CRLF warning。
- `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 2, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260802-dcc-uncontrolled-file-local-import-design\backend-api-evidence.md` -> PASS，`Backend API evidence is valid.`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS，`BDD/TDD acceptance plan validation passed.`
- UTF-8/trailing whitespace check for M17 task/evidence/backend files -> PASS，`contains_replacement=[]`，`trailing_whitespace=[]`。
- `git -C E:\IntRuoyi diff --check -- <M17 tracked task/backend files>` -> PASS，仅存在 Git LF-to-CRLF warning。
- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期原因为服务返回新插入任务 `8202`，而不是事务内已存在的幂等任务 `8102`。
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 4, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- REGRESSION: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 8, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260802-dcc-uncontrolled-file-local-import-design\backend-api-evidence.md` -> PASS，`Backend API evidence is valid.`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS，`BDD/TDD acceptance plan validation passed.`
- UTF-8/trailing whitespace check for M19 task/evidence/backend files -> PASS，`UTF8_AND_TRAILING_WHITESPACE_CHECK_PASS`。
- `git -C E:\IntRuoyi diff --check -- <M19 tracked task/backend files>` -> PASS，仅存在 Git LF-to-CRLF warning。
- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest#nasControlAudit_mapsImportSelectedWithTransferWritePermission" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期原因为缺少 `/dcc/controlled-files/nas-control-audit/{taskId}/import-selected` endpoint mapping。
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest#nasControlAudit_mapsImportSelectedWithTransferWritePermission" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- REGRESSION: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 11, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_returnsBinaryForBoundTaskWithoutMutatingLocalOrArchiveState,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_rejectsCrossTaskOrStaleSignatureWithoutReadingNas" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期原因为 service interface 缺少 `readUncontrolledImportContent(...)`。
- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasUncontrolledImportControllerTest#nasUncontrolledImport_mapsContentAsBinaryWithSnapshotQueryParamsAndWritePermission" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期原因为缺少 `DccNasUncontrolledImportController`。
- GREEN: isolated worktree `D:\IntRuoyiWorktree\dcc-uncontrolled-import-m21-verify-20260803`; `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasUncontrolledImportControllerTest#nasUncontrolledImport_mapsContentAsBinaryWithSnapshotQueryParamsAndWritePermission,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_returnsBinaryForBoundTaskWithoutMutatingLocalOrArchiveState,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_rejectsCrossTaskOrStaleSignatureWithoutReadingNas" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 3, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- REGRESSION: isolated worktree `D:\IntRuoyiWorktree\dcc-uncontrolled-import-m21-verify-20260803`; `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest,DccNasUncontrolledImportControllerTest,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_returnsBinaryForBoundTaskWithoutMutatingLocalOrArchiveState,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_rejectsCrossTaskOrStaleSignatureWithoutReadingNas" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 14, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- RED: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am -rf :yudao-module-dcc "-Dmaven.resources.skip=true" "-Dtest=DccNasUncontrolledImportControllerTest#nasUncontrolledImport_mapsLocalWriteResultWithSnapshotBodyAndWritePermission,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_marksLocalWrittenWithoutArchiveSideEffects,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_replaysSameSuccessWithoutMutatingOrArchivingAgain,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_rejectsConflictingTerminalResultWithoutArchive" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期原因为 local-write-result controller/service/VO 契约尚未实现。
- GREEN: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am -rf :yudao-module-dcc "-Dmaven.resources.skip=true" "-Dtest=DccNasUncontrolledImportControllerTest#nasUncontrolledImport_mapsLocalWriteResultWithSnapshotBodyAndWritePermission,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_marksLocalWrittenWithoutArchiveSideEffects,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_replaysSameSuccessWithoutMutatingOrArchivingAgain,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_rejectsConflictingTerminalResultWithoutArchive" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 4, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- REGRESSION: `mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-dcc -am "-Dmaven.resources.skip=true" "-Dtest=DccNasControlAuditControllerTest,DccNasUncontrolledImportControllerTest,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_doesNotRequireLegacyNasTransferInputs,DccControlledFileNasTransferServiceTest#processWaitingTasks_skipsNasUncontrolledImportUntilContentAndLocalWritten,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_createsTaskItemsAndAuditBindingsAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsInvalidSelectionAtomically,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_returnsExistingTaskForSameIdempotencyHashRegardlessOfOrder,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rechecksIdempotencyInsideTransactionBeforeInsert,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsSameIdempotencyWithDifferentRequestHash,DccControlledFileNasTransferServiceTest#createUncontrolledImportTask_rejectsDuplicateAuditIdsBeforeHashingOrWrites,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_returnsBinaryForBoundTaskWithoutMutatingLocalOrArchiveState,DccControlledFileNasTransferServiceTest#readUncontrolledImportContent_rejectsCrossTaskOrStaleSignatureWithoutReadingNas,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_marksLocalWrittenWithoutArchiveSideEffects,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_replaysSameSuccessWithoutMutatingOrArchivingAgain,DccControlledFileNasTransferServiceTest#recordUncontrolledImportLocalWriteResult_rejectsConflictingTerminalResultWithoutArchive" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 18, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260802-dcc-uncontrolled-file-local-import-design\backend-api-evidence.md` -> PASS，`Backend API evidence is valid.`。
- `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS，`BDD/TDD acceptance plan validation passed.`。
- UTF-8/trailing whitespace check for M22 task/backend files -> PASS，`UTF8_AND_TRAILING_WHITESPACE_CHECK_PASS`。
- `git diff --check -- <M22 task evidence and backend files>` -> PASS，仅存在 Git LF-to-CRLF warning，无 whitespace error。

## Design Checks

- No fallback: 未识别文件进入 `未分类/待处理`，不默认项目代码、item、分类或 ZIP 降级。
- Existing reuse: 方案复用 `统计未受控文件`、NAS audit task、NAS transfer、DCC 项目代码和文件分类规则。
- TDD ready: 每个生产行为都有 RED 预期失败、GREEN 目标命令和相邻回归命令。
- E2E ready: 真实路径通过 NAS 管理页面、目录选择、本地写入、DCC 项目代码详情和受控文件详情核验，API 仅作最终只读核验。
- Closeout preview/apply ready: 当前任务目录只保留 `task.md`、`execution-log.md`、`design.md` 和 `verification-report.md`，没有可清理临时产物。
- Optimized gate ready: 已补强“先本地写入成功再归档”的两阶段门禁，避免本地写入失败、路径冲突、NAS 文件变化或归档失败被合并展示为成功。
- Path safety ready: 本地路径必须禁止 `..`、盘符、非法字符、Windows 保留名和规范化冲突；不允许覆盖、自动改名或取第一条。
- Status model ready: 新增明细以 `PENDING_RECOGNITION / NOT_SELECTED / NOT_STARTED` 作为初始状态；路径冲突和非法路径统一使用 `LOCAL_WRITE_FAILED + localWriteErrorCode`，不扩散下载状态枚举。
- Sequence ready: 目录授权和相对路径校验通过前不得创建 `NAS_UNCONTROLLED_IMPORT` 任务；取消目录选择和浏览器不支持目录写入时无 import task、无 content 请求、无 local-write-result。
- Command ready: 后续 RED/GREEN 命令从 `E:\IntRuoyi` 可定位后端 Maven 根、后端 SQL pytest 目录和前端 pnpm 脚本，避免引用不存在的根级 `script/tests`。
- Idempotency ready: 相同 `idempotencyKey` 返回原任务，已归档 audit file 使用不同 key 重复提交必须返回已处理或冲突，不创建第二个受控文件。
- Long-term gate ready: `docs/e2e-rules.md#浏览器本地目录写入门禁` 已覆盖 `showDirectoryPicker`、目录授权、请求顺序、取消授权和禁止 ZIP/API-only 降级，后续类似任务可直接复用。
- Large-file gate ready: content 下载必须使用二进制、流式或明确分块语义，禁止 JSON/base64 文件内容字段。
- State-machine gate ready: 文档已定义识别、下载和归档状态的允许流转、终态和并发同一 audit file 的事务检查要求。
- Local-path gate ready: 本地目标已存在、路径过长、非法段和规范化冲突均必须 fail fast，不覆盖、不截断、不自动改名。
- Schema slice ready: `dcc_nas_control_audit_file` additive migration、test schema、DO、Mapper、JUnit schema test 和 SQL contract 均已建立并通过验证。
- Recognition backend ready: `/files/recognize` 已复用项目代码、文件分类规则和 taxonomy path，覆盖 `MATCHED / UNCLASSIFIED_PENDING / AMBIGUOUS`、候选摘要、原因码和期望本地相对路径。

- API contract ready: GET /files, recognize, import-selected, content, and local-write-result now have testable fields, permissions, statuses, and error boundaries.
- No-state-mutation ready: unsupported browser, canceled directory picker, and local precheck failures happen before import task creation and must not mutate backend download status.
- Snapshot binding ready: content and local-write-result must match user, tenant, import task, audit file, source signature, and local relative path snapshot.
- Recognition snapshot ready: 文档要求持久化 `classification_candidates_json` 或等价快照、稳定 `classificationReason` 和 `expectedLocalRelativePath`，避免只在响应或日志里临时保存候选。
- Import idempotency ready: 文档要求保存 `request_hash`，相同 `idempotencyKey` 但选择文件、签名或本地路径变化时必须冲突。
- Import atomicity ready: 文档要求任一选中项无效时整体拒绝，不创建半成品 import task，不留下部分 `SELECTED` 或 import 绑定。
- Canonical hash ready: 文档要求 `request_hash` 对 `selectedFiles` 顺序不敏感，重复 `auditFileId` 在 hash 前失败。
- Local path trust boundary ready: 后端必须根据识别快照重算本地相对路径并拒绝前端篡改，不能把客户端路径当作分类事实来源。
- Selection scope ready: 首版只处理显式勾选的 `auditFileId` 列表；全筛选结果处理必须另有服务端 selection snapshot/token 和 TDD。
- Import binding ready: 文档要求 audit 明细与 import task/item 绑定可查询，活动绑定重复提交必须拒绝。
- Local write replay ready: 文档要求相同 local-write-result 重放幂等返回且不重复归档，冲突终态回写必须拒绝。
- Import snapshot schema ready: transfer task 已具备 `audit_task_id/idempotency_key/request_hash`，task item 已具备 audit file、source signature、识别快照、本地相对路径、本地写入和归档状态字段，audit 明细已具备 import task/item 绑定字段，并通过 JUnit 与 SQL contract 验证。
- Legacy field isolation ready: `NAS_UNCONTROLLED_IMPORT` 不得要求或伪造任务级 `templateCategoryId/effectiveDate/dccProjectCodeId` 等旧 NAS 转移全局目标字段，旧 `NAS` / `LOCAL_FOLDER` 入口仍需 fail fast 校验必填输入。
- Processor isolation ready: import-selected 只创建任务快照，不得由旧 waiting processor 自动读取 NAS、调用 DCC submit 或写 ACTIVE NAS 来源映射。
- M17 processor isolation verified: `NAS_UNCONTROLLED_IMPORT` waiting task 会在 `processWaitingTasks()` 中被跳过，不会触发 `claimWaitingTask`、NAS 读取、DCC submit 或 ACTIVE NAS source insert。
- Import-selected service boundary verified: 请求 VO 和服务签名已存在且不暴露旧 NAS transfer 目标字段；服务体已覆盖原子校验、快照持久化、audit 绑定和服务级幂等冲突/复用。
- Archive metadata gate ready: `LOCAL_WRITTEN` 后正式归档仍必须具备模板分类、生效日期、变更原因或等价正式来源；缺失时进入 `ARCHIVE_METADATA_REQUIRED` 或明确失败/阻塞状态。
- Idempotency concurrency verified: `createUncontrolledImportTask` 会在事务内使用 `FOR UPDATE` 二次查询 `auditTaskId + operatorUserId + sourceType + idempotencyKey`，相同 hash 复用原任务、不同 hash 冲突、重复 audit id 在 hash/写入前失败。
- Import-selected controller verified: `/import-selected` 已暴露为写入端点，要求 `submit + directory:manage + category:manage` 权限组合，并通过 `@Valid @RequestBody` 绑定无旧 transfer 默认字段的请求 VO。
- Content binary verified: `/content` 已暴露为二进制下载端点，要求 `submit + directory:manage + category:manage` 权限组合，并在 NAS 读取前绑定 user/import task/audit file/source signature/local relative path 快照。
- Local-write-result verified: `/local-write-result` 已暴露为写入端点，要求 `submit + directory:manage + category:manage` 权限组合；服务在 mutation 前绑定 user/import task/audit file/source signature/local relative path 快照，`LOCAL_WRITTEN` 只更新本地写入状态，不触发 NAS 读取、workflow submit、归档或 ACTIVE NAS source insert。

## Blockers

- Closeout Git blocker: 当前 `int_main` 仍相对 `origin/int_main` 存在未推送提交，且工作区存在任务开始前和其它并发任务改动及未跟踪目录。本任务未执行 baseline commit、implementation commit 或 push，避免把并发任务资产混入本任务收尾。
- Implementation blocker for future coding: 当前证据未确认独立 DCC item 表；后续正式归档实现前必须再次检索正式 item 模型。若存在独立 item 表，需先更新设计与测试，不得临时映射。
- M24 archive metadata blocker: 当前 `dcc_nas_control_audit_file` 与 `dcc_controlled_file_nas_transfer_task_item` 未保存处理项级正式归档元数据快照；正式创建受控文件和 ACTIVE NAS 来源映射前，必须先补 schema/VO/service RED，并证明 `categoryId/directoryId/effectiveDate/versionNo/changeType/fileNumber` 等字段来源可审计。

## M11 Backend API Slice

- Scope: implemented query-only backend slice for GET /dcc/controlled-files/nas-control-audit/{taskId}/files.
- Contract: CommonResult<PageResult<DccNasControlAuditFileRespVO>>, permission dcc:controlled-file:query, filters keyword/classificationStatus/downloadStatus/archiveStatus, stable id ASC ordering.
- RED: targeted controller contract failed on missing /files endpoint.
- GREEN: targeted controller contract passed with Tests run 1, Failures 0, Errors 0, Skipped 0.
- REGRESSION: DccNasControlAuditControllerTest and DccNasControlAuditServiceImplTest passed with Tests run 3, Failures 0, Errors 0, Skipped 0.
- Evidence: backend-api-evidence.md validator PASS.
- Remaining: import-selected, content, local-write-result, frontend static contract, and real E2E remain in progress.

## M13 Backend Recognition Slice

- Scope: implemented deterministic pre-recognition for `POST /dcc/controlled-files/nas-control-audit/{taskId}/files/recognize`.
- Contract: `CommonResult<DccNasControlAuditRecognizeRespVO>`, permission `dcc:controlled-file:query`, response counts for matched, pending, ambiguous and skipped rows.
- Behavior: unique project + category writes `MATCHED`; missing project/category writes `UNCLASSIFIED_PENDING`; multiple candidates writes `AMBIGUOUS`; unknown rule types fail fast.
- GREEN: targeted service tests passed with 4 recognition scenarios, and adjacent schema/controller/service regression passed with Tests run 9, Failures 0, Errors 0.
- Boundary: recognition does not read content, create import tasks, write local result, archive, or create controlled files.
- Remaining: import-selected, content binary download, local-write-result, frontend, and real E2E remain in progress.

## M14 Documentation Gate

- Scope: optimized design, BDD, TDD, E2E, test data, task and verification evidence for import-selected/local-write implementation readiness.
- Added gates: import-selected whole-request atomicity, canonical request hash, audit/import binding visibility, duplicate active binding rejection, local-write-result replay idempotency and conflicting terminal result rejection.
- Boundary: documentation-only update; no production code, NAS files, local folders, runtime services or business data were modified.
- Remaining: backend import-selected schema/service/controller, content binary download, local-write-result, frontend static contract and real E2E still require strict RED/GREEN implementation.

## M15 Schema Slice

- Scope: implemented additive schema support for `NAS_UNCONTROLLED_IMPORT` task snapshots and audit/import binding.
- Contract: task header stores audit task id, idempotency key and canonical request hash; task item stores audit file id, source signature, recognition snapshot, local relative path, local-write status/error and archive status/error; audit file stores selected import task/item binding.
- GREEN: targeted schema JUnit passed with Tests run 1, Failures 0, Errors 0, Skipped 0.
- GREEN: SQL static contracts passed with 4 tests.
- Evidence: database schema validator and BDD/TDD acceptance plan validator passed after evidence updates.
- Boundary: schema/persistence contract only; import-selected service/API, content binary download, local-write-result, frontend and real E2E remain in progress.

## M16 Documentation Gate

- Scope: optimized design, BDD, TDD, E2E, test data, task and verification evidence for import-selected implementation readiness beyond schema.
- Added gates: legacy transfer field isolation, processor isolation before content/local-write, idempotency concurrency lock, stable audit row locking, and formal archive metadata source validation.
- Verification: acceptance validator PASS, UTF-8 read check PASS, scoped git diff --check PASS.
- Boundary: documentation-only update; no production code, NAS files, local folders, runtime services or business data were modified.
- Remaining: backend import-selected service/API, content binary download, local-write-result, frontend static contract and real E2E still require strict RED/GREEN implementation.

## M17 Backend Import Isolation Slice

- Scope: implemented backend service contract and legacy processor isolation for `NAS_UNCONTROLLED_IMPORT`.
- Contract: `DccNasUncontrolledImportSelectedReqVO` contains selection scope, idempotency key and selected audit-file snapshots, without legacy task-level template category, effective date or project-code defaults.
- GREEN: targeted transfer service tests passed with Tests run 2, Failures 0, Errors 0, Skipped 0.
- Evidence: backend API validator, acceptance plan validator, UTF-8/trailing whitespace check and scoped diff-check passed after evidence updates.
- Boundary: `createUncontrolledImportTask` still fails fast until the next slice implements atomic import-selected validation and persistence.
- Remaining: import-selected task creation, canonical request hash, content binary download, local-write-result, archive, frontend static contract and real E2E remain in progress.

## M18 Backend Import Creation Slice

- Scope: implemented service-level atomic import-selected creation for explicit selected audit files.
- Contract: validates selection scope, duplicate audit ids, task ownership, source signature, importable classification status, download/archive status, expected local relative path, prior import binding and controlled-file state before any write.
- GREEN: targeted creation/rejection tests passed with Tests run 2, Failures 0, Errors 0, Skipped 0.
- REGRESSION: M17+M18 transfer service target set passed with Tests run 4, Failures 0, Errors 0, Skipped 0.
- Boundary: content binary download, local-write-result, archive execution, frontend static contract and real E2E remain in progress.

## M19 Backend Import Idempotency Slice

- Scope: implemented service-level idempotency hardening for `NAS_UNCONTROLLED_IMPORT`.
- Contract: same `idempotencyKey + requestHash` returns the existing task regardless of selected-file order; same key with different hash fails before audit reads or writes; duplicate audit ids fail before hash/persistence.
- RED: transaction-race test failed because the service inserted task `8202` instead of returning existing task `8102`.
- GREEN: targeted idempotency/rejection tests passed with Tests run 4, Failures 0, Errors 0, Skipped 0.
- REGRESSION: M17+M18+M19 transfer service target set passed with Tests run 8, Failures 0, Errors 0, Skipped 0.
- Boundary: content binary download, local-write-result, archive execution, frontend static contract and real E2E remain in progress.

## M20 Backend Import-Selected Controller Slice

- Scope: implemented controller contract for `POST /dcc/controlled-files/nas-control-audit/{taskId}/import-selected`.
- Contract: response is `CommonResult<DccControlledFileNasTransferRespVO>`; request uses path `taskId` plus `@Valid @RequestBody DccNasUncontrolledImportSelectedReqVO`; controller passes `getLoginUserId()` and `taskId` into `createUncontrolledImportTask`.
- Permission: requires `dcc:controlled-file:submit`, `dcc:controlled-file:directory:manage`, and `dcc:controlled-file:category:manage`.
- RED: targeted controller contract failed on missing `/import-selected` endpoint mapping.
- GREEN: targeted controller contract passed with Tests run 1, Failures 0, Errors 0, Skipped 0.
- REGRESSION: controller and M17-M19 import-selected service regression passed with Tests run 11, Failures 0, Errors 0, Skipped 0.
- Boundary: content binary download, local-write-result, archive execution, frontend static contract and real E2E remain in progress.

## M21 Backend Content Binary Slice

- Scope: completed content binary download for selected `NAS_UNCONTROLLED_IMPORT` audit files.
- Contract: `GET /dcc/controlled-files/nas-uncontrolled-import/tasks/{importTaskId}/files/{auditFileId}/content` returns `ResponseEntity<byte[]>`, requires `submit + directory:manage + category:manage`, and binds query `sourceSignature` plus `localRelativePath`.
- Service behavior: `readUncontrolledImportContent(...)` validates current user, import task, audit file, selected task item, source signature, local relative path, selected download status, local-write/archive not-started state, and absence of controlled file before NAS read.
- RED: targeted service compile failed because `readUncontrolledImportContent(...)` did not exist on the service interface.
- RED: targeted controller contract failed because `DccNasUncontrolledImportController` did not exist.
- GREEN: isolated worktree `D:\IntRuoyiWorktree\dcc-uncontrolled-import-m21-verify-20260803` targeted controller + service command passed with Tests run 3, Failures 0, Errors 0, Skipped 0.
- REGRESSION: isolated worktree `D:\IntRuoyiWorktree\dcc-uncontrolled-import-m21-verify-20260803` controller and M17-M21 service regression passed with Tests run 14, Failures 0, Errors 0, Skipped 0.
- Evidence: backend API validator PASS, acceptance plan validator PASS, UTF-8/trailing whitespace check PASS, and scoped `git diff --check` PASS for M21 task evidence files.
- Boundary: main worktree Maven pass is not claimed because earlier shared `target` contention made it unsafe; M21 PASS evidence is explicitly scoped to the isolated verification worktree.

## M22 Backend Local-Write-Result Slice

- Scope: completed local-write-result controller/service contract for selected `NAS_UNCONTROLLED_IMPORT` audit files.
- Contract: `POST /dcc/controlled-files/nas-uncontrolled-import/tasks/{importTaskId}/files/{auditFileId}/local-write-result` returns `CommonResult<DccControlledFileNasTransferRespVO>`, requires `submit + directory:manage + category:manage`, and binds `@Valid @RequestBody DccNasUncontrolledImportLocalWriteResultReqVO`.
- Service behavior: `recordUncontrolledImportLocalWriteResult(...)` validates current user, import task, audit file, selected task item, source signature, local relative path, non-archived state and terminal local-write status before mutation.
- RED: targeted controller + service command failed before implementation because local-write-result controller/service/VO contract did not exist.
- GREEN: targeted controller + service command passed with Tests run 4, Failures 0, Errors 0, Skipped 0.
- REGRESSION: controller and M17-M22 service regression passed with Tests run 18, Failures 0, Errors 0, Skipped 0.
- Evidence: local write success updates audit/task item state only; replayed `LOCAL_WRITTEN` is idempotent; conflicting terminal result is rejected; NAS read, workflow submit, controlled-file archive and ACTIVE NAS source insert are not called.
- Evidence validation: backend API evidence validator PASS, acceptance plan validator PASS, UTF-8/trailing whitespace check PASS, and scoped `git diff --check` PASS for M22 task/backend files.
- Boundary: formal archive execution, archive metadata required/blocked state, frontend static contract and real E2E remain in progress.

## M23 Backend Archive Metadata Blocker Slice

- Scope: completed the explicit archive metadata required blocker after matched uncontrolled import files reach `LOCAL_WRITTEN`.
- Service behavior: `recordUncontrolledImportLocalWriteResult(...)` now marks matched files as `downloadStatus=LOCAL_WRITTEN`, `archiveStatus=FAILED`, and `archiveErrorCode=ARCHIVE_METADATA_REQUIRED` when no formal archive metadata source exists; it does not use current date, legacy task defaults, empty template/category metadata, or any silent fallback.
- Side effects: targeted tests verify no NAS read, no `fileService.createFileAndReturnId(...)`, no `workflowService.submitControlledFileWithoutApproval(...)`, no controlled file id, and no ACTIVE NAS source insert on the metadata blocker path.
- RED: targeted service test failed because the previous implementation left `archiveStatus=NOT_STARTED` after `LOCAL_WRITTEN`.
- GREEN: targeted metadata blocker test passed with Tests run 1, Failures 0, Errors 0, Skipped 0.
- GREEN: local-write-result controller/service adjacent set passed with Tests run 5, Failures 0, Errors 0, Skipped 0.
- REGRESSION: controller and M17-M23 service regression passed with Tests run 19, Failures 0, Errors 0, Skipped 0.
- Evidence validation: backend API evidence validator PASS, acceptance plan validator PASS, UTF-8/trailing whitespace check PASS, and scoped `git diff --check` PASS for M23 task/backend files.
- Boundary: controlled-file creation, ACTIVE NAS source mapping and already-archived replay protection remain blocked until a formal archive metadata source is designed, stored and verified in M24.

## M25 Frontend Static Contract Slice

- Scope: completed static frontend contract and minimal NAS management page/API integration for downloading selected uncontrolled files into a user-authorized local directory.
- Contract: `src/api/system/nas/index.ts` exposes files page, recognize, import-selected, content binary download and local-write-result wrappers; `ControlledFileNasTransferSourceType` now includes `NAS_UNCONTROLLED_IMPORT`.
- UI behavior: completed audit tasks show uncontrolled file rows, matched rows are selectable, `UNCLASSIFIED_PENDING/AMBIGUOUS` remain visible as “未分类/待处理/待确认”, and `ARCHIVE_METADATA_REQUIRED` is shown as “归档元数据待补齐”.
- Local directory gate: page uses `showDirectoryPicker`; validates backend `expectedLocalRelativePath` for backslashes, absolute paths, drive letters, `.`, and `..`; creates import-selected only after directory authorization and path validation; does not store or send local absolute paths.
- Local write sequence: page downloads content Blob by import task/audit file/source signature/local relative path snapshot, writes through `getDirectoryHandle/getFileHandle/createWritable/write/close`, posts `LOCAL_WRITTEN` only after `close()`, and posts `LOCAL_WRITE_FAILED` with explicit error details if local write fails.
- RED: `node tests/e2e/dcc-nas-uncontrolled-local-import-static.spec.js` failed before implementation because the package script and NAS local import contract were missing.
- GREEN: `node tests/e2e/dcc-nas-uncontrolled-local-import-static.spec.js` -> PASS.
- GREEN: `pnpm e2e:dcc:nas-uncontrolled-local-import:static` -> PASS.
- GREEN: `node tests/e2e/nas-control-audit-static.spec.js` -> PASS.
- GREEN: UTF-8/trailing whitespace check -> PASS, `contains_replacement=[]`, `trailing_whitespace=[]`.
- GREEN: scoped `git diff --check` for M25 frontend files -> PASS, only LF-to-CRLF warnings.
- GREEN: `pnpm ts:check` -> PASS, current frontend workspace type check completed successfully.
- Boundary: real Playwright E2E and formal archive success path remain pending; this slice does not implement ZIP fallback, browser default download fallback, controlled-file creation, workflow submit, or ACTIVE NAS source mapping.

## M26 Archive Metadata Precondition Gate

- Scope: completed documentation and verification hardening for the M24 precondition before enabling formal archive success.
- Code evidence reviewed: `DccControlledFileNasTransferTaskItemDO` and `DccNasControlAuditFileDO` persist audit recognition/local-write/archive status snapshots but not a formal DCC submit metadata snapshot; `DccControlledFileSubmitReqVO` still requires `categoryId`, `directoryId`, `changeType`, `fileName`, `fileNumber`, `versionNo`, and `effectiveDate` for formal submission.
- Gate added: success archive path must fail RED until a processing-item-level metadata source exists and is bound to `auditFileId + sourceSignature + localRelativePath`; `matchedFileTypeTaxonomyId` and `classificationCandidatesJson` are not accepted as submit metadata facts.
- Blocker recorded: M24 controlled-file creation, workflow submit, and ACTIVE NAS source mapping remain blocked by missing formal metadata source; current behavior must stay `ARCHIVE_METADATA_REQUIRED` with no archive side effects.
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS, `BDD/TDD acceptance plan validation passed.`
- GREEN: UTF-8/trailing whitespace check for M26 verification report -> PASS, `contains_replacement=[]`, `trailing_whitespace=[]`.
- GREEN: `git diff --check -- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/verification-report.md` -> PASS, only Git LF-to-CRLF warning.

## M27 Documentation Consistency Gate

- Scope: completed a document consistency audit for the task package and acceptance documents before further implementation.
- RED: `node -e "<doc consistency marker scan>"` failed because `docs/acceptance/tdd-plan.md` lacked `未分类/待处理`, and `docs/acceptance/test-data.md` lacked `未分类/待处理`, `showDirectoryPicker`, and `NAS_UNCONTROLLED_IMPORT`.
- Fix: updated `docs/acceptance/tdd-plan.md` to explicitly require official `未分类/待处理` behavior for unresolved files, and updated `docs/acceptance/test-data.md` to bind local-directory samples to `showDirectoryPicker` and `NAS_UNCONTROLLED_IMPORT`.
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi` -> PASS, `BDD/TDD acceptance plan validation passed.`
- GREEN: `node -e "<doc consistency marker scan>"` -> PASS, `DOC_CONSISTENCY_PASS files=8 markers=5`.
- GREEN: `node -e "<utf8/trailing whitespace check>"` -> PASS, `UTF8_TRAILING_CHECK_PASS files=5`.
- GREEN: `git diff --check -- doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/task.md doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/execution-log.md doc/tasks/20260802-dcc-uncontrolled-file-local-import-design/verification-report.md docs/acceptance/tdd-plan.md docs/acceptance/test-data.md` -> PASS, only Git LF-to-CRLF warnings.
- Experience: merged the marker-scan lesson into `docs/e2e-rules.md#规划型 E2E 前置与业务 RED 分离门禁` and indexed keywords in `docs/experience-index.md`.
- GREEN: `rg -n "doc consistency marker scan|关键业务标记扫描|ARCHIVE_METADATA_REQUIRED" docs\experience-index.md docs\e2e-rules.md` -> PASS.
- GREEN: long-term docs UTF-8/trailing whitespace check and scoped `git diff --check` for `docs/e2e-rules.md` and `docs/experience-index.md` -> PASS, only Git LF-to-CRLF warnings.
- Boundary: documentation-only hardening; no production code, NAS file, local directory, database data, workflow submit, controlled-file creation, or ACTIVE NAS source mapping was modified.

## M24 Formal Archive Success Slice

- Scope: completed the formal archive success path after local write using processing-item-level archive metadata snapshots.
- Service behavior: `MATCHED + LOCAL_WRITTEN` now archives only when `archive*Snapshot` metadata is complete; it reads NAS content, uploads the original file, submits DCC workflow, writes exact ACTIVE NAS source mapping, sets audit/item `ARCHIVED`, and preserves replay idempotency.
- No-fallback boundary: missing archive metadata remains `ARCHIVE_METADATA_REQUIRED`; no legacy task header, current date, candidate JSON, default category/template, or taxonomy-only match is used to fabricate archive success.
- Schema behavior: `dcc_controlled_file_nas_transfer_task_item` now stores formal archive snapshot fields, and `NAS_UNCONTROLLED_IMPORT` task header no longer requires legacy `template_category_id/effective_date` defaults.
- RED/GREEN: M24 service RED failed on missing snapshot setters, then targeted archive success test passed; schema RED failed on incomplete migration contract, then targeted schema JUnit and Python SQL contracts passed.
- Regression: content/local-write/archive targeted set passed with 7 tests, including metadata blocker, replay, conflict, content snapshot binding, and formal archive success.
- Evidence validation: backend API validator, database schema validator, acceptance plan validator, scoped `git diff --check`, and UTF-8/trailing whitespace check all passed for M24 evidence.
- Remaining: real browser E2E and task closeout/commit/push are still pending due runtime/test-data prerequisites and mixed concurrent workspace state.