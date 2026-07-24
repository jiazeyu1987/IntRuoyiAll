# Task: 其余三张工序记录表对图分析

## Goal

对当前系统里生成的另外三张工序记录表做图片级对比分析，和用户给出的
对应图片逐张比较，明确还能怎么优化，并尽量提炼成可复用的通用规则。
本轮分析对象为：

- 精洗工序生产记录
- 清洗工序生产记录
- 清洁工序生产记录

## Scope

- 仅做对图分析和证据整理，不改生产代码
- 使用当前系统中的真实生成结果做截图比对
- 输出每张表的主要视觉差异、共性问题和通用优化建议

## Previous Task Check

- Previous task:
  `doc/tasks/20260517-rough-wash-visual-fidelity-phase5/task.md`
- Status before this analysis: completed
- Impact: rough-wash 页眉比例已经收口，本次转向其余三张工序页的现状摸底和优化方向分析

## Milestones

- [x] M1: 创建任务包并确认上一任务已完成
- [x] M2: 抽取当前系统里三张目标表的真实生成截图
- [x] M3: 对照对应原始页图逐张分析差异
- [x] M4: 汇总共性问题与优先级建议
- [x] M5: 更新任务证据并完成收口

## Expected Verification

- `GET http://127.0.0.1:48081/v3/api-docs`
- `POST http://127.0.0.1:48081/admin-api/mes/pro/batch-record-report/recognize-fixed?routeKey=B`
- `GET http://127.0.0.1:48081/admin-api/mes/pro/batch-record-report/page?pageNo=1&pageSize=50&routeKey=B`
- artifacts under:
  `doc/tasks/20260517-other-three-process-record-compare-analysis/artifacts/`

## Current Status

Completed. Three live screenshots were captured and compared with the source-page
images. The current results indicate:

- 精洗: overall closest to the source, with only mild spacing and shading tweaks left
- 清洗: the most structurally heavy page, still needing more reusable table-structure handling
- 清洁: the page width and table proportions are still the main visible gap

## Conclusion

The next generic optimization should not be keyed by template name. It should
focus on reusable rules for:

- page-width budgeting and long-table fill behavior
- header-row detection and gray shading from cell/content shape instead of report name
- preserving repeated subheaders for multi-stage process tables
- line-weight hierarchy for outer border, section dividers, and grid lines

