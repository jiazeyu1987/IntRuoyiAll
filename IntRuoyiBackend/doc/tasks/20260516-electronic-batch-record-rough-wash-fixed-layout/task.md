# Task: 电子批记录粗洗工序固定版式校准

## Goal

根据用户提供的粗洗工序目标图片，持续优化 `粗洗工序生产记录` 的生成报表，
让系统截图尽量接近原始纸质记录单。当前已经完成并反复验证的目标包括：

- 恢复整页页眉：`球囊扩张压力泵生产记录 / 记录编号 / RE-PP-ID-01 / 版本 / A/1`
- 恢复粗洗标题灰条和页头层次
- 让勾选行按最新目标图显示为双空框
- 保留单页显示、固定表头、多级表头和页脚式 `生效日期`

## Scope

- 仅修改 `batchrecordreport` 相关的布局校准、报表 JSON 生成、样式增强和对应测试
- 不修改识别入口、接口契约、业务数据源和非粗洗模板业务行为
- 继续使用真实 `Route B` 生成为最终视觉验证路径

## Previous Task Check

- Previous task: `doc/tasks/20260516-batch-record-single-page-layout-constraints/task.md`
- Status before this task: completed for shared single-page constraints
- Impact: the shared compression and fillability rules were already available,
  so this task focused on rough-wash-specific visual fidelity

## Milestones

- [x] M1: 对照用户目标图，恢复粗洗页固定结构
- [x] M2: 通过 RED/GREEN 修复页脚式 `生效日期`
- [x] M3: 强化标题灰条、首行表头层次和勾选态规则
- [x] M4: 恢复整页页眉并用真实 `Route B` 重生验证
- [x] M5: 更新任务证据并完成本轮收口

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportStyleEnhancerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package`
- `POST http://127.0.0.1:48081/admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B`
- 最新真实截图：
  `doc/tasks/20260516-electronic-batch-record-rough-wash-fixed-layout/artifacts/rough-wash-B-live-20260517-0248.png`

## Current Status

Completed. The rough-wash page now restores the full page header, keeps the
rough-wash title bar, preserves the fixed multi-level table structure, shows
the checklist row in the latest target-image unchecked state, and renders the
effective date as a footer-like element instead of a boxed table row.

## Blocker And Impact

- Blocker: the repository-specific `verify_tdd_compliance.py` admission script
  referenced by the local instructions is not present under either
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` or `D:\ProjectPackage\RagInt`.
- Impact: focused RED/GREEN evidence, package verification, runtime restart,
  real regeneration, and real screenshot validation all passed, but the extra
  repo-level TDD gate command could not be executed in this environment.

## Final Verification Result

- Focused backend tests -> PASS, `20` tests passed across the rough-wash
  JSON-builder, layout-calibrator, and style-enhancer suites.
- Server packaging -> PASS, `mvn ... -Dmaven.test.skip=true package` rebuilt
  `yudao-server.jar`.
- Runtime restart -> PASS, backend switched to a fresh
  `D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-rough-wash-tune-*.jar`,
  and `GET http://127.0.0.1:48081/v3/api-docs` returned HTTP `200`.
- Real rough-wash regeneration -> PASS,
  `POST /admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B`
  returned `importedCount=15`, `updatedCount=15`.
- Latest real screenshot validation -> PASS, the final screenshot at
  `doc/tasks/20260516-electronic-batch-record-rough-wash-fixed-layout/artifacts/rough-wash-B-live-20260517-0248.png`
  restores the full page header, shows the unchecked checklist row, keeps the
  title bar shading, and is visibly closer to the user-provided target image.

## Cleanup Keep

- doc/tasks/20260516-electronic-batch-record-rough-wash-fixed-layout/artifacts/rough-wash-B-live-20260517-0248.png
