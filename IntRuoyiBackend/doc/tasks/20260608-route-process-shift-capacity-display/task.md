# 工艺路线工序班次产能接口扩展

## 任务目标

扩展 MES 工艺路线工序接口 `/mes/pro/route-process/list-by-route` 的响应字段，提供工序总小时产能、总班次产能、无设备工序人工人数和产能来源，支撑前端将“准备时间”列替换为“班次产能”列，并在 `0 台` 弹窗中展示 5 人人工产能。

## 前置任务状态

- 已检查同主题后端任务 `20260608-route-process-machinery-capacity-list`：状态为已完成。
- 当前后端工作区存在既有运行态文件改动：`runtime/runtime-control/runtime-ops/alerts.json`、`runtime/runtime-control/runtime-ops/capacity-status.json`，本任务不修改、不提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。有设备工序只使用设备工序产能；无设备工序只使用工位人工产能；无工位或无产能字段按明确缺失显示 0，不切换到其他数据源。
- `是否从根因和长期维护角度解决`：是。后端在工序接口正式暴露工序级产能聚合字段，前端不复制跨表聚合规则。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 有设备工序返回机器班次产能 -> Given 工序工作站绑定多台设备且设备工序小时产能已配置 / When 查询工艺路线工序列表 / Then 返回 `capacitySource=MACHINE`、设备数量合计、工序小时产能合计和 10.5 小时班次产能。
- BDD: 无设备工序返回人工班次产能 -> Given 工序工作站没有设备但配置了单人小时产能和人工人数 / When 查询工艺路线工序列表 / Then 返回 `capacitySource=WORKER`、`workerQuantityTotal` 和人工总班次产能。
- BDD: 有设备时不使用人工兜底 -> Given 工序同时存在设备和人工配置 / When 查询工艺路线工序列表 / Then 工序产能只来自设备工序产能，不叠加或替换为人工产能。

## 里程碑

- [x] M1：创建任务文档，记录 BDD 与设计约束。
- [x] M2：新增后端 RED 测试。
- [x] M3：扩展响应 VO 与控制器聚合逻辑。
- [x] M4：运行后端目标测试和接口契约验证。
- [x] M5：更新证据，运行 task-closeout-cleanup 预览并提交。

## 预期验证

- `mvn -pl yudao-module-mes -Dtest=MesProRouteProcessControllerWorkstationViewTest test`
- 与前端真实页面 E2E 联动验证 `/mes/pro/route?openId=900026`。

## 当前状态

已完成：后端响应字段和聚合逻辑已实现，目标测试、本机重启、健康检查、证据校验和 task-closeout-cleanup 预览均通过，等待提交。

## 验证结果

- RED：`mvn -pl yudao-module-mes -Dtest=MesProRouteProcessControllerWorkstationViewTest test` -> FAIL，缺少新响应字段 getter。
- GREEN：`mvn -pl yudao-module-mes -Dtest=MesProRouteProcessControllerWorkstationViewTest test` -> PASS，6 tests。
- GREEN：`powershell -NoProfile -ExecutionPolicy Bypass -File script\deploy\restart-int-ruoyi-local.ps1 -Component backend` -> PASS。
- GREEN：`Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> PASS，status `UP`。
- GREEN：`python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260608-route-process-shift-capacity-display\backend-api-evidence.md` -> PASS。
- GREEN：`python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260608-route-process-shift-capacity-display --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --worktree-closeout off --json` -> PASS，delete/blocked/warnings 均为空。

## Cleanup Keep

- `doc/tasks/20260608-route-process-shift-capacity-display/backend-api-evidence.md`
