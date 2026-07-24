# Task: 粗洗页眉比例贴近原 DOC

## Goal

继续收紧 `粗洗工序生产记录` 的报表形态，让页眉三段比例更接近用户提供的
目标图。目标是让粗洗页眉在 20 列固定布局中保持：

- 左侧标题区 `球囊扩张压力泵生产记录` 占 `10` 列
- 中间标签区 `记录编号 / 版本` 占 `4` 列
- 右侧值区 `RE-PP-ID-01 / A/1` 占 `6` 列

## Scope

- 仅验证并固化粗洗页眉三段比例
- 仅修改 `batchrecordreport` 相关布局校准和测试
- 不修改识别入口、接口契约、业务数据源和非粗洗模板业务行为

## Previous Task Check

- Previous task:
  `doc/tasks/20260517-rough-wash-visual-fidelity-phase4/task.md`
- Status before this follow-up: completed
- Impact: phase4 已修正 `生产批量汇总` 左侧留白，本轮只验证页眉比例是否贴近原 DOC

## Milestones

- [x] M1: 创建任务记录并确认上一轮已完成
- [x] M2: 写入 RED 测试复现页眉 `9 / 4 / 7` 比例偏差
- [x] M3: 最小修正页眉列跨度为 `10 / 4 / 6`
- [x] M4: 跑 focused 与相关回归测试
- [x] M5: 打包、重启后端、真实 Route B 重生并截图
- [x] M6: 补齐执行证据、closeout 预览并完成收口

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportLayoutCalibratorTest#calibrate_shouldUseDocLikeHeaderProportionsForRoughWash -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportStyleEnhancerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package`
- `GET http://127.0.0.1:48081/v3/api-docs`
- `POST http://127.0.0.1:48081/admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B`
- latest screenshot artifact under:
  `doc/tasks/20260517-rough-wash-visual-fidelity-phase5/artifacts/`

## Current Status

Completed. The rough-wash page header keeps the doc-like `10 / 4 / 6`
proportions and remains stable under real Route B regeneration.

## Blocker And Impact

- Blocker: the repository-specific `verify_tdd_compliance.py` admission script
  referenced by the local instructions is not present under either
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` or `D:\ProjectPackage\RagInt`.
- Impact: focused RED/GREEN evidence, package verification, runtime restart,
  real regeneration, and real screenshot validation all passed, but the extra
  repo-level TDD gate command could not be executed in this environment.

## Final Verification Result

- Focused backend test -> PASS, `1` test passed for the rough-wash header proportion regression.
- Related regression -> PASS, `25` tests passed across JSON builder, layout calibrator, and style enhancer coverage.
- Server packaging -> PASS, `mvn ... -Dmaven.test.skip=true package` rebuilt `yudao-server.jar`.
- Runtime restart -> PASS, backend remained reachable at `http://127.0.0.1:48081/v3/api-docs` with HTTP `200`.
- Real rough-wash regeneration -> PASS, `POST /admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B` returned `importedCount=15`, `updatedCount=15`.
- Latest real screenshot validation -> PASS, the final screenshot at
  `doc/tasks/20260517-rough-wash-visual-fidelity-phase5/artifacts/rough-wash-title-centered-20260517-1240.png`
  keeps the restored page header and preserves the expected three-block width relationship.

## Cleanup Keep

- doc/tasks/20260517-rough-wash-visual-fidelity-phase5/artifacts/rough-wash-title-centered-20260517-1240.png
