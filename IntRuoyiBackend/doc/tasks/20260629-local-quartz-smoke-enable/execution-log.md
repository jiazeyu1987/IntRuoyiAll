BDD: local 默认加载 Quartz 且无需额外启动参数 -> Given 本地后端使用统一重启脚本以 local profile 启动 / When 排产冒烟调用 ERP 同步定时任务触发接口 / Then 本地运行态提供 Quartz Scheduler，不再返回 [定时任务 - 已禁用]。
BDD: local 自动任务按白名单收口 -> Given 本地后端启动后会同步 infra_job 到 Quartz / When local 自动任务收口器执行 / Then 非白名单 Quartz 自动任务会被暂停，且 DCC/展厅的 @Scheduled 本地默认不装配。

GREEN: experience-preflight -> PASS，已命中并读取 `docs/login-access.md` 与 `docs/powershell-memory.md`；真实 E2E 前置登录最小路径此前已对四个排产角色跑通。
RED: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\smart-scheduling-smoke-real-flow.e2e.js` -> FAIL，`/admin-api/infra/job/trigger` 返回 `{"success":false,"message":"[定时任务 - 已禁用][参考 https://doc.iocoder.cn/job/ 开启]","code":500}`。
RED: `rg -n "QuartzAutoConfiguration" D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-server\src\main\resources\application-local.yaml` -> FAIL，定位到 local profile 显式排除 Quartz 自动配置，当前统一本地重启脚本未覆盖该默认值。
GREEN: `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_restart_ruoyi_script.py -q` -> PASS，8 tests passed，已覆盖“local 默认启用 Quartz 且不再依赖 MES_SMOKE_BASE_URL”的脚本/配置契约。
GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-infra -Dtest=JobStartupSyncRunnerTest,LocalQuartzAutoPauseRunnerTest test` -> PASS，7 tests passed，已覆盖启动后 Quartz 同步与 local 自动暂停非白名单 Quartz 任务的门禁契约。
INFO: `docker exec ... SELECT id, name, handler_name, status, cron_expression FROM infra_job ORDER BY status, id` -> 当前本机启用态 Quartz 任务为 `kingdeeProductionOrderSyncJob`、`kingdeeBomSyncJob`、`mesEdhrWorkTaskOverdueJob`、`mesProNightlyReplanJob`；其余 Quartz 任务当前库中已为停用态。
INFO: `rg -n "@Scheduled|JobHandler"` -> 本机除 Quartz 外，还存在 DCC 与展厅的 `@Scheduled` 后台任务；本轮已把它们统一切到 `yudao.local-job-control.*` 显式配置开关，`application-local.yaml` 默认关闭。
