# Task: 其余三张工序页修改后对图复核

## Goal

在通用规则改动之后，重新对 `精洗工序生产记录`、`清洗工序生产记录`、
`清洁工序生产记录` 做图片级复核，确认相对上一轮是否有了优化，并记录还
剩哪些通用差异。

## Scope

- 仅做“修改后”真实生成结果的截图复核与分析，不改生产代码
- 使用当前分支最新代码重新打包、重启、重生 Route B 后截图
- 输出逐张结论、是否有优化、以及剩余的通用问题

## Previous Task Check

- Previous task:
  `doc/tasks/20260517-other-three-process-record-compare-analysis/task.md`
- Status before this follow-up: completed
- Impact: previous analysis给出了三张工序页的初始差异清单；本轮验证通用规则改动后是否有正向变化

## Milestones

- [x] M1: 创建任务包并确认上一轮已完成
- [x] M2: 重新打包并重启当前最新 backend
- [x] M3: 真实重生 Route B 并抓取精洗/清洗/清洁三张当前截图
- [x] M4: 与原图逐张比较，判断是否有优化
- [x] M5: 更新任务证据并完成收口

## Expected Verification

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package`
- `GET http://127.0.0.1:48081/v3/api-docs`
- `POST http://127.0.0.1:48081/admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B`
- screenshot artifacts under:
  `doc/tasks/20260517-other-three-process-post-change-compare/artifacts/`

## Current Status

Completed. The modified generic rules improved all three pages, with `精洗` closest to the target, `清洗` still the largest structural gap, and `清洁` improved but still needing width/spacing refinement.
