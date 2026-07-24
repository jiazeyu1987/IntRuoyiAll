# 工艺路线结构化排产资源实现（后端）

## 任务目标

在 `paichan_new` 后端 worktree 中实现 `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260609-route-structured-scheduling-resource-requirements\requirements.md` 中与后端相关的第一期能力：复用现有生产工单、工艺路线、资源大表、设备维修和排程日历数据，扩展现有工艺路线工序接口与资源保存接口，让工艺路线详情可以按工序展示标准资源、今日可用资源、班次产能、维修影响和资源状态。

## Previous Task Check

- 前序同仓库相关任务：`doc/tasks/20260608-route-process-shift-capacity-display/task.md`。
- 检查结果：该任务在当前 `int_main` 基线已提交，当前 worktree 从该提交创建；本任务在其班次产能字段基础上继续扩展，不覆盖其成果。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺少必要产能、工位、设备主数据、冲突产能时继续 fail fast 或显式返回未配置状态，不静默成功。
- `是否从根因和长期维护角度解决`：是。长期标准资源仍写入现有工位/工位设备/工位人工/设备工序产能；当天维修影响来自现有维修工单状态，不写入工艺路线主数据。
- `是否存在临时补丁或绕过`：否。不新增独立订单池、维修模块、报工模块或重复排产系统。

## BDD 场景

- BDD: 工艺路线工序返回结构化排产资源摘要 -> Given 工艺路线存在设备工序和人工工序 / When 前端调用现有路线工序列表接口 / Then 每道工序返回资源类型、标准资源、今日可用资源、标准班次产能、今日班次产能和状态原因。
- BDD: 活跃维修设备降低今日可用设备与今日产能 -> Given 设备工序中一台设备存在维修中或待验收维修工单 / When 查询该工序排产资源 / Then 今日可用设备数扣除该设备，今日班次产能降低，并返回设备维修原因。
- BDD: 工艺路线下维护人工单人产能 -> Given 人工工序已有工位 / When 保存资源时提交单人产能和人数 / Then 系统更新现有工位单人产能与工位人工人数，不新增重复资源表。

## 里程碑

- [x] M1：补充后端 RED 测试覆盖结构化资源字段、维修影响、人工单人产能保存。
- [x] M2：扩展 VO、服务与查询逻辑并通过 GREEN。
- [x] M3：扩展资源保存接口支持单人产能维护。
- [x] M4：补充排程日历分析必要字段或确认复用现有字段。
- [x] M5：运行后端目标测试和证据验证。

## 预期验证

- `mvn -pl yudao-module-mes -Dtest=MesProRouteProcessControllerWorkstationViewTest,MesProRouteResourceServiceImplTest test`
- 后端证据文件通过 `backend-api-delivery` 校验。
- 如涉及 schema，数据库证据文件通过 `database-schema-delivery` 校验；若不新增 schema，记录“不新增 schema”的证据。

## 当前状态

completed

## 完成记录

- 已扩展 `/mes/pro/route-process/list-by-route` 返回标准资源、今日可用资源、标准/今日小时产能、标准/今日班次产能、资源状态和原因。
- 已将维修中、待验收维修工单纳入今日设备可用性计算；长期标准产能不受当天维修影响。
- 已扩展资源保存请求，使人工资源保存可以同步更新工位 `singleStandardHourlyCapacity`，仍写入现有工位主数据。
- 未新增数据库 schema；本期复用现有工位、工位设备、工位人员、设备工序产能和维修工单表。

## 最终验证

- `mvn -pl yudao-module-mes "-Dtest=MesProRouteProcessControllerWorkstationViewTest,MesProRouteResourceServiceImplTest" test` -> PASS，10 个用例通过。
- `mvn -pl yudao-server -am package "-DskipTests"` -> PASS，用于启动 `paichan_new` 后端真实 E2E 服务。
- 融入 `int_main` 后，主目录后端 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-server\target\yudao-server.jar` 以 `48081` 启动，`http://127.0.0.1:48081/actuator/health` -> PASS。
- 融入 `int_main` 后，配合主目录前端 `8085` 执行 `MES_ROUTE_RESOURCE_E2E_BASE_URL=http://127.0.0.1:8085 node tests\e2e\mes-route-structured-scheduling-resource-real-flow.e2e.js` -> PASS。
