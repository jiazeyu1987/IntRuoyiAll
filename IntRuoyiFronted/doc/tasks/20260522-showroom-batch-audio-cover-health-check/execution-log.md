# 执行日志：展厅产品一键语音与一键封面健康检查（前端）

BDD: 一键语音当前状态必须可解释 -> Given 产品管理页存在一键语音入口 When 独立读取后端状态与现有页面契约 Then 必须能判断当前是否仍有自动检查、剩余待处理和失败上下文
BDD: 一键封面当前状态必须可解释 -> Given 产品管理页存在一键封面入口 When 独立读取后端批任务数据与现有页面契约 Then 必须能判断当前是否存在活动任务、失败任务或明显运行异常
GREEN: `node --test scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-batch-cover-auto-resume.test.mjs scripts/showroom-admin-batch-cover-mode.test.mjs` -> PASS，前端一键语音/一键封面状态承接与结果展示源码契约全部通过
INFO: 一键语音运行态 -> 后端状态接口返回 `enabled=true`、`failedCount=40+`、`remainingActionableCount=40+`、最近失败为 `SHOWROOM_AUDIO_GENERATION_FAILED: aliyun_nls_tts_failed status=400`
INFO: 一键封面运行态 -> 当前无 `WAITING/RUNNING` 活动任务；最近任务 `id=1` 已 `COMPLETED`，`failedCount=0`
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-batch-audio-cover-health-check --mode preview` -> PASS
