# 执行日志：展厅一键封面失败项诊断（前端）

BDD: 前端应暴露真实失败背景 -> Given 一键封面已有后台任务 When 用户再次点击一键封面 Then 前端必须原样提示“仍有待生成产品”，并允许后端失败项诊断继续定位真实原因
GREEN: real frontend error surfacing -> PASS，前端原样显示“任务 2 仍有 124 个产品待生成，请等待自动续跑完成后再重试”
INFO: backend diagnosis alignment -> 当前 `10` 个失败项根因统一为 `SHOWROOM_TARGET_NOT_FOUND: live product preview asset is required`
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-batch-cover-failed-items-diagnosis --mode preview` -> PASS
