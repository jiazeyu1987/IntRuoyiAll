# 一线生产工艺路线工序正式工作站绑定修复

## Task Goal

定位并修复点击“一线生产”页签时出现 `工艺路线工序缺少正式工作站绑定，routeId=922119, processId=922985` 的问题，保持一线生产运行态继续读取正式路线工序工作站绑定，不引入默认值、过滤缺失工序或其它替代数据源。

## Milestones

- [x] M1：确认报错入口、后端门禁和目标路线工序身份
- [x] M2：核对当前数据库 schema、目标记录和可追溯的正式工作站来源
- [x] M3：先建立可复现的 RED 证据，再实施最小正式修复
- [x] M4：完成目标回归与真实页面验证
- [x] M5：完成证据归档、cleanup、提交和推送

## Expected Verification

- 只读核对 `mes_pro_route_process` 及相关正式工作站、路线版本、任务/排产来源，确认 `routeId=922119 / processId=922985` 的唯一正式绑定目标。
- RED：修复前通过正式接口或真实页面稳定复现业务码 `1040760104`。
- GREEN：修复后正式接口返回业务码 `0`，目标工序包含非空、存在且启用的正式 `workstationId`。
- REGRESSION：相邻一线生产工序加载、设备账号工作站门禁和目标真实页面路径通过；不得把 `formBindings`、批记录表单、工序开始上传人或默认工作站作为来源。
- Bug evidence validator PASS，任务文档结构与 `git diff --check` PASS。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；继续保留正式工作站缺失时 fail-fast 的门禁。
- `是否从根因和长期维护角度解决`：是；先核对正式数据来源后再修复，不在前端隐藏报错，不跳过缺失工序。
- `是否存在临时补丁或绕过`：否；不使用默认工作站、相邻工序工作站、`formBindings`、API-only 成功或 mock 数据替代正式绑定。

## Experience Gate

- `docs/experience-index.md` 存在并已读取。
- 适用门禁：`docs/backend-development.md#生产组长工序配置维护权限不得被工序开始快照误拦`。其中明确一线设备账号接口会执行正式工作站绑定门禁，不能用生产组长 `process-config/list` 的成功替代本路径验证。
- 收尾沉淀：已将本任务可复用的正式/展示工作站字段分离门禁合并到 `docs/backend-development.md#候选流程图正式工作站与展示工作站必须分字段`，并加入 `docs/experience-index.md` 路由。
- 项目术语契约继续生效：工序开始、批记录表单、表单槽位均不得替代路线工序正式工作站绑定。

## Current Status

completed - 正式字段修复、V27 发布、14 条正式工作站绑定、目标接口和真实“一线生产”页面均已验证通过；相关 3 个测试类 78 个用例和 bug evidence validator 均通过，经验沉淀、cleanup、实现提交 `3dcc7f69a` 及 `origin/int_main` 推送均已完成。

## Cleanup Candidates

- `output/verification/20260807-route-binding/`
- `output/verification/20260807-route-binding-v2.tar`
- `output/verification/20260807-route-binding-v3/`
- `output/verification/20260807-route-binding-v4/`
- `output/verification/20260807-route-binding-v5/`
- `output/verification/20260807-route-binding-v6/`
- `output/verification/20260807-route-binding.tar`
- `output/playwright/20260807-frontline-route-binding-fix/`
