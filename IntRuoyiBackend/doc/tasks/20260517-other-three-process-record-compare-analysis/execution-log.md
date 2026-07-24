# Execution Log: 其余三张工序记录表对图分析

BDD: 精洗工序页应保持与原图接近的标题层级 -> Given 精洗工序生产记录的目标图具有完整页眉、标题灰条和相对紧凑的分区结构 When 系统生成 Route B 的精洗页 Then 结果应保持单页结构、标题层级和近似的灰底/边框层次，而不是把分区压平。

BDD: 清洗工序页应保留多段子表结构 -> Given 清洗工序生产记录的目标图包含多个连续工艺子段 When 系统生成 Route B 的清洗页 Then 页面应以通用方式保留子段标题、重复子表头和分段留白，而不是把多个工艺段视觉上压成一张平铺表。

BDD: 清洁工序页应更接近原图的横向比例 -> Given 清洁工序生产记录的目标图是更宽的横向布局 When 系统生成 Route B 的清洁页 Then 页面内容应该尽量填满表格宽度并保持合理列比，而不是只占用左半侧。

GREEN: live screenshot capture -> PASS, current Route B screenshots were captured for 精洗 / 清洗 / 清洁 and stored under `doc/tasks/20260517-other-three-process-record-compare-analysis/artifacts/`.

GREEN: comparative review -> PASS, the three current screenshots were compared against source pages `page-05.png`, `page-06.png`, and `page-08.png`, producing the following optimization priorities:

- 精洗: closest match; only minor spacing and shading adjustments remain
- 清洗: most structurally complex; needs reusable multi-stage table handling
- 清洁: width/proportion gap remains the largest visible issue

GREEN: generic recommendation -> PASS, the next optimization step should be template-agnostic and focus on:

- page-width budgeting and long-table fill behavior
- header-row detection and gray shading from cell/content shape instead of report name
- preservation of repeated subheaders for multi-stage process tables
- line-weight hierarchy for outer border, section dividers, and grid lines

BLOCKER: no production-code changes were made in this analysis round; it was a pure comparison and review task.
