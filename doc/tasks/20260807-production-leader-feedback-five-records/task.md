# 生产组长报工管理新增 5 条一线提交数据

## Task Goal

在本机 `int_main` 测试环境中，为生产组长的“报工管理”新增 5 条任务自有、可追踪、符合一线生产正式提交链路的数据，并确保生产组长本人及已配置正式员工范围的本机 admin 可见。

## Milestones

- [ ] M1 核对本机运行态、真实 schema、现有正式链路样本和目标生产组长责任范围。
- [ ] M2 记录 BDD 与 RED，确认本次任务标识尚无数据。
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

in_progress - 已完成规则与历史正式链路核对，正在检查本机运行态、真实 schema 和可复用业务对象。

## Verification Evidence

- 待执行。

## Remaining Blockers

- 暂无；若本机数据库、测试租户、目标生产组长、正式业务链路或可清理范围缺失，将立即阻塞并记录影响。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按生产组长报工管理正式时间线读模型所需完整数据链路造数。
- `是否存在临时补丁或绕过`：否。

