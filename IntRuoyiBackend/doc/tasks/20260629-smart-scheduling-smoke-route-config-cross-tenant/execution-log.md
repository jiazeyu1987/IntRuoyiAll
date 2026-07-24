# Execution Log：排产路线配置包支持跨租户导入

BDD: 路线配置包允许跨租户导入 -> Given tenant 1 导出的 route-config 包 / When tenant 122 导入 / Then 系统按 routeCode 匹配目标路线，而不是要求 routeId 一致。
BDD: 工序用途与排产配置按目标路线工序映射 -> Given 包内 useConfig/scheduleConfig 绑定源 routeProcessId / When tenant 122 导入 / Then 系统按目标路线同 sort 或同 processCode 的 routeProcess 映射后保存。
BDD: 资源配置按目标工位和绑定关系映射 -> Given 包内 resource 绑定源 workstationId/workstationMachineId/workstationWorkerId / When tenant 122 导入 / Then 系统按目标租户工位编码与设备/人工绑定关系映射保存，不直接写入源主键。
RED: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProSchedulerWorkbenchRouteConfigPackageServiceTest" test -> FAIL, RouteConfigPackageService 仍直接依赖源主键，跨租户映射测试无法编译/通过。
GREEN: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProSchedulerWorkbenchRouteConfigPackageServiceTest" test -> PASS
GREEN: experience-preflight -> PASS，本机 `http://localhost:8081` 已分别用 `芋道源码/admin` 与 `测试租户/aoteman` 真实登录进入 `/mes/pro/scheduler-workbench`，后端 `http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`。
RED: node D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-smart-scheduling-smoke-route-config-cross-tenant\route-config-roundtrip-real.mjs -> FAIL, tenant 122 导入 route schedule config 时把源 id=108 带入 INSERT，触发 `Duplicate entry '108' for key 'mes_pro_route_schedule_config.PRIMARY'`。
GREEN: 根因定位 -> PASS，`MesProRouteScheduleConfigSaveReqVO.id` 经 `BeanUtils.toBean(reqVO, MesProRouteScheduleConfigDO.class)` 透传到插入对象，现已在 `saveConfig()` 强制 `config.setId(null)` 并补测试断言导入请求 `id == null`。
RED: node D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-smart-scheduling-smoke-route-config-cross-tenant\route-config-roundtrip-real.mjs -> FAIL, tenant 122 导入 `WORKER` 资源时抛 `Target workstationWorker binding not found for workstationId=922439`。
GREEN: 根因定位 -> PASS，导出包里的 `WORKER` 资源缺少 `postId`，无法跨租户按业务键匹配目标工位人员绑定；现已补 `postId` 出包并让导入在缺少目标绑定时交由正式保存层创建。
RED: node D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-smart-scheduling-smoke-route-config-cross-tenant\route-config-roundtrip-real.mjs -> FAIL, 芋道源码源数据存在 `workstationWorker.post_id = null` 的正式记录，tenant 122 对应工位无人工绑定时仍会卡在 `sourcePostId missing`。
GREEN: 根因定位 -> PASS，已将导入语义补齐为“源 `postId` 为空时，若目标工位无绑定则传 `workstationWorkerId=null, postId=null` 交由正式保存层创建空岗位绑定”。
RED: node D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-smart-scheduling-smoke-route-config-cross-tenant\route-config-roundtrip-real.mjs -> FAIL, tenant 122 存在 `Z4740/z4740` 双工序编码，`selectByCode()` 命中 2 条触发 `TooManyResultsException`。
GREEN: 根因定位 -> PASS，已将目标路线工序解析改为优先按目标路线 `sort` 命中，必要时再用编码候选列表筛目标路线工序，不再依赖租户内工序编码唯一。
