# 执行日志：展厅一键封面失败项诊断（后端）

BDD: 失败项诊断必须能定位到具体产品和具体错误 -> Given 批量封面任务存在失败项 When 读取任务项明细与运行日志 Then 必须能给出失败产品列表、错误原因和是否可重试判断
GREEN: failed item query -> PASS，任务 `id=2` 共查到 `10` 条 `FAILED` item
INFO: failed item root cause -> 10 条失败项 `lastError` 全部一致：`SHOWROOM_TARGET_NOT_FOUND: live product preview asset is required`
INFO: retryability assessment -> 该类失败属于业务前置缺失，不是瞬时网络/并发故障；在补齐 live preview asset 前继续续跑不会自动成功
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-batch-cover-failed-items-diagnosis --mode preview` -> PASS
