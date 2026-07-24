# 执行日志：展厅产品一键讲解重启并观察推进（后端）

BDD: 新一轮任务应进入新的活动态 -> Given 后端旧卡死任务已清理并完成热修复 When 前端重新点击一键讲解 Then 状态接口必须返回新的 active/running/startedAt 或 currentProduct 信号
BDD: 新一轮任务应在观察窗口内出现真实推进 -> Given 新一轮任务已启动 When 持续轮询状态接口 Then 必须观察到当前产品、生成计数、剩余数量、失败信息或停止态中的至少一种真实变化
GREEN: authenticated start request -> PASS，`POST /admin-api/showroom/product/batch-generate-narration-script/start` 返回 `active=true`、`remainingCount=47`、`startedAt=1779427290332`
GREEN: progress observation -> PASS，30 秒内状态从 `currentProduct=product_125 / 无菌抽吸管路` 推进到 `currentProduct=product_126 / 斑马导丝`，同时 `remainingCount=47 -> 46`、`generatedLanguageCount=0 -> 1`
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-product-narration-restart-and-monitor --mode preview` -> PASS
