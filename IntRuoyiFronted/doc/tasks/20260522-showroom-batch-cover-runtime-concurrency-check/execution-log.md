# 执行日志：展厅一键封面运行态并发验证（前端）

BDD: 真实页面可以触发一键封面任务 -> Given 产品管理页存在一键封面入口 When 用户从真实页面启动批量封面 Then 前端必须真实命中当前后端任务接口
BDD: 页面触发后应能配合后端验证 8 路并发运行 -> Given 一键封面任务已从真实页面启动 When 后端任务表进入运行态 Then 结合页面与任务表证据必须能判断本轮是否按最多 8 路并发运行
GREEN: real page request -> PASS，真实页面发出 `POST /admin-api/showroom/product/batch-generate-cover-image`
GREEN: real error surfacing -> PASS，前端未吞掉后端业务拒绝，原样暴露“任务 2 仍有 124 个产品待生成”
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-batch-cover-runtime-concurrency-check --mode preview` -> PASS
