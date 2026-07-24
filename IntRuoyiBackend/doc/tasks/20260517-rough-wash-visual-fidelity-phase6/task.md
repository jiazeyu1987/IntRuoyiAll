# Task: 粗洗页通用视觉规则收口

## Goal

把粗洗相关的视觉优化从“按表名修”收敛成“按结构/形态修”的通用规则，
重点覆盖四类通用能力：

- 按行形态/单元格形态识别表头和灰底，而不是按模板名
- 保留多段工艺页的重复子表头，不把它们压平
- 统一外框/分区线/普通网格线的粗细层级
- 统一页宽预算和说明区留白，让不同工序页都能稳定落在同一套规则里

## Scope

- 仅修改 `batchrecordreport` 相关的布局校准、报表 JSON 生成、样式增强和对应测试
- 不修改识别入口、接口契约、业务数据源和非粗洗模板业务行为
- 以真实 `Route B` 为验证入口，但优化目标必须是通用规则而不是单表分支

## Previous Task Check

- Previous task:
  `doc/tasks/20260517-rough-wash-visual-fidelity-phase5/task.md`
- Status before this follow-up: completed
- Impact: rough-wash 页眉比例已经收口，本轮转向把剩余视觉规则抽成通用逻辑

## Milestones

- [x] M1: 创建任务包并记录上一轮完成状态
- [ ] M2: 4 个子 agent 并行完成各自的通用规则实现
- [ ] M3: 相关回归测试转绿并通过打包
- [ ] M4: 真实 Route B 重生与截图复核
- [ ] M5: 更新任务证据并完成收口

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesProBatchRecordReportJsonBuilderTest,MesProBatchRecordReportLayoutCalibratorTest,MesProBatchRecordReportStyleEnhancerTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package`
- `POST http://127.0.0.1:48081/admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B`
- screenshot artifacts under:
  `doc/tasks/20260517-rough-wash-visual-fidelity-phase6/artifacts/`

## Current Status

Blocked. This phase was interrupted by a higher-priority request to restore
MES 工艺路线 data and will resume later if re-issued.

Subagent slice 1/4 is complete: the style enhancer now derives header and
section shading from row/cell shape only, with no template-name lookup. The
other three slices remain pending.

Blocker: this task was interrupted by a higher-priority request to restore
five MES 工艺路线 records. No further work from this phase was performed in
this turn.
