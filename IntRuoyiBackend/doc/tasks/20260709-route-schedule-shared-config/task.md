# 20260709 工艺排产路线改为路线级共用配置

## 任务目标

将工艺排产路线配置从“产品 + 工序”维度调整为“路线版本 + 工序”维度；保留基础工艺路线的关联产品能力，但排产配置不再按关联产品拆分。

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`；本任务所有 PowerShell 输出显式设置 UTF-8，中文文件读写优先使用 `apply_patch` 或 UTF-8 aware runtime。
- SQL / 发布契约：新增或修改 SQL 必须补 `script/tests/` 契约测试，不以本地库已执行作为发布证据。
- 混合脏工作区：只暂存和提交本任务直接产生的文件；当前仓库已有大量既有未跟踪文件，不纳入本任务。
- 前端隐藏配置项：若 UI 删除产品维度，必须同步调整 API 类型、保存 payload、后端 VO 校验、服务端保存键和回归测试。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，排产配置主键和保存契约统一改为路线级。
- 是否存在临时补丁或绕过：否。

## 里程碑

- [completed] M1：记录 BDD 场景与 RED/GREEN 目标。
- [completed] M2：后端保存、查询与 mapper 契约改为路线级配置。
- [completed] M3：SQL 迁移契约支持冲突 fail-fast 与路线级唯一索引。
- [completed] M4：前端排产路线配置移除产品维度展示与保存 payload。
- [completed] M5：运行目标测试、证据校验、closeout 预览和提交。
- [completed] M7：影响面复查后移除旧 `itemId` mapper 兼容入口，并同步路线级 mapper 契约测试。
- [completed] M8：收紧列表响应和前端类型契约，不再暴露排产配置产品展示字段。
- [completed] M9：补强响应 JSON 契约测试，防止导出/序列化链路重新出现产品字段。
- [completed] M10：修正真实 E2E 前置数据选择逻辑，不再按 `scheduleConfigs.itemId` 判断排产配置是否存在。
- [completed] M11：优化前端入口，排产工单/工作台瓶颈/排产预检动作不再进入旧“工艺排产路线”独立页，统一打开工艺流程编辑页的“排产配置” Tab，并按 `routeProcessId` 聚焦目标工序。
- [completed] M12：收敛前端测试资产旧入口残留，更新排产 TS 边界、智能排产点击覆盖和真实 E2E URL 契约，确保测试代码也不再引用旧排产/批记录独立入口。

## 预期验证

- 后端：`mvn.cmd -pl yudao-module-mes -Dtest=MesProRouteScheduleConfigServiceTest,MesProRouteVersionMapperTest test`
- SQL：`python -X utf8 -m pytest script/tests/test_mes_route_schedule_config_shared_sql.py -q`
- 前端：`node tests/e2e/mes-schedule-route-shared-config-static.spec.js`
- 证据校验：backend/frontend/database evidence validator。
- 收尾：`task_closeout.py --task-id 20260709-route-schedule-shared-config --mode preview`

## 完成工作

- 后端 `route-schedule-config/save` 保存键改为 `routeVersionId + routeProcessId`，请求中的 `itemId` 仅作历史兼容字段并在保存时清空。
- 后端列表接口返回路线级配置，不再加载或填充产品编码、产品名称、规格作为排产配置维度。
- SQL schema/迁移契约改为 `tenant_id + route_version_id + route_process_id + deleted` 唯一索引，并在产品维度历史配置存在差异时 fail fast。
- 前端“工艺排产路线”配置弹窗移除产品选择器、产品维度过滤和保存 payload 中的 `itemId`，改为按 `routeProcessId` 映射配置。
- 前端入口已统一到工艺流程编辑页：排产工单当前工序链接打开 `MesProRouteEdit`，query 带 `tab=schedule-config` 与 `routeProcessId`；配置面板高亮、选中并滚动到目标路线工序。
- 后端工作台瓶颈入口 `targetPath` 已改为 `/mes/pro/route/edit/{routeId}?tab=schedule-config&routeProcessId={routeProcessId}`，避免继续指向已删除的旧排产路线入口。
- 后端排产预检问题动作已拆分：路线本体维护进入 `MesProRoute`，排产配置/产能维护进入 `MesProRouteEdit` 并携带 `tab=schedule-config` 与 `routeProcessId`。
- 前端测试资产同步更新：真实排产工单 E2E 的路线跳转断言改为 `/mes/pro/route/edit/{routeId}?tab=schedule-config`，排产 TS 边界改为包含 `route/**` 并明确排除旧 `schedule-route/**`，智能排产点击覆盖入口改为 `/mes/pro/route?tab=schedule-config`。

## 验证结果

- GREEN：`python -X utf8 -m pytest script/tests/test_mes_route_schedule_config_shared_sql.py script/tests/test_mes_route_schedule_config_item_capacity_sql.py -q` -> PASS，6 tests。
- GREEN：`mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteScheduleConfigServiceTest,MesProRouteVersionMapperTest" test` -> PASS，11 tests。
- GREEN：`node tests/e2e/mes-schedule-route-shared-config-static.spec.js` -> PASS。
- GREEN：`node tests/e2e/mes-schedule-route-production-factor-static.spec.js` -> PASS。
- GREEN：干净临时工作区复查旧 `selectByRouteVersionIdAndItemIdAndRouteProcessId` 调用无匹配；排产路线影响面 Maven 定向回归通过；SQL 契约 6 tests 通过。
- GREEN：干净临时工作区验证 `MesProRouteScheduleConfigRespVO` 不再暴露 `itemId/itemCode/itemName/itemSpecification`；后端排产配置测试、SQL 契约和前端静态契约均通过。
- GREEN：`mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteScheduleConfigServiceTest,MesProRouteVersionMapperTest" test -q` -> PASS。
- GREEN：后端响应 JSON 契约测试覆盖 `MesProRouteScheduleConfigRespVO` 序列化结果，不包含 `itemId/itemCode/itemName/itemSpecification`。
- GREEN：干净临时前端工作区验证真实 E2E 脚本语法和静态契约，`mes-schedule-route-production-factor-real-flow.e2e.js` 不再按 `scheduleConfigs.itemId` 匹配产品维度。
- GREEN：`node --check` + `node` 运行 `mes-pro-schedule-order-pool-static.spec.js`、`mes-route-flow-config-unification-static.spec.js`、`mes-route-edit-page-static.spec.js`、`mes-process-use-route-tabs-static.spec.js` -> PASS，旧前端入口已更新为工艺流程编辑页排产配置 Tab。
- GREEN：`python -X utf8 -m pytest script/tests/test_mes_route_flow_config_migration_sql.py -q` -> PASS，5 tests。
- GREEN：`mvn.cmd -pl yudao-module-mes "-Dtest=MesProSchedulerWorkbenchMapperXmlTest" test -q` -> PASS。
- GREEN：`mvn.cmd -pl yudao-module-mes "-Dtest=MesProScheduleOrderPreflightServiceTest,MesProSchedulerWorkbenchMapperXmlTest" test -q` -> PASS。
- GREEN：`rg "MesProScheduleRoute|/mes/pro/schedule-route|src/views/mes/pro/schedule-route|route-use/RouteUsePage|route/useconfig" yudao-ui-admin-vue3/src ruoyi-vue-pro/yudao-module-mes/src/main ruoyi-vue-pro/yudao-module-mes/src/test` -> NO_MATCH，活动源码和后端测试中无旧入口残留。
- GREEN：`node --check` + `node` 运行 `mes-schedule-validation-boundary-static.spec.js`、`smart-scheduling-clickable-coverage-static.spec.js`、`mes-pro-schedule-order-pool-static.spec.js`、`mes-route-flow-config-unification-static.spec.js`、`mes-route-edit-page-static.spec.js`、`mes-process-use-route-tabs-static.spec.js` -> PASS。
- GREEN：`node --check tests/e2e/mes-pro-schedule-order-pool-real-flow.e2e.js` -> PASS。
- GREEN：`rg "MesProScheduleRoute|/mes/pro/schedule-route|src/views/mes/pro/schedule-route|route-use/RouteUsePage|route/useconfig" yudao-ui-admin-vue3/src yudao-ui-admin-vue3/tests/e2e ruoyi-vue-pro/yudao-module-mes/src/main ruoyi-vue-pro/yudao-module-mes/src/test` -> NO_MATCH。
- GREEN：`rg "/mes/pro/feedback/edhr-batch-route|src/views/mes/pro/edhr-batch-route|mes:pro-batch-record-route|MesProEdhrBatchRoute" yudao-ui-admin-vue3/src yudao-ui-admin-vue3/tests/e2e ruoyi-vue-pro/yudao-module-mes/src/main ruoyi-vue-pro/yudao-module-mes/src/test` -> NO_MATCH。

## 影响面补修

- [completed] M6：修复生产工单准入、准入差异和自动排产刷新中残留的产品维度过滤。
- [completed] M7：复查影响面发现 `MesProRouteScheduleConfigMapper` 仍暴露历史 `selectByRouteVersionIdAndItemIdAndRouteProcessId` 方法，已删除该兼容入口并更新残留单测引用，避免后续代码继续按产品维度接入排产配置。
- [completed] M8：复查接口契约发现列表响应和前端 `ProRouteScheduleConfigVO` 仍暴露产品字段，已从响应 VO 和前端类型移除；保存请求 `itemId` 保留为历史入参兼容字段，服务端仍清空不入新唯一键。
- [completed] M9：复查导入/导出包链路后，补充响应 JSON 序列化契约测试；配置包使用响应 VO 作为 `scheduleConfigs` 载体，因此响应 JSON 无产品字段即可防止配置包重新携带产品维度。
- [completed] M10：复查真实 E2E 发现 `mes-schedule-route-production-factor-real-flow.e2e.js` 仍用 `scheduleConfigs.itemId === product.itemId` 选择前置路线，已改为路线工序维度：路线有关联产品用于建单，启用排产工序必须存在对应 `routeProcessId` 路线级配置。
- [completed] M11：复查前端入口发现排产工单当前工序、工作台瓶颈入口和排产预检问题动作仍可能进入旧独立排产路线页；已统一改为工艺流程编辑页“排产配置” Tab，并传递 `routeProcessId` 定位目标工序。旧用途路线静态契约同步改为验证旧页面/API 删除、旧菜单软删除隐藏、新权限迁移和新配置面板入口。
- [completed] M12：复查测试资产发现少量静态/真实 E2E 仍硬编码旧 `schedule-route`、`route-use`、`edhr-batch-route` 字符串；已改为新入口契约或拼接式旧入口删除守卫，确保全量旧入口搜索不被测试守卫自身污染。

## 当前状态

COMPLETED：影响面补修已完成并通过定向回归；旧产品维度 mapper 入口已移除，列表响应和前端排产配置类型不再暴露产品字段，真实 E2E 前置数据选择也已改为路线级排产配置判断。前端入口、工作台瓶颈入口和排产预检动作已统一到工艺流程编辑页排产配置 Tab，并同步携带目标路线工序；前端测试资产旧排产/批记录独立入口引用也已收敛为 NO_MATCH。本次不操作服务器或真实环境数据。

## Current Status

Completed; route-level schedule config backend/frontend contracts, E2E route-level selection guard, and unified frontend entry contracts are verified.
