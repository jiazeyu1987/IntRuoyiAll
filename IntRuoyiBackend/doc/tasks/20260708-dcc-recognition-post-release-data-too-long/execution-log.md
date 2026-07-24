# Execution Log: DCC 产品名称识别 status 截断 post-release 修复

BDD: 识别记录 status 必须符合数据库安全枚举 -> Given 测试服产品名称识别返回业务失败、异常或未知状态 / When 系统写入 `dcc_controlled_file_recognition_record` / Then `status` 只能是受支持且长度不超过 16 的识别状态，详细原因进入 `failure_message` 或匹配字段，不得触发数据库截断。

GREEN: experience-preflight -> PASS，已读取 PowerShell、服务器访问、发布恢复和 bug regression 门禁；允许继续测试服只读审计，发布/重启前仍需再次记录目标版本与操作范围。

EVIDENCE: 上一轮校正结论 -> `release-20260708-dcc-recognition-fix-4` 后以后端容器启动时间 `2026-07-08T08:39:52Z` 为基准，测试服仍新增 4 条 `Data truncation: Data too long for column 'status'`，因此不能声明已修复。

EVIDENCE: 测试服只读审计 -> `release-20260708-dcc-recognition-fix-4` 当前仍运行在测试服，后端容器启动时间 `2026-07-08T08:39:52.026340163Z`；`dcc_controlled_file_recognition_record.status` 为 `varchar(16)`；该启动时间之后仍存在 4 条 `failure_message = Data truncation: Data too long for column 'status' at row 1`，受影响文件为 `2054545668044057075/7077/7078/7079`，均属于 batch task `18`。

ROOT-CAUSE: 代码根因 -> 主识别服务已把显式业务状态缩短为 `UNKNOWN_DCC` / `NAME_MISMATCH`，但 `upsertRecognitionRecord(...)` 作为识别记录持久化边界没有统一白名单校验；如果后续或历史分支把 `UNRECOGNIZED_PROJECT_NAME` 等长状态传入，当前实现会直接调用 mapper，让 MySQL `varchar(16)` 截断错误成为第一现场，并被外层失败记录二次写成 `FAILED + Data too long...`，掩盖原始非法状态来源。

RED: `mvn.cmd -pl yudao-module-dcc "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest#upsertRecognitionRecordRejectsUnsupportedStatusBeforeDatabaseWrite" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，当前实现没有抛出 `IllegalStateException`，非法长状态会进入 `recognitionRecordMapper.upsert(...)`。

GREEN: `mvn.cmd -pl yudao-module-dcc "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest#upsertRecognitionRecordRejectsUnsupportedStatusBeforeDatabaseWrite" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`upsertRecognitionRecord(...)` 已在调用 mapper 前拒绝非白名单状态。

