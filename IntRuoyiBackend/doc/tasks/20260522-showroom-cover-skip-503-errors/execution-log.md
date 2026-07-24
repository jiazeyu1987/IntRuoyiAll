# 执行日志：展厅一键封面遇到 503 错误直接跳过（后端）

BDD: 503 错误应直接跳过而不是续跑 -> Given 一键封面批任务中的某个产品生成时返回 OpenAI native `image_generation` `503 Service temporarily unavailable` When 批任务处理该产品 Then 该任务项必须直接标记为 `FAILED`，不再回到 `WAITING`
RED: 现状行为 -> FAIL，`503 Service temporarily unavailable` 当前会被当作可重试错误重新放回 `WAITING`
GREEN: `mvn -pl yudao-module-showroom -am clean "-Dtest=ShowroomProductCoverBatchTaskServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，新增回归已证明 503 错误会直接进入 `FAILED`
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-cover-skip-503-errors --mode preview` -> PASS
