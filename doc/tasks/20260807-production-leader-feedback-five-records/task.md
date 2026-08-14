# 生产组长报工管理新增 5 条一线提交数据

## Task Goal

在本机 `int_main` 测试环境中，为生产组长的“报工管理”新增 5 条任务自有、可追踪、符合一线生产正式提交链路的数据，并确保生产组长本人及已配置正式员工范围的本机 admin 可见。

## Milestones

- [x] M1 核对本机运行态、真实 schema、现有正式链路样本和目标生产组长责任范围。
- [x] M2 记录 BDD 与 RED，确认本次任务标识尚无数据。
- [ ] M3 按正式链路写入 5 条报工、记录本、工序池事件、数量片段并更新工序池汇总。
- [ ] M4 通过数据库、生产组长登录态接口和真实前端页面验证 5 条数据可见且员工姓名可解析。
- [ ] M5 完成证据校验、经验沉淀和任务收尾。

## Expected Verification

- 写入前使用真实 schema 和既有正式样本核对表字段、关联关系、租户、员工、工单、任务、路线工序、工序池和生产组长责任范围。
- `RED`：任务标识 `CODX-RPT-20260807-%` 在正式报工和 `PRODUCTION_SUBMIT` 工序池事件中的计数均为 `0`。
- `GREEN`：正式报工、记录本 entry、记录本 event、工序池事件和数量片段均新增且各命中 `5` 条，工序池汇总指向最后一条事件。
- 使用生产组长本人登录态请求报工分页接口，按任务标识或事件 ID 断言命中新增 5 条，`actualEmployeeUserName` 非空。
- 使用 Playwright 走本机真实前端“生产组长 -> 报工管理”页面，断言今日列表可见新增任务数据、目标接口业务码为 `0`，且页面无目标链路错误。

## Applicable Experience Gate

- 生产组长报工管理按 `mes_pro_process_pool_event.server_submit_time`、`actual_employee_id` 和生产组长责任员工集合筛选。
- 造数必须同时补齐正式报工、记录本 entry/event、工序池 `PRODUCTION_SUBMIT` 事件、数量片段和 `mes_pro_process_pool` 汇总。
- `actual_employee_id` 必须在目标生产组长 `PRODUCTION + EMPLOYEE` scope 内，且可解析到 `system_users.nickname`。
- 禁止只写 `mes_pro_feedback`、直接改工序池汇总、前端假行或使用 admin 冒充生产组长可见性。

## Current Status

blocked - M1/M2 已完成，但写入前置检查发现正式一线提交上下文失效；事务已回滚且任务数据计数保持为 `0`。

## Verification Evidence

- 本机前后端、MySQL、Redis 和 OnlyOffice 健康检查通过；未重启或占用其它任务进程。
- 员工 `964/liuyueyue`、生产组长 `1520/lvyujie`、工单 `980008`、任务 `981941`、路线 `922119`、工序 `922987`、工作站 `980009`、设备 `41`、记录本 `980011`、模板 `980010` 和工序池 `37` 有效；但工序池引用的路线工序 `928611` 已软删除。
- `1520 + PRODUCTION + EMPLOYEE + 964` 责任范围存在且启用；员工 `964` 具有一线报工创建与 eDHR 批次执行查询权限。
- RED 已通过：`mes_pro_feedback.code LIKE 'CODX-RPT-20260807-%'` 与 `mes_pro_process_pool_event.event_idempotency_key LIKE 'CODX-RPT-20260807-%'` 的计数均为 `0`。

## Remaining Blockers

- 工序池 `37` 引用路线工序 `928611`，该记录当前 `deleted=1`；同路线/工序的活动记录 `980647` 当前 `workstation_id=NULL`。
- `MesFrontlineDeviceAccountContextServiceImpl` 只读取活动路线工序并要求工作站非空，因此当前上下文不符合正式一线生产提交条件；直接补写历史六表数据会伪造正式提交来源，违反本任务 no-fallback 门禁。
- 需要用户明确授权修复正式路线工序/工作站绑定并重建或迁移对应工序池上下文后，才能继续新增 5 条数据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按生产组长报工管理正式时间线读模型所需完整数据链路造数。
- `是否存在临时补丁或绕过`：否。
