# 执行日志：展厅一键封面批量生成默认并发提升到 8（后端）

BDD: 待生成产品达到 8 个以上时最多并发 8 个 Codex CLI -> Given 批量封面任务存在至少 8 个待处理产品 When 后端进入本轮批量封面执行 Then 执行器必须以 8 个并发 worker 处理，不再沿用默认 3 个
BDD: 待生成产品少于 8 个时并发数等于待处理数量 -> Given 批量封面任务只剩 N 个待处理产品且 N 小于 8 When 后端进入本轮执行 Then 并发数必须等于 N，不得空转额外 worker
BDD: 非法并发配置继续 fail-fast -> Given Codex CLI 并发配置小于等于 0 When 后端解析批量封面并发 Then 必须显式失败，不得默默回退
RED: 源码与既有测试基线 -> FAIL，当前默认并发仍为 `3`，不满足“最多 8 个 Codex CLI 并发”的新需求
GREEN: `mvn -pl yudao-module-showroom -am "-Dmaven.test.skip=true" compile` -> PASS，生产代码在默认并发提升到 8 后编译通过
INFO: 先前不带 `clean` 的 Maven 增量编译曾误报 `ShowroomHttpApiIntegrationTest.java:1351`，复核后确认属于增量编译脏状态，不是本任务功能缺陷
GREEN: `mvn -pl yudao-module-showroom -am clean "-Dtest=ShowroomProductCoverBatchTaskServiceTest,ShowroomProductCoverImageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，封面批任务并发选择规则与默认并发上限回归全部通过
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-batch-cover-parallelism-8 --mode preview` -> PASS
