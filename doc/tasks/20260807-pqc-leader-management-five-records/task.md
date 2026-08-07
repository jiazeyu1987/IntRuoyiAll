# PQC组长的PQC管理新增5条一线提交数据

## Task Goal

- 在本机 `int_main` 测试环境中，通过真实一线 PQC 提交路径新增 5 条任务自有、可追踪的数据。
- 5 条数据必须进入 PQC 组长的 `PQC管理` 正式列表读模型，不使用 mock、前端假行、API-only 写入或孤立数据库记录。
- 数据限定在本机测试租户，且实际检验人必须属于目标 PQC 组长可见范围。

## Milestones

- [ ] M1 核对本机运行态、测试租户/账号、正式 schema、发布 QA 规程、活跃订单与 PQC 组长人员范围。
- [ ] M2 记录 BDD 与 RED，确认任务标识不存在，并排除并发写入冲突。
- [ ] M3 使用 Playwright 通过真实一线 PQC 页面完成 5 次正式提交。
- [ ] M4 通过真实 PQC 组长页面和只读 API/数据库复核新增 5 条数据及完整关联。
- [ ] M5 完成证据校验、经验沉淀、任务清理、提交与推送。

## Expected Verification

- 写入前核对真实表结构和正式提交样本，确认 PQC 任务、PQC 事件、PQC 记录、逐件/项目明细和 QA 规程来源。
- `RED`：任务标识 `CODX-PQC-20260807` 在正式 PQC 提交链路中的命中数为 `0`。
- `GREEN`：真实一线 PQC 页面连续完成 5 次提交，5 条记录各自具有正式 PQC task/event/record 和结构化检验项目数据。
- 使用目标 PQC 组长登录真实前端 `PQC管理`，断言 5 条新增记录均可见，列表接口业务码为 `0`。
- 使用只读 API/数据库核对租户、实际检验人、提交日期、工单/工序、QA 规程快照、检验数量和任务标识。

## Applicable Experience Gate

- `docs/backend-development.md#MES-PQC-项目级检验快照门禁`：检验项目必须来自发布 QA 规程并写入结构化 `itemResults` / `pqcItemDetails`，不得用固定字段或前端文本猜测。
- `docs/e2e-rules.md`：写入型 E2E 必须使用真实页面、确认的测试租户/账号和任务自有可追踪数据；API 仅用于最终验证。
- `docs/database-rules.md`：写入前按真实 schema 核对正式链路；并发写入同一业务范围时必须停止或隔离目标对象。

## Current Status

in_progress - 正在核对本机正式 PQC 提交前置、并发写入边界和可复用活跃订单。

## Verification Evidence

- 待执行。

## Remaining Blockers

- 暂无；若本机运行态、测试账号、发布 QA 规程、活跃订单、PQC 人员范围或正式提交入口缺失，将立即阻塞并记录影响。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，使用正式一线 PQC 提交和 PQC 组长管理读模型链路。
- `是否存在临时补丁或绕过`：否。

