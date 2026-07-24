# 执行日志

BDD: 工艺路线工序返回结构化排产资源摘要 -> Given 工艺路线存在设备工序和人工工序 / When 前端调用现有路线工序列表接口 / Then 每道工序返回资源类型、标准资源、今日可用资源、标准班次产能、今日班次产能和状态原因。

BDD: 活跃维修设备降低今日可用设备与今日产能 -> Given 设备工序中一台设备存在维修中或待验收维修工单 / When 查询该工序排产资源 / Then 今日可用设备数扣除该设备，今日班次产能降低，并返回设备维修原因。

BDD: 工艺路线下维护人工单人产能 -> Given 人工工序已有工位 / When 保存资源时提交单人产能和人数 / Then 系统更新现有工位单人产能与工位人工人数，不新增重复资源表。

- PRECHECK: worktree -> PASS，后端工作区为 `D:\ProjectPackage\Int\IntRuoyi\worktrees\paichan_new\ruoyi-vue-pro`，分支 `codex/paichan_new`。
- PRECHECK: scope -> PASS，后端只扩展现有接口/服务，不新增重复订单池、维修模块、报工模块。
- RED: `mvn -pl yudao-module-mes "-Dtest=MesProRouteProcessControllerWorkstationViewTest,MesProRouteResourceServiceImplTest" test` -> FAIL，主代码缺少今日产能字段、设备维修查询 Mapper 方法、设备可用字段与人工单人产能保存字段。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProRouteProcessControllerWorkstationViewTest,MesProRouteResourceServiceImplTest" test` -> PASS，10 个用例通过，覆盖设备维修扣减今日产能、人工产能保存与既有资源列表契约。
- CHECK: schema -> PASS，本任务未新增或修改数据库 schema，复用现有工位、工位设备、工位人员、设备工序产能和维修工单表。
- GREEN: `mvn -pl yudao-server -am package "-DskipTests"` -> PASS，用于启动 `paichan_new` 后端真实 E2E 服务。
- GREEN: `http://127.0.0.1:48082/actuator/health` -> PASS，`paichan_new` 后端以本机真实 MySQL/Redis 参数启动成功。
- REGRESSION: 融入 `int_main` 后主目录后端 `48081` 健康检查 -> PASS，后端进程命令行指向 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-server\target\yudao-server.jar`，不是 worktree jar。
- REGRESSION: `MES_ROUTE_RESOURCE_E2E_BASE_URL=http://127.0.0.1:8085 node tests\e2e\mes-route-structured-scheduling-resource-real-flow.e2e.js` -> PASS，真实登录 `芋道源码/admin`，打开路线 `900026`，验证组成工序结构化资源列、设备详情弹窗和人工产能弹窗；本次只读验证，未修改 admin 数据。
