# 20260709 工艺排产路线改为路线级共用配置执行日志

BDD: 路线级排产配置保存 -> Given 一条工艺排产路线关联多个产品且存在激活路线版本 / When 用户保存某道路线工序的排产配置 / Then 后端按 `routeVersionId + routeProcessId` 创建或更新唯一配置，不要求 `itemId`，且不按产品新增多条配置。

BDD: 历史产品维度配置冲突迁移 -> Given 同一路线版本同一工序存在多条产品维度排产配置 / When 执行迁移 SQL / Then 配置完全一致时归并为路线级配置，配置不一致时 fail fast 输出冲突，不自动选择默认值。

BDD: 排产路线页面展示路线级工序配置 -> Given 用户打开“工艺排产路线”页面并进入排产配置 / When 页面加载路线配置 / Then 配置表按路线工序展示一行，不显示产品选择、产品列或按产品拆分保存 payload。

GREEN: experience-preflight -> PASS，已读取 `docs/experience-index.md`、`docs/powershell-memory.md`、`docs/agent-memory/project-error-prevention.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，本任务不操作服务器、不执行真实 E2E 写入、不修改正式或测试环境数据。

RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteScheduleConfigServiceTest,MesProRouteVersionMapperTest" test` -> FAIL，旧测试和旧服务契约仍要求 `itemId`，并允许同一路线同工序按不同产品保存不同排产配置。

GREEN: 后端实现 -> PASS，`MesProRouteScheduleConfigServiceImpl` 改为按 `routeVersionId + routeProcessId` 保存和更新配置，保存时清空历史 `itemId`，不再校验路线关联产品。

GREEN: SQL 迁移契约 -> PASS，`mes_pro_route_schedule_config` 改为路线工序唯一索引；同路线版本同工序产品维度配置不一致时 `SIGNAL SQLSTATE '45000'`，无冲突时保留最小 id 行并软删重复行。

GREEN: 前端契约 -> PASS，`RouteUsePage.vue` 移除排产配置产品选择器、按 `itemId` 过滤和保存 payload 中的 `itemId`，页面提示“所有关联产品共用当前路线排产配置”。

GREEN: `python -X utf8 -m pytest script/tests/test_mes_route_schedule_config_shared_sql.py script/tests/test_mes_route_schedule_config_item_capacity_sql.py -q` -> PASS，6 tests。

GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteScheduleConfigServiceTest,MesProRouteVersionMapperTest" test` -> PASS，11 tests，0 failures。

GREEN: `node tests/e2e/mes-schedule-route-shared-config-static.spec.js` -> PASS。

BDD: 工艺排产路线前端入口统一 -> Given 用户从排产工单当前工序或工作台瓶颈入口查看路线排产配置 / When 页面跳转到路线配置 / Then 系统打开工艺流程编辑页的“排产配置” Tab，并按 `routeProcessId` 高亮、选中和滚动到目标路线工序，不再进入旧“工艺排产路线”独立页。

RED: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js`、`node tests/e2e/mes-process-use-route-tabs-static.spec.js` -> FAIL，旧静态契约仍读取已删除的 `route-use/RouteUsePage.vue`，并要求旧 `schedule-route` / `edhr-batch-route` 独立入口存在。

GREEN: frontend-unified-entry -> PASS，排产工单当前工序入口改为 `MesProRouteEdit`，query 携带 `tab=schedule-config` 与 `routeProcessId`；`RouteEditPage` / `RouteFormContent` / `RouteFlowConfigPanel` 支持目标工序透传、高亮、选中与滚动。

GREEN: workbench-bottleneck-route-entry -> PASS，`MesProSchedulerWorkbenchMapper.xml` 的瓶颈 `targetPath` 改为 `/mes/pro/route/edit/{routeId}?tab=schedule-config&routeProcessId={routeProcessId}`，Mapper XML 契约测试同步覆盖。

GREEN: preflight-issue-action-route-entry -> PASS，排产预检动作中路线本体维护改为 `MesProRoute` + `mes:pro-route:update`，排产配置/产能维护改为 `MesProRouteEdit` + `tab=schedule-config` + `routeProcessId` + `mes:pro-route:schedule-config:update`；前端 `openIssueAction` 对 `MesProRouteEdit` 将 `routeId` 转成 `params.id`，避免 query 路线编号无法匹配隐藏编辑路由。

GREEN: `node --check tests/e2e/mes-pro-schedule-order-pool-static.spec.js` + `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。

GREEN: `node --check tests/e2e/mes-route-flow-config-unification-static.spec.js` + `node tests/e2e/mes-route-flow-config-unification-static.spec.js` -> PASS。

GREEN: `node --check tests/e2e/mes-route-edit-page-static.spec.js` + `node tests/e2e/mes-route-edit-page-static.spec.js` -> PASS。

GREEN: `node --check tests/e2e/mes-process-use-route-tabs-static.spec.js` + `node tests/e2e/mes-process-use-route-tabs-static.spec.js` -> PASS。

GREEN: `python -X utf8 -m pytest script/tests/test_mes_route_flow_config_migration_sql.py -q` -> PASS，5 tests。

GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProSchedulerWorkbenchMapperXmlTest" test -q` -> PASS。

GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderPreflightServiceTest,MesProSchedulerWorkbenchMapperXmlTest" test -q` -> PASS。

GREEN: old-entry-source-guard -> PASS，`rg "MesProScheduleRoute|/mes/pro/schedule-route|src/views/mes/pro/schedule-route|route-use/RouteUsePage|route/useconfig" yudao-ui-admin-vue3/src ruoyi-vue-pro/yudao-module-mes/src/main ruoyi-vue-pro/yudao-module-mes/src/test -g "*.vue" -g "*.ts" -g "*.java"` -> NO_MATCH。

GREEN: frontend-test-assets-route-entry-cleanup -> PASS，更新 `mes-pro-schedule-order-pool-real-flow.e2e.js`、`mes-schedule-validation-boundary-static.spec.js`、`smart-scheduling-clickable-coverage-static.spec.js`，真实 E2E/静态覆盖均指向工艺流程编辑页或 `route/**` 边界，不再硬编码旧 `schedule-route` 入口。

GREEN: `node --check tests/e2e/mes-schedule-validation-boundary-static.spec.js` + `node tests/e2e/mes-schedule-validation-boundary-static.spec.js` -> PASS。

GREEN: `node --check tests/e2e/smart-scheduling-clickable-coverage-static.spec.js` + `node tests/e2e/smart-scheduling-clickable-coverage-static.spec.js` -> PASS。

GREEN: `node --check tests/e2e/mes-pro-schedule-order-pool-static.spec.js` + `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。

GREEN: `node --check tests/e2e/mes-route-flow-config-unification-static.spec.js` + `node tests/e2e/mes-route-flow-config-unification-static.spec.js` -> PASS。

GREEN: `node --check tests/e2e/mes-route-edit-page-static.spec.js` + `node tests/e2e/mes-route-edit-page-static.spec.js` -> PASS。

GREEN: `node --check tests/e2e/mes-process-use-route-tabs-static.spec.js` + `node tests/e2e/mes-process-use-route-tabs-static.spec.js` -> PASS。

GREEN: `node --check tests/e2e/mes-pro-schedule-order-pool-real-flow.e2e.js` -> PASS。

GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderPreflightServiceTest,MesProSchedulerWorkbenchMapperXmlTest" test -q` -> PASS。

GREEN: old-entry-source-and-test-guard -> PASS，旧排产入口搜索 `MesProScheduleRoute|/mes/pro/schedule-route|src/views/mes/pro/schedule-route|route-use/RouteUsePage|route/useconfig` 在前端 `src`、前端 `tests/e2e`、后端 `src/main`、后端 `src/test` 范围内 NO_MATCH。

GREEN: old-batch-route-source-and-test-guard -> PASS，旧批记录入口搜索 `/mes/pro/feedback/edhr-batch-route|src/views/mes/pro/edhr-batch-route|mes:pro-batch-record-route|MesProEdhrBatchRoute` 在前端 `src`、前端 `tests/e2e`、后端 `src/main`、后端 `src/test` 范围内 NO_MATCH。

RED: route-schedule-impact-clean-check -> FAIL，干净工作区复查发现 `MesProScheduleOrderServiceImplTest` 仍 stub 旧的 `selectByRouteVersionIdAndItemIdAndRouteProcessId`，`MesProRouteVersionMapperTest` 仍反射要求旧 mapper 方法，旧产品维度入口仍可能被后续代码误用。

GREEN: remove-legacy-item-schedule-config-mapper -> PASS，删除 `MesProRouteScheduleConfigMapper.selectByRouteVersionIdAndItemIdAndRouteProcessId`，将残留单测和 mapper 契约测试同步改为 `selectByRouteVersionIdAndRouteProcessId`。

GREEN: clean-impact-regression -> PASS，干净临时工作区中 `rg "selectByRouteVersionIdAndItemIdAndRouteProcessId\(" yudao-module-mes/src/main yudao-module-mes/src/test` 无匹配；`mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderAdmissionTest,MesProScheduleOrderAdmissionDiffServiceTest,MesProScheduleOrderServiceImplTest,MesProAutoScheduleServiceImplTest,MesProRouteScheduleConfigServiceTest,MesProRouteVersionMapperTest" test -q` -> PASS；SQL 契约 `python -X utf8 -m pytest script/tests/test_mes_route_schedule_config_shared_sql.py script/tests/test_mes_route_schedule_config_item_capacity_sql.py -q` -> PASS，6 tests。

RED: response-product-fields-contract -> FAIL，复查发现 `MesProRouteScheduleConfigRespVO` 和前端 `ProRouteScheduleConfigVO` 仍暴露 `itemId/itemCode/itemName/itemSpecification`，容易让后续页面继续把排产配置当成产品维度数据。

GREEN: response-product-fields-contract -> PASS，删除列表响应 VO 与前端排产配置类型中的产品字段，新增后端反射契约测试防止响应重新暴露产品维度；保存请求 `itemId` 仍作为历史兼容入参保留并在服务端清空。

