# 执行日志：展厅一键语音 token 修复后复测（后端）

BDD: token 修复后一键语音应恢复推进 -> Given 阿里云 NLS token 已修复 When 重新触发一键语音或继续自动检查 Then 状态接口应体现失败停止累积并出现成功或剩余数量下降
BDD: 一键封面不应被语音 token 修复影响 -> Given 一键封面链路独立于语音 token When 复测一键语音 Then 封面任务状态应维持当前健康基线
INFO: token 修复后基线复查 -> 一键语音旧自动检查状态曾残留异常或空白 enabled 基线，但并未阻止新的批量请求
GREEN: authenticated retry request -> PASS，真实鉴权触发 `POST /admin-api/showroom/product/batch-generate-narration-audio` 返回 `succeededCount=17`、`failedCount=0`、`autoCheckEnabled=false`
GREEN: audio state recheck -> PASS，`GET /admin-api/showroom/product/batch-generate-narration-audio-state` 返回 `enabled=false`、`remainingActionableCount=0`、`lastFailureMessage=''`
GREEN: cover state recheck -> PASS，最近封面任务 `id=1` 仍为 `COMPLETED`、`failedCount=0`
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-batch-audio-token-fix-retry --mode preview` -> PASS
