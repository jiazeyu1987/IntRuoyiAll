# Task: 粗洗工序视觉拟真二轮优化

## Goal

继续对 `粗洗工序生产记录` 的生成报表做图片级优化，让最新生成结果更接近
用户提供的目标图。本轮聚焦 4 类视觉差异：

- `/pcs` 的红色强调
- 外框、分区线、普通网格线的粗细层级
- 灰底层级统一
- 顶部字段行与说明区的层次感

## Scope

- 仅修改 `batchrecordreport` 相关的布局校准、报表 JSON 生成、样式增强和对应测试
- 不修改识别入口、接口契约、业务数据源和非粗洗模板业务行为
- 继续使用真实 `Route B` 重生为最终视觉验证路径

## Previous Task Check

- Previous task:
  `doc/tasks/20260516-electronic-batch-record-rough-wash-fixed-layout/task.md`
- Status before this follow-up: completed
- Impact: the previous round already restored the full page header and
  unchecked checklist row, so this round could focus on finer visual hierarchy

## Milestones

- [x] M1: 创建本轮任务包并记录上一轮完成状态
- [x] M2: 为 `/pcs` 强调色、线条层级、灰底规则补 RED 用例
- [x] M3: 实现样式增强并确保不破坏现有单页布局
- [x] M4: 重新打包、真实重生 Route B 粗洗页并抓取新截图
- [x] M5: 更新任务证据并完成收口

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportStyleEnhancerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package`
- `POST http://127.0.0.1:48081/admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B`
- latest screenshot artifact under:
  `doc/tasks/20260517-rough-wash-visual-fidelity-phase2/artifacts/`

## Current Status

Completed. The rough-wash page now keeps the restored full page header from the
previous round, and this round adds `/pcs` red emphasis plus stronger
header/section border hierarchy while preserving the single-page layout.

## Blocker And Impact

- Blocker: the repository-specific `verify_tdd_compliance.py` admission script
  referenced by the local instructions is not present under either
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` or `D:\ProjectPackage\RagInt`.
- Impact: focused RED/GREEN evidence, package verification, runtime restart,
  real regeneration, and real screenshot validation all passed, but the extra
  repo-level TDD gate command could not be executed in this environment.

## Final Verification Result

- Focused backend tests -> PASS, `22` tests passed across JSON builder,
  layout calibrator, and style enhancer coverage.
- Server packaging -> PASS, `mvn ... -Dmaven.test.skip=true package` rebuilt
  `yudao-server.jar`.
- Runtime restart -> PASS, backend switched to a fresh
  `D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-rough-wash-phase2-*.jar`,
  and `GET http://127.0.0.1:48081/v3/api-docs` returned HTTP `200`.
- Real rough-wash regeneration -> PASS,
  `POST /admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B`
  returned `importedCount=15`, `updatedCount=15`.
- Latest real screenshot validation -> PASS, the final screenshot at
  `doc/tasks/20260517-rough-wash-visual-fidelity-phase2/artifacts/rough-wash-B-live-20260517-1117.png`
  keeps the restored page header, preserves the unchecked checklist row, adds
  stronger line hierarchy, and shows the `/pcs` quantity headers in red.

## Cleanup Keep

- doc/tasks/20260517-rough-wash-visual-fidelity-phase2/artifacts/rough-wash-B-live-20260517-1117.png