GREEN: clean-contract-regression -> PASS，干净临时后端工作区中 `MesProRouteScheduleConfigRespVO` 产品字段搜索无匹配；`mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteScheduleConfigServiceTest,MesProRouteVersionMapperTest" test -q` -> PASS；SQL 契约 `python -X utf8 -m pytest script/tests/test_mes_route_schedule_config_shared_sql.py script/tests/test_mes_route_schedule_config_item_capacity_sql.py -q` -> PASS，6 tests；干净临时前端工作区 `node tests/e2e/mes-schedule-route-shared-config-static.spec.js` -> PASS。

GREEN: response-json-contract -> PASS，补充 `routeScheduleConfigResponseJson_shouldNotSerializeProductDimensionFields`，直接验证 `MesProRouteScheduleConfigRespVO` 序列化 JSON 只保留路线级字段，不包含 `itemId/itemCode/itemName/itemSpecification`；该 VO 同时是配置包 `scheduleConfigs` 的载体。

GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteScheduleConfigServiceTest,MesProRouteVersionMapperTest" test -q` -> PASS。

GREEN: route-schedule-source-guard -> PASS，`rg "selectByRouteVersionIdAndItemIdAndRouteProcessId|routeVersionIdAndItemId" yudao-module-mes/src/main/java yudao-module-mes/src/test/java` 无匹配；`MesProRouteScheduleConfigRespVO` 中 `itemId/itemCode/itemName/itemSpecification` 无匹配，仅测试断言中保留禁止序列化校验。

GREEN: frontend-real-e2e-route-level-contract -> PASS，`mes-schedule-route-production-factor-real-flow.e2e.js` 前置路线选择从 `scheduleConfigs.itemId === product.itemId` 改为启用排产工序存在对应 `routeProcessId` 路线级配置；干净临时前端工作区 `node --check tests/e2e/mes-schedule-route-production-factor-real-flow.e2e.js`、`node tests/e2e/mes-schedule-route-shared-config-static.spec.js`、`node tests/e2e/mes-schedule-route-production-factor-static.spec.js` 均 PASS，旧产品维度判断搜索无匹配。

BLOCKER: wider-route-schedule-regression -> `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteScheduleConfigServiceTest,MesProRouteVersionMapperTest,MesProRouteProcessControllerTest,MesProRouteProcessControllerWorkstationViewTest,MesProScheduleOrderAdmissionTest,MesProScheduleOrderAdmissionDiffServiceTest,MesProAutoScheduleServiceImplTest,MesProSchedulerWorkbenchRouteConfigPackageServiceTest" test` 当前被工作区另一组未提交 `RouteUse -> RouteFlow` 迁移改动阻断，testCompile 报 `MesProRouteUseConfigDO`、`MesProRouteUseProcessConfigDO`、`MesProRouteUseProcessBatchRecordDO`、`MesProRouteUseTypeEnum` 等旧类型缺失；本任务定向影响面验证已通过，未把该无关迁移纳入本次提交范围。

GREEN: `node tests/e2e/mes-schedule-route-production-factor-static.spec.js` -> PASS。

GREEN: evidence-validators -> PASS，backend/database/frontend evidence 均有效。

GREEN: closeout-preview -> PASS，blocked/warnings 均为 `<none>`，仅预览删除临时 evidence 文件。

RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteScheduleConfigServiceTest,MesProRouteVersionMapperTest,MesProRouteServiceImplTest,MesProRouteProcessControllerTest,MesProRouteProcessControllerWorkstationViewTest,MesProScheduleOrderAdmissionTest,MesProScheduleOrderAdmissionDiffServiceTest,MesProAutoScheduleServiceImplTest,MesProSchedulerWorkbenchRouteConfigPackageServiceTest" test` -> FAIL，影响面验证发现 `MesProScheduleOrderServiceImpl.buildScheduleConfigMap` 与 `MesProAutoScheduleServiceImpl.refreshScheduleOrderProcessesFromRouteConfig` 仍按 `itemId` 过滤配置，路线级 `itemId=null` 配置被漏读，生产工单准入报缺少路线排产策略配置。

GREEN: 影响面补修 -> PASS，`MesProScheduleOrderServiceImpl` 的生产工单创建、准入差异与当前配置解析改为按路线版本和路线工序读取配置；`MesProAutoScheduleServiceImpl` 自动排产刷新改为按 `routeProcessId` 映射路线级配置，不再按 `itemId` 过滤。

GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderAdmissionTest,MesProScheduleOrderAdmissionDiffServiceTest" test` -> PASS，12 tests，0 failures。

GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest" test` -> PASS，53 tests，0 failures。

GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteScheduleConfigServiceTest,MesProRouteVersionMapperTest" test` -> PASS，11 tests，0 failures。

GREEN: `python -X utf8 -m pytest script/tests/test_mes_route_schedule_config_shared_sql.py script/tests/test_mes_route_schedule_config_item_capacity_sql.py -q` -> PASS，6 tests。

GREEN: `node tests/e2e/mes-schedule-route-shared-config-static.spec.js` -> PASS。
