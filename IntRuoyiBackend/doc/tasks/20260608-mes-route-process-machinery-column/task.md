# 20260608-mes-route-process-machinery-column

## 任务目标

扩展 MES 工艺路线工序详情接口 `GET /mes/pro/route-process/list-by-route`，返回每道工序的设备数量合计和设备资源列表，供前端工艺路线详情表展示。设备数量按当前工序下所有工作站设备资源绑定 `quantity` 求和；设备主数据或数量缺失时 fail fast。

## 前置任务状态

- 已检查最近后端任务 `20260608-runtime-console-overview-all-error`，状态为 completed。
- 当前后端仓库存在运行控制台相关既有脏改动，本任务不触碰、不提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺失设备主数据或设备数量为空时抛出明确错误，不按 0 或空列表静默降级。
- `是否从根因和长期维护角度解决`：是；在现有 route-process 查询中统一拼接工序设备资源，避免前端多接口重复聚合。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: route-process 返回设备数量求和 -> Given 某工序关联多个工作站设备资源 / When 查询路线工序列表 / Then 返回该工序设备资源 `quantity` 合计。
- BDD: route-process 返回设备列表 -> Given 某工序存在多个设备绑定 / When 查询路线工序列表 / Then 返回按工作站和设备编码排序的设备列表。
- BDD: 设备主数据缺失时失败 -> Given 工作站设备绑定引用不存在的设备 / When 查询路线工序列表 / Then 接口抛出包含绑定 ID 和设备 ID 的错误。
- BDD: 设备数量缺失时失败 -> Given 工作站设备绑定数量为空 / When 查询路线工序列表 / Then 接口抛出包含绑定 ID 和设备 ID 的错误。

## 里程碑

- [x] M1：创建任务文档，记录 BDD 与设计约束。
- [x] M2：写后端 RED 单元测试。
- [x] M3：扩展 route-process 响应 VO 与控制器聚合逻辑。
- [x] M4：运行后端目标测试和资源大表回归。
- [ ] M5：更新执行证据，运行 task-closeout-cleanup 预览，仅提交本任务相关改动。

## 预期验证

- `mvn --% -pl yudao-module-mes -Dtest=MesProRouteProcessControllerWorkstationViewTest,MesProRouteResourceServiceImplTest test`

## 当前状态

blocked: 后端实现与目标 Maven 测试已通过；跨前端真实 E2E 在测试租户 `aoteman` 点击设备编码时被现有设备台账查询权限 `mes:dv-machinery:query` 阻塞，暂不提交。管理员账号只读验证已确认同一代码路径可完整打开设备详情。

## 最终验证结果

- PASS: `mvn --% -pl yudao-module-mes -Dtest=MesProRouteProcessControllerWorkstationViewTest,MesProRouteResourceServiceImplTest test`，10 个测试通过。
- PASS: 真实接口 `GET /mes/pro/route-process/list-by-route` 返回 `machineryQuantityTotal` 与 `machineryList`，示例工序 `B010` 数量为 `5`，列表包含 5 条设备资源。
- BLOCKED: 测试租户 `测试租户/aoteman` 的完整真实 E2E 在点击设备编码后，现有设备详情接口 `/mes/dv/machinery/get` 返回 `Access Denied`，业务码 `500`；缺少权限导致无法完成测试租户下设备台账详情展示验证。
- PASS: `芋道源码/admin` 只读验证通过设备数量、设备列表与现有设备台账详情弹窗。

## Cleanup Keep

- `doc/tasks/20260608-mes-route-process-machinery-column/backend-api-evidence.md`
