# Execution Log

BDD: 恢复数据向导不再要求演练报告和现场快照 -> Given 操作员选择数据异常场景 / When 后端返回恢复数据推荐 / Then `requiredEvidence` 不包含 `rehearsal-report` 和 `现场快照`。

BDD: 缺少演练证据不产生恢复推荐阻断 -> Given 存在 manifest、checksum、镜像标签完整但缺少演练报告和现场快照的备份点 / When 查询数据异常推荐 / Then 恢复候选保持 `AVAILABLE`，推荐阻断原因不包含演练或现场快照。

BDD: 基础恢复证据仍需 fail fast -> Given 备份点缺少 manifest、checksum 或镜像标签 / When 查询恢复候选或推荐 / Then 后端仍返回 `BLOCKED` 和明确阻断原因。

RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeOpsGuideServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，4 tests 中 2 failures；失败原因是 `data-exception` 场景与推荐结果仍把 `rehearsal-report` / `现场快照` 作为恢复数据必需证据。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeOpsGuideServiceImplTest,RuntimeRestoreCandidateServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，12 tests，0 failures，0 errors。

VERIFY: `python -X utf8 C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --evidence docs\changes\20260603-restore-data-guide-alignment.md` -> PASS。

VERIFY: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260603-restore-data-guide-alignment\backend-api-evidence.md` -> PASS。

VERIFY: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260603-restore-data-guide-alignment --mode preview` -> PASS，delete `<none>`、blocked `<none>`、warnings `<none>`。
