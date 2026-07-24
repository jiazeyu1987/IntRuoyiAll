# 执行日志：智能排产四条路线默认值补齐 后端实现

BDD: 后端创建或补齐排产配置时不再写死数值 -> Given 路线工序缺少 SCHEDULE 用途配置或排产策略 / When 系统执行正式补齐逻辑 / Then 读取值必须来自正式配置存储或显式请求数据，不得再写死小时产能、班次小时或人工数量。

BDD: 四条目标路线补齐后通过路线前置 -> Given 当前 4 条目标路线存在缺失用途配置、排产策略或人工资源 / When 后端按正式默认值完成补齐 / Then 当前入池/预检不再因为缺排产策略或缺用途配置而阻断。

GREEN: previous-task-blocked -> PASS，上一后端任务 `20260628-srm-nas-locator` 已标记为 `BLOCKED`。
GREEN: backend-scope-discovery -> PASS，已确认隐藏默认值主要位于 `MesProRouteServiceImpl` 自动建配置逻辑与工作台策略配置读取逻辑之间。
RED: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProRouteServiceImplTest,MesProSchedulerWorkbenchServiceImplTest,MesProRouteScheduleConfigServiceTest" test` -> FAIL，当前后端尚未支持工作台正式默认排产字段，也仍会写死自动排产默认值。
GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProRouteServiceImplTest,MesProSchedulerWorkbenchServiceImplTest,MesProRouteScheduleConfigServiceTest" test` -> PASS，工作台正式默认排产字段、自动补齐读取和夜班日历规则解析均已通过定向单测。
GREEN: tenant1-write-authorization -> PASS，用户已明确授权通过现有 MES 正式接口写入 `tenant_id=1` 的目标路线排产数据。
GREEN: experience-preflight -> PASS，已在后端真实写入前完成 `tenant_id=1` 授权登记、登录门禁核对和目标控制器契约核对。
GREEN: tenant1-edhr-route-scope-save -> PASS，`admin` 已通过正式权限作用域接口取得 4 条目标路线的 `ROUTE_EDIT` 对象级权限。
GREEN: tenant1-policy-settings-save -> PASS，工作台正式默认值已写入 `infra_config.config_key='mes.scheduler-workbench.policy-settings'`。
GREEN: tenant1-route-use-config-save -> PASS，`route_id=900022` 的 `21` 条与 `route_id=900025` 的 `24` 条 `SCHEDULE` 工序用途配置已补齐；`route_id=900021/900026` 复核时已处于启用状态。
GREEN: tenant1-route-schedule-config-save -> PASS，`route_version_id=1/2/3/4` 分别存在 `30/21/24/26` 条排产配置，均为 `FINITE_HOURLY + hourly_capacity=30 + night_shift_enabled=0`。
GREEN: tenant1-worker-defaults-save -> PASS，目标路线关联工位 `single_standard_hourly_capacity` 缺口为 `0`。
