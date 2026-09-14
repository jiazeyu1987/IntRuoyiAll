# 路线快照 routeProcess 运行态错误排查

## Task Goal

排查并修复一线 PQC 页面报错 `设备账号上下文不完整或不一致：routeVersion.routeSnapshotJson.routeProcessId=980645，processId=922985`，确认“按压式压力泵/球囊扩张压力泵”对应 PQC 红框内容是否能按产品、工序映射到 QA 检验项目；同时处理用户指出的“部分检验项目没有设备”不能被统一当作设备必填的问题。

## Milestones

- [x] M1 定位错误来源：源码、编译产物、运行态 Jar 或数据库正式快照
- [x] M2 如需代码修复，新增/更新 RED 回归测试
- [x] M3 实现最小正式修复或明确运行态未刷新处理方式
- [x] M4 运行目标验证并记录证据
- [ ] M5 收尾清理和经验复核

## Expected Verification

- 能解释 `routeProcessId=980645 / processId=922985` 属于哪条路线版本快照以及为什么当前接口会报错。
- 若是代码问题，目标 Maven 测试必须 PASS。
- 若是运行态未刷新，必须用源码/编译产物/运行进程证据说明，不做数据库或默认 fallback。
- 目标接口必须返回 `activeOrderId=48` 的冻结工序，包含 `routeProcessId=980645 / processId=922985`，且不再出现旧 `routeVersion.routeSnapshotJson.routeProcessId` 错误。
- “清洗工序”必须能挂到正式 QA/PQC 任务选项；无设备检验项必须由 QA 项目设备绑定数据决定 `equipmentRequired=false`，不得在接口或提交时把所有项目统一当作设备必填。

## Current Status

blocked

接口错误与快照工序映射已修复并在本机运行态验证通过；剩余阻塞为本地数据库写入未获明确授权，新增的 `20260808_mes_qa_optional_equipment_items.sql` 尚未应用到运行库，因此当前 API 仍会把 46 个无设备绑定的 QA 项显示为设备必填。

## Applicable Experience Gates

- `docs/backend-development.md#MES PQC 项目级检验快照门禁`：PQC 检验项目事实必须来自发布 QA 规程；设备是否必填按单个 QA 项目判断，`equipmentRequired=false` 且无设备选项应允许空设备快照，禁止把所有 PQC 项统一当作设备必填。
- `docs/database-rules.md`：数据修复必须先核对真实表结构与正式来源，缺少授权时不得直接修改数据库。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。接口改为读取 active order 冻结工序快照；无设备项通过正式数据迁移把 `equipment_required` 与项目设备绑定行一致化，不在运行时代码中降级。
- `是否从根因和长期维护角度解决`：是。代码修复快照工序漂移、重复 ACTIVE 订单和 PQC 提交生产事件上下文；数据迁移修复 QA 项设备必填标志与设备绑定不一致。
- `是否存在临时补丁或绕过`：否。热修 Jar 仅用于本机运行态验证；正式代码、测试和 SQL 迁移均已落地，数据库应用等待明确授权。
