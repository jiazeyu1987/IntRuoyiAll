# 执行日志：展厅产品一键语音与一键封面健康检查（后端）

BDD: 一键语音应能从状态接口判断健康度 -> Given 一键语音具备自动检查状态持久化 When 独立读取当前状态 Then 必须能判断 enabled、剩余待处理、失败信息与最近执行时间是否合理
BDD: 一键封面应能从任务表判断健康度 -> Given 一键封面使用批任务表持久化 When 独立读取当前任务记录 Then 必须能判断是否存在卡死中的活动任务、反复失败或未收口的异常状态
INFO: 一键语音状态接口 -> `GET /admin-api/showroom/product/batch-generate-narration-audio-state` 返回 `enabled=true`、`matchedCount=180`、`publishedCount=54`、`failedCount=40`、`remainingActionableCount=40`、最近失败为 `aliyun_nls_tts_failed status=400 ... status=40000001`
INFO: 一键语音日志 -> `output/runtime/backend-ebr-20260522-131241.out.log` 在 `2026-05-22 13:26:28` 至 `13:30:44` 多次写入同类 `aliyun_nls_tts_failed status=40000001` 失败，说明问题正在重复发生
INFO: 一键封面任务表 -> 当前无 `WAITING/RUNNING` 活动任务；最近任务 `id=1` 为 `COMPLETED`，`succeededCount=8`、`failedCount=0`
INFO: 一键封面 item 明细 -> 最近任务前 8 条 item 均为 `COMPLETED`，`lastError=null`
GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductCoverBatchTaskServiceTest,ShowroomApiRuntimeBatchCoverModeTest,ShowroomProductNarrationRegressionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-batch-audio-cover-health-check --mode preview` -> PASS
