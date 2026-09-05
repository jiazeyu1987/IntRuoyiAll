# Verification Report

## Scope
只验证生产组长报工管理展开明细中物料标题显示真实物料名称，不处理设备参数、提交可编辑性或其它一线生产问题。

## Results
- RED: `pnpm exec playwright test tests/e2e/team-leader-submission-material-real-name-static.spec.cjs --reporter=line` -> FAIL，缺少真实物料名解析器，当前代码会回退生成 `物料 ${index + 1}`。
- RED: 用户运行态反馈 -> FAIL，页面显示“物料名称未记录”，说明列表接口也没有从正式报工物料表补齐旧记录名称。
- GREEN: `node tests\e2e\team-leader-submission-material-real-name-static.spec.cjs` -> PASS。
- REGRESSION: `node tests\e2e\team-leader-production-report-payload-columns-static.spec.cjs` -> PASS。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=ProcessPoolTimelineQueryTest#shouldEnrichProductionMaterialNamesFromFormalFeedbackMaterials,ProcessPoolTimelineReportAllocationProjectionTest#shouldBatchProjectCurrentAllocationsAndReleasedState,MesProFrontlineFeedbackSubmitServiceTest#shouldUseMinimumMaterialCompletionForProgressAndPersistEveryMaterial" test` -> PASS，3 tests, 0 failures/errors。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit --pretty false` -> PASS。
- GREEN: `git diff --check` -> PASS，仅有 Windows LF/CRLF 提示，无空白错误。
- GREEN: `node tests\e2e\production-leader-active-order-process-submission-detail-static.spec.cjs` -> PASS。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProFrontlineFeedbackSubmitServiceTest#shouldUseMinimumMaterialCompletionForProgressAndPersistEveryMaterial,ProcessPoolTimelineQueryTest,ProcessPoolTimelineReportAllocationProjectionTest" test` -> PASS，8 tests, 0 failures/errors/skipped。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit --pretty false` -> PASS，收尾复跑无输出。
- GREEN: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，int_main/int_main 端口 8081/48081。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260905-team-leader-material-real-name-display/backend-api-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260905-team-leader-material-real-name-display/frontend-feature-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260905-team-leader-material-real-name-display --mode preview` -> PASS，blocked/warnings 均无。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260905-team-leader-material-real-name-display --mode apply` -> PASS。

## Notes
- 收尾阶段已移除本任务相邻静态合同的 EOF 多余空行，`git diff --check` 全量通过。
- 新提交报工时，后端会把验证后的 `materialCode/materialName/materialSpecification` 写入 `materialDetails` 快照。
- 旧提交报工时，生产组长时间线接口会按 `sourceFeedbackId` 查询 `mes_pro_feedback_material` 并补齐真实物料名。
- 实现提交：`5c969a5ff`；临时 evidence 已按 cleanup apply 删除。
