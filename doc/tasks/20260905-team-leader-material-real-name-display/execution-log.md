# Execution Log

BDD: 展开报工明细显示真实物料名 -> Given 生产组长报工列表展开一条包含多条物料明细的提交记录，且提交快照中包含正式物料名称 When 页面渲染物料明细标题 Then 标题显示真实物料名称，不显示“物料 1 / 物料 2”占位。
BDD: 时间线从正式报工物料表补齐物料名 -> Given 生产提交事件 raw_payload 的 materialDetails 只有 materialId When 生产组长查询报工列表 Then 接口按 sourceFeedbackId 查询 mes_pro_feedback_material 并返回真实 materialName/materialCode。

RED: `pnpm exec playwright test tests/e2e/team-leader-submission-material-real-name-static.spec.cjs --reporter=line` -> FAIL，缺少真实物料名解析器，当前代码会回退生成 `物料 ${index + 1}`。
RED: 用户运行态反馈 -> FAIL，页面显示“物料名称未记录”，说明时间线接口没有从正式报工物料表补齐旧记录名称。
GREEN: `node tests\e2e\team-leader-submission-material-real-name-static.spec.cjs` -> PASS。
REGRESSION: `node tests\e2e\team-leader-production-report-payload-columns-static.spec.cjs` -> PASS。
GREEN: `mvn -pl yudao-module-mes "-Dtest=ProcessPoolTimelineQueryTest#shouldEnrichProductionMaterialNamesFromFormalFeedbackMaterials,ProcessPoolTimelineReportAllocationProjectionTest#shouldBatchProjectCurrentAllocationsAndReleasedState,MesProFrontlineFeedbackSubmitServiceTest#shouldUseMinimumMaterialCompletionForProgressAndPersistEveryMaterial" test` -> PASS，3 tests, 0 failures/errors。
GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit --pretty false` -> PASS。
GREEN: `git diff --check -- <本轮修改文件>` -> PASS。
RESOLVED: 删除 `IntRuoyiFronted/tests/e2e/production-leader-active-order-process-submission-detail-static.spec.cjs` 的 EOF 多余空行后，全量 `git diff --check` -> PASS。
GREEN: `node tests\e2e\team-leader-submission-material-real-name-static.spec.cjs` -> PASS，收尾复跑通过。
GREEN: `node tests\e2e\production-leader-active-order-process-submission-detail-static.spec.cjs` -> PASS，收尾复跑通过。
GREEN: `node tests\e2e\team-leader-production-report-payload-columns-static.spec.cjs` -> PASS，收尾复跑通过。
FIX: `production-leader-active-order-process-submission-detail-static.spec.cjs` 改为基于测试文件位置解析前端根目录，避免从仓库根运行时误读 `E:\IntRuoyi\src`。
GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProFrontlineFeedbackSubmitServiceTest#shouldUseMinimumMaterialCompletionForProgressAndPersistEveryMaterial,ProcessPoolTimelineQueryTest,ProcessPoolTimelineReportAllocationProjectionTest" test` -> PASS，8 tests, 0 failures/errors/skipped。
GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit --pretty false` -> PASS，收尾复跑无输出。
GREEN: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，int_main/int_main 端口 8081/48081。
GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260905-team-leader-material-real-name-display/backend-api-evidence.md` -> PASS。
GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260905-team-leader-material-real-name-display/frontend-feature-evidence.md` -> PASS。
EXPERIENCE: 已合并到 `docs/backend-development.md`、`docs/frontend-development.md` 和 `docs/experience-index.md`，记录报工明细物料标题必须来自正式 `materialName`/服务端提交快照，不得用序号、编码或 ID 生成可见占位。
GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260905-team-leader-material-real-name-display --mode preview` -> PASS，keep task/execution/verification，delete 临时 evidence，blocked/warnings 均无。
COMMIT: `5c969a5ff` -> `fix: show active order submission material details`，包含物料真实名称补齐、提交快照修复、静态合同和长期经验文档。
GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260905-team-leader-material-real-name-display --mode apply` -> PASS，删除 backend/frontend evidence，保留 task/execution/verification。
