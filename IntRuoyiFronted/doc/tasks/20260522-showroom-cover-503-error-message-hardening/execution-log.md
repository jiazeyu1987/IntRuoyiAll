# 执行日志：展厅封面 503 错误信息收敛（前端）

BDD: 前端应看到更干净的真实 503 背景 -> Given 后端已经把封面 503 错误信息收敛 When 前端再次展示封面失败提示 Then 用户应直接看到真实上游失败原因，而不是 `Illegal char` 噪音
GREEN: backend alignment -> PASS，后端已新增定向测试锁定“错误文本不再被当作路径解析”
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-cover-503-error-message-hardening --mode preview` -> PASS
