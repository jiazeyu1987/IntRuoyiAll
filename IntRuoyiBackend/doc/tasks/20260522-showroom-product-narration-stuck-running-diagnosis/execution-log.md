# 执行日志：展厅产品一键讲解长时间停留执行中排障（后端）

BDD: 单次执行线程已退出时状态不能永久保持 running -> Given 一键讲解任务线程已结束或异常退出 When 再次读取任务状态 Then 后端必须回落 running 状态或显式暴露失败，不能无限保持执行中
BDD: 后台任务卡住时必须暴露真实上下文 -> Given 一键讲解任务阻塞在某个产品或外部调用 When 读取状态或日志 Then 系统必须能定位当前产品、最近失败或锁占用来源，便于用户判断是否需要重试或停止
INFO: 现场复现 -> `infra_config` 中 `showroom.product.batch-narration-script.*` 自 `2026-05-22 11:30:00` 起持续保持 `active=true`、`running=true`、`lastRunAt=null`、`remainingCount=171`，与页面“执行中（剩171）”一致
INFO: JVM thread dump -> `scheduled-thread-jm-1` 卡在 `CodexCliChatModel.executePrompt -> ShowroomProductNarrationCodexService.generateScript -> ShowroomApiRuntime.runProductNarrationScriptBatch`
RED: `mvn -pl yudao-module-ai "-Dtest=CodexCliChatModelTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增回归 `callShouldInvokeCliWithEphemeralFlag` 报 `Local codex cli failed with exit code 17, stdout: missing-ephemeral`
GREEN: `mvn -pl yudao-module-ai "-Dtest=CodexCliChatModelTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`CodexCliChatModel` 现在会把 `--ephemeral` 传给 `codex exec`
GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomProductNarrationRegressionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，展厅批讲解回归未被破坏
GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，新的 `yudao-server.jar` 已重新打包
INFO: 运行态处置 -> 停掉旧 `48081` Java 进程树，手工把 `showroom.product.batch-narration-script.active/running` 改为 `false`，并写入 `SYSTEM_RESET` 失败原因，避免前端继续显示旧执行中
GREEN: real login + status API -> PASS，`测试租户(122) / aoteman / admin123` 登录后读取 `/admin-api/showroom/product/batch-generate-narration-script/status`，返回 `active=false`、`running=false`、`currentProduct=null`
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-product-narration-stuck-running-diagnosis --mode preview` -> PASS，closeout preview ready，无额外清理阻塞
