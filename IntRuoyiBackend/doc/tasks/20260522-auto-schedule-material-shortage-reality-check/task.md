# 任务：自动排产物料短缺真实性核对

## Goal

核对当前自动排产预览中剩余的 `54` 条 `MATERIAL` warning，判断它们是：

- 真实库存为 `0 / 不足`
- 还是库存主数据未同步 / 缺失导致的假性短缺

并输出可追溯的事实结论，便于后续决定是补库存数据还是继续修系统逻辑。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-auto-schedule-material-shortage-reality-check\**`
- 本地运行态真实数据：
  - `POST /admin-api/mes/pro/auto-schedule/preview`
  - `mes_wm_material_stock`
  - 与工单 `903200 / 903245` 对应的工单物料需求

## Non-Scope

- 不修改仓库生产代码。
- 不直接补库存数据。
- 不调整工单 BOM / 物料需求算法。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-balloon-forming-workstation-data-fix\task.md`
- Status before this task: `In progress`
- Impact: 上一任务已完成路线工作站产线绑定补数并消除 `LINE` 阻塞；本次继续分析剩余 `MATERIAL` warning，不冲突。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务只新增任务文档和分析证据，不覆盖无关改动。

## Milestones

1. 建立任务文档并重放当前预览请求。
2. 提取剩余 `MATERIAL` warning 的物料清单与需求/可用数量。
3. 核对库存表现有正库存记录，判断这些 warning 是否来自真实零库存。
4. 输出结论并给出下一步建议。

## Expected Verification

- 真实接口与真实数据库只读核对
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-auto-schedule-material-shortage-reality-check --mode preview`

## Current Status

Completed.

## Completed Work

- 已重放当前自动排产预览请求，确认路线阻塞已消失，仅剩 `54` 条 `MATERIAL` warning。
- 已提取 warning 明细，确认这些 warning 基本表现为“两个工单共享同一批叶子物料需求，每条物料在两个工单上各报一次”。
- 已直接查询库存表 `mes_wm_material_stock`，确认当前系统中仅有 `1` 个 `item_id=900200` 存在正库存 `10`。
- 已核对当前 warning 涉及的物料，其 `availableQty` 全部为 `0`，与库存表“几乎无正库存记录”的现状一致。

## Verification Result

- PASS: 当前预览结果 `blockingIssueCount = 0`、`shortageCount = 54`
- PASS: 54 条 warning 中，所有展示行的 `availableQty = 0`
- PASS: `mes_wm_material_stock` 当前仅 1 个物料存在正库存，且并不在本次短缺清单中

## Conclusion

- 这批 `54` 条 warning 不是“算法把工序 BOM 算错了”的问题。
- 从当前本地真实库存表看，它们更接近“库存数据确实缺失/为 0”而不是“排产逻辑误报”。
- 更准确地说：系统当前几乎没有可用库存数据，因此这些短缺提示在现状下是真实结果。

## Next Suggestion

- 若业务上这些物料不应为 0，应优先排查库存同步/库存导入链路，而不是继续修改排产逻辑。
- 若你愿意，我下一步可以继续把这 `54` 条按“唯一物料编码”去重成一张清单，并标出哪些是包材、哪些是导管主材，方便你们找仓储/ERP 同步负责人。
