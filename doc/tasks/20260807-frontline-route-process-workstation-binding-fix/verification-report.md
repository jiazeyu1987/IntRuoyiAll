# Verification Report

## Scope

验证“一线生产”正式路线工序工作站绑定修复，覆盖候选流程图正式字段、版本发布投影、流程配置解析、当前正式数据、正式接口和真实页面点击。

## Result

PASS - 路线 `922119` 的正式工作站绑定已通过 V27 发布恢复；`processId=922985` 当前正式绑定 `workstationId=980010`，一线生产页面不再报错。

## Automated Verification

- RED：版本发布旧实现投影结果 `expected 980010 but was null`。
- RED：流程配置旧实现解析结果 `expected 980010 but was 922757`。
- GREEN：正式字段目标用例 1/1 通过。
- REGRESSION：`MesProRouteVersionPublishProjectionServiceTest`、`MesProRouteProcessFlowServiceImplTest`、`MesProRouteFlowConfigServiceImplTest` 共 78/78 通过。
- STATIC：3 条生产链路均读取或写入 `routeProcessWorkstationId`；未发现将展示字段 `workstationId` 用作正式绑定的读取/写入。
- EVIDENCE：bug regression evidence validator PASS。

## Data Verification

- 版本：V27/`id=627` ACTIVE；V24/`id=490` SUPERSEDED；V25/`id=624`、V26/`id=626` CANCELLED。
- 路线：`route_id=922119` 当前 14 条路线工序全部具有非空正式工作站。
- 一致性：14/14 工作站存在、启用、未删除，且工作站 `process_id` 与路线工序一致。
- 目标映射：`922985 -> 980010`；其余 13 条映射为 `922986 -> 980008`、`922987 -> 980009`、`922988..922998 -> 980011..980021`。
- 数据修复只使用正式候选保存、取消和发布 API，未直接写数据库。

## Runtime And API Verification

- 48081 actuator health=`UP`。
- 当前运行包内 `MesProRouteVersionPublishProjectionServiceImpl`、`MesProRouteProcessFlowServiceImpl`、`MesProRouteFlowConfigServiceImpl` class 哈希与任务隔离编译产物一致。
- 一线设备账号工序接口 business code=`0`，返回 14 条工序；目标工序唯一命中并返回 `workstationId=980010`。

## Real UI Verification

- Playwright 使用真实登录页进入生产组长模块并点击“一线生产”。
- 页面 URL 为 `/mes/pro/feedback/edhr-batch-production-fill`，数量、设备区域及设备候选正常渲染。
- 原错误文案无匹配，浏览器 console error=`0`。

## Residual Risk

- V25/V26 是本任务发布前门禁拦截的候选，均已正式取消且从未成为 ACTIVE。
- 正式工作站缺失门禁保持不变；未来数据缺失仍会明确失败，不会使用展示工作站、默认工作站或其它配置链路降级。

## Closeout

- `project-experience-consolidation` 已将正式/展示工作站字段分离门禁合并到现有后端经验文档和经验索引。
- `task-closeout-cleanup` 最终 apply 状态为 `applied`，无 blocker；仅保留核心任务记录和正式 `src/test` 回归。
- 实现与 cleanup 提交 `3dcc7f69a` 已推送到 `origin/int_main`。
