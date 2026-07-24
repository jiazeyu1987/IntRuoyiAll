# 执行日志：展厅一键封面运行态并发验证（后端）

BDD: 待处理数量不少于 8 时应最多并发 8 个 -> Given 一键封面任务命中至少 8 个待生成产品 When 真实任务表进入运行态 Then `RUNNING` 的任务项峰值应达到 8，且不超过 8
BDD: 待处理数量少于 8 时应按实际数量并发 -> Given 一键封面任务命中的待生成产品少于 8 个 When 真实任务表进入运行态 Then `RUNNING` 的任务项峰值应等于待处理产品数
INFO: runtime precondition -> 当前已发布且缺少封面的产品数为 `124`
GREEN: real frontend trigger -> PASS，真实页面发出 `POST /admin-api/showroom/product/batch-generate-cover-image`，后端原样返回“任务 2 仍有 124 个产品待生成”，说明前端入口命中真实接口且后端活动任务闸门正常
GREEN: runtime concurrency observation -> PASS，任务 `id=2` 处于 `RUNNING` 状态，`showroom_product_cover_batch_task_item` 在 4 次轮询（30 秒）中持续保持 `RUNNING=8 / WAITING=106 / FAILED=10 / COMPLETED=0`
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-batch-cover-runtime-concurrency-check --mode preview` -> PASS
