# 执行日志：展厅产品一键讲解长时间停留执行中排障（前端）

BDD: 后台任务已停止时页面不能继续显示执行中 -> Given 后端状态接口返回任务已非 running When 产品管理页轮询状态 Then 前端必须及时回落为“定时续跑中”或“已停止”，不得持续显示旧的执行中标签
BDD: 后台任务卡住时页面必须暴露真实当前产品或失败原因 -> Given 后端任务真实卡在某个产品或某次调用 When 页面读取状态 Then 前端必须展示当前执行产品或最近失败，不得只停留在无上下文的剩余数量
INFO: 现场对齐 -> 页面症状“执行中（剩171）”与后台状态 `active=true/running=true/remainingCount=171` 完全一致，说明问题不在前端伪渲染
INFO: 根因定位 -> 后端线程真实卡在 Codex CLI 子进程，前端只是在持续展示后端真实 running 态
GREEN: real login + status API -> PASS，修复部署并手工清理卡住任务后，`测试租户(122) / aoteman / admin123` 登录读取状态接口返回 `active=false`、`running=false`
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-product-narration-stuck-running-diagnosis --mode preview` -> PASS，closeout preview ready，无额外清理阻塞
