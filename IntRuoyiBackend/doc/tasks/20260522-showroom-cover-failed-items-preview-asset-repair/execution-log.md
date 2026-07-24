# 执行日志：展厅封面失败项补齐 live preview asset（后端）

BDD: 缺 preview asset 的失败项补齐后应恢复可续跑 -> Given 批量封面任务中存在因缺少 `live product preview asset` 而失败的产品 When 为这些产品补齐合法的 live preview asset 并恢复任务项状态 Then 下一轮后台续跑应不再因为同一前置缺失而立即失败
GREEN: preview asset baseline query -> PASS，确认共享预览文件 `infra_file.id = 2272` 可复用，且这批失败产品此前没有任何 `PRODUCT` live preview asset 记录
GREEN: preview asset repair -> PASS，一次性补齐 `124` 条缺失的 `PRODUCT` live preview asset `PUBLISHED` 记录
GREEN: task item recovery -> PASS，将 `10` 条因 preview asset 缺失而 `FAILED` 的项恢复为 `WAITING`，并清理同类 stale 错误背景
GREEN: repair verification -> PASS，修复后 task `id=2` 中 `missing preview asset count = 0`，`preview-asset error count = 0`
GREEN: scheduler consumption -> PASS，下一轮后台续跑后任务项分布变为 `FAILED=0 / RUNNING=8 / WAITING=115 / COMPLETED=1`
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-cover-failed-items-preview-asset-repair --mode preview` -> PASS
