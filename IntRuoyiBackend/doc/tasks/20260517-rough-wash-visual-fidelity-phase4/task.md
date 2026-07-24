# Task: 粗洗工序左侧留白与版式微调

## Goal

继续对 `粗洗工序生产记录` 的生成报表做图片级优化，让最新生成结果更接近
用户提供的目标图。本轮聚焦一个局部但稳定可验证的版式差异：

- `生产批量汇总` 行需要先让出左侧竖排栏对应的白底区域，再开始灰底标题

## Scope

- 仅修改 `batchrecordreport` 相关的布局校准和对应测试
- 不修改识别入口、接口契约、业务数据源和非粗洗模板业务行为
- 继续使用真实 `Route B` 重生为最终视觉验证路径

## Previous Task Check

- Previous task:
  `doc/tasks/20260517-rough-wash-visual-fidelity-phase3/task.md`
- Status before this follow-up: completed
- Impact: phase3 已恢复顶部页眉、分页标题和表头层级；本轮只修正
  生产批量汇总行与左侧竖排栏之间的留白关系

## Milestones

- [x] M1: 创建本轮任务包并记录上一轮完成状态
- [x] M2: 写入 RED 测试，复现生产批量汇总行左侧起始过早的问题
- [x] M3: 最小修正粗洗固定布局，让汇总行先让出左侧留白
- [x] M4: 相关回归、打包、重启和真实 Route B 验证
- [x] M5: 更新任务证据并完成收口

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldKeepProductionBatchSummaryBehindRoughWashSideColumn -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportStyleEnhancerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package`
- `POST http://127.0.0.1:48081/admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B`
- latest screenshot artifact under:
  `doc/tasks/20260517-rough-wash-visual-fidelity-phase4/artifacts/`

## Current Status

Completed. The rough-wash page now keeps the previously restored full page
header and title bar, and the production batch summary row no longer collides
with the left side column.

## Blocker And Impact

- Blocker: the repository-specific `verify_tdd_compliance.py` admission script
  referenced by the local instructions is not present under either
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` or `D:\ProjectPackage\RagInt`.
- Impact: focused RED/GREEN evidence, package verification, runtime restart,
  real regeneration, and real screenshot validation all passed, but the extra
  repo-level TDD gate command could not be executed in this environment.

## Final Verification Result

- Focused backend test -> PASS, `1` test passed for the production batch
  summary left-gap regression.
- Related regression -> PASS, `24` tests passed across JSON builder,
  layout calibrator, and style enhancer coverage.
- Server packaging -> PASS, `mvn ... -Dmaven.test.skip=true package` rebuilt
  `yudao-server.jar`.
- Runtime restart -> PASS, backend switched to a fresh
  `D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-rough-wash-phase4-*.jar`,
  and `GET http://127.0.0.1:48081/v3/api-docs` returned HTTP `200`.
- Real rough-wash regeneration -> PASS,
  `POST /admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B`
  returned `importedCount=15`, `updatedCount=15`.
- Latest real screenshot validation -> PASS, the final screenshot at
  `doc/tasks/20260517-rough-wash-visual-fidelity-phase4/artifacts/rough-wash-batch-summary-left-gap-20260517-1205.png`
  preserves the restored page header and keeps the production batch summary row
  aligned behind the left side column as intended.

## Cleanup Keep

- doc/tasks/20260517-rough-wash-visual-fidelity-phase4/artifacts/rough-wash-batch-summary-left-gap-20260517-1205.png
