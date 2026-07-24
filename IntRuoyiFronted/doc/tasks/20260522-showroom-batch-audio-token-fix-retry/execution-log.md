# 执行日志：展厅一键语音 token 修复后复测（前端）

BDD: token 修复后重试一键语音应恢复推进 -> Given 用户已修复阿里云 NLS token 过期问题 When 重新触发一键语音或观察自动检查 Then 前端承接的后端状态应体现失败停止累积并出现真实推进
BDD: 一键封面不应被语音 token 修复影响 -> Given 一键封面与一键语音链路独立 When 复测一键语音 Then 一键封面的任务状态不应退化
INFO: token 修复后基线复查 -> 一键语音旧自动检查状态曾残留异常或空白 enabled 基线，但不影响新的真实批量请求
GREEN: authenticated retry request -> PASS，真实鉴权后触发 `POST /admin-api/showroom/product/batch-generate-narration-audio` 返回 `succeededCount=17`、`failedCount=0`、`autoCheckEnabled=false`
GREEN: audio state recheck -> PASS，重试后 `GET /admin-api/showroom/product/batch-generate-narration-audio-state` 返回 `enabled=false`、`remainingActionableCount=0`
GREEN: cover state recheck -> PASS，最近封面任务仍为 `COMPLETED`、`failedCount=0`
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-batch-audio-token-fix-retry --mode preview` -> PASS
