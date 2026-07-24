# Execution Log: 工序主数据联动工艺路线

## 2026-07-08

- BDD: 同名不同编码工序允许保存 -> Given 已存在工序名称为“清洗”的工序 / When 新增另一个名称同为“清洗”但编码不同的工序 / Then 保存成功。
- BDD: 工序编码仍保持唯一 -> Given 已存在编码为 `PROC-CLEAN-001` 的工序 / When 新增或修改另一个工序为同编码 / Then 接口失败并提示工序编码已存在。
- BDD: 工艺路线展示最新工序主数据 -> Given 两条路线工序引用同一个 `processId` / When 工序主数据变更名称、工艺要求、状态和人工班次产能 / Then 两条路线详情接口均返回最新主数据字段，路线级配置不被覆盖。
- GREEN: experience-preflight -> PASS，已读取 PowerShell、项目经验索引、backend-api-delivery 和 closeout 门禁；本任务不涉及高风险服务器写入、真实 E2E 或数据库写入。
- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProProcessServiceImplTest,MesProRouteProcessControllerWorkstationViewTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，路线工序响应缺少 `processProductName/processAttention/processStatus/processManualShiftCapacity`，且同名工序保存测试先暴露名称唯一校验仍被调用。
- GREEN: `mvn.cmd -pl yudao-framework/yudao-common,yudao-framework/yudao-spring-boot-starter-mybatis -am "-DskipTests" install` -> PASS，刷新当前混合工作区已有 QuickFilter 框架本地依赖，未修改文件。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProProcessServiceImplTest,MesProRouteProcessControllerWorkstationViewTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，11 tests / 0 failures / 0 errors。
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260708-process-master-route-linkage/backend-api-evidence.md` -> PASS，后端 API 证据格式有效。
- BLOCKER: `pnpm ts:check` -> FAIL，默认 Node heap OOM；改用 `NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check` 后被既有 `src/views/mes/pro/scheduler-workbench/index.vue(634,3)` 的 `bottlenecks` 字段类型错误阻塞，非本任务 route-process 类型文件引入。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-process-master-route-linkage --mode preview` -> PASS，keep `task.md`、`execution-log.md`、`backend-api-evidence.md`，delete/blocked/warnings 均为 `<none>`。