GREEN: `mvn.cmd -pl yudao-module-dcc "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccControlledFileBatchRecognitionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，60 tests，覆盖单文件识别与批量识别回归。

BLOCKER: `mvn.cmd -pl yudao-server -am "-DskipTests" clean package` -> FAIL，测试服发布包构建被 MES 已提交代码阻塞：`MesProScheduleOrderServiceImpl` 调用 `MesProRouteProductMapper.selectListByItemId(Long)`，但当前已提交 HEAD 缺少 mapper 方法；该问题不由本次 DCC 修复引入，但会阻止新测试服版本发布。

GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderAdmissionDiffServiceTest#getAdmissionDiff_shouldBlockDuplicateRouteProductMappingsWithoutSystemException" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，补齐 `MesProRouteProductMapper.selectListByItemId(Long)` 后，MES 重复工艺路线产品映射阻断用例通过，证明构建前置缺口已被最小修复覆盖。

GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteProductMapperTest,MesProScheduleOrderAdmissionDiffServiceTest#getAdmissionDiff_shouldBlockDuplicateRouteProductMappingsWithoutSystemException" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，2 tests，新增 mapper 级真实数据库测试覆盖 `selectListByItemId(Long)` 仅返回目标产品的多条工艺路线关联。

BLOCKER: `build-release release-20260708-dcc-status-guard-v2-6c053a4f2b` -> FAIL，发布门禁报 `Release migration metadata missing: sql/mysql/20260708_system_user_table_column_config.sql`；该 SQL 文件存在，但缺少首行 `-- release-migration:` 元数据，导致 manifest 无法纳入 required SQL。

GREEN: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> PASS，migrationCount=261；已为 `20260708_system_user_table_column_config.sql` 补齐 `allowedEnvironments=test,backup,prod; type=schema; riskLevel=medium`，发布迁移元数据门禁通过。

GREEN: `python -X utf8 -m pytest script/tests/test_system_user_table_column_config_sql.py -q` -> PASS，3 tests，覆盖用户列表列配置迁移的 release 元数据、建表契约和非破坏性约束。

GREEN: `build-release release-20260708-dcc-status-guard-v3-e1bd69ce96` -> PASS，operation `op-2026-07-08T093634259105900Z-8e8bd932-1b01-46bd-8b9b-d49b1a394d1d`；发布包位于 `E:\Int\CacheData\IntRuoyi\publish-int-ruoyi\release-20260708-dcc-status-guard-v3-e1bd69ce96`，manifest 显示后端 source commit `e1bd69ce9663d586b663d790fce063176e2655e0` 且 dirty=false，前端 source commit `bea908ae29e2c04ab3ebb622b1efbe27fc6ed973` 且 dirty=false，required SQL 包含 `20260629_dcc_controlled_file_recognition_record.sql` 与 `20260708_system_user_table_column_config.sql`。

GREEN: `publish-test release-20260708-dcc-status-guard-v3-e1bd69ce96` -> PASS，operation `op-2026-07-08T095528809526300Z-fa95b6b7-01f2-4389-8c70-4948fce2c487`；测试服后端健康检查 HTTP 200，前端 HTTP 200，`pdf.worker.mjs` HTTP 200 `application/javascript`，`20260708_system_user_table_column_config` migration 状态为 APPLIED。

GREEN: 测试服运行态审计 -> PASS，`.env IMAGE_TAG=release-20260708-dcc-status-guard-v3-e1bd69ce96`，后端镜像 `intruoyi-backend:release-20260708-dcc-status-guard-v3-e1bd69ce96`，前端镜像 `intruoyi-frontend:release-20260708-dcc-status-guard-v3-e1bd69ce96`；后端启动时间 `2026-07-08T10:08:30.462225196Z`，后端 health `{"status":"UP"}`，前端 HTTP 200，`pdf.worker.mjs` HTTP 200 `application/javascript`。

GREEN: 测试服数据库后验审计 -> PASS，数据库 `NOW()=2026-07-08 18:11:42`、`UTC_TIMESTAMP()=2026-07-08 10:11:42`；`dcc_controlled_file_recognition_record.status` 为 `varchar(16)`；以后端新启动时间 `2026-07-08 10:08:30` UTC 为基准，`post_start_data_too_long_count=0`，确认新版本启动后无新增 `Data truncation: Data too long for column 'status'`。

GREEN: migration audit -> PASS，`infra_release_migration` 中 `20260708_system_user_table_column_config` 对应 release tag `release-20260708-dcc-status-guard-v3-e1bd69ce96`，状态 APPLIED，`update_time=2026-07-08 18:08:13`，`error_message=NULL`。

GREEN: task-closeout-cleanup preview -> PASS，`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-dcc-recognition-post-release-data-too-long --mode preview` 返回 `status: ready`；保留 `task.md` 与 `execution-log.md`，`delete=<none>`，`blocked=<none>`，`warnings=<none>`。

GREEN: maintenance console config restore -> PASS，`runtime-control.local.yaml` 已恢复 `repo-root=D:/ProjectPackage/Int/IntRuoyi/ruoyi-vue-pro` 与 `frontend-root=D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3`；维护控制台从 PID 55256 精确重启为 PID 32160，`http://127.0.0.1:48181/actuator/health` 返回 HTTP 200 / UP。
