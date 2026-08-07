# PQC组长的PQC管理新增5条一线提交数据

## Task Goal

- 在本机 `int_main` 测试环境中，通过真实一线 PQC 提交路径新增 5 条任务自有、可追踪的数据。
- 5 条数据必须进入 PQC 组长的 `PQC管理` 正式列表读模型，不使用 mock、前端假行、API-only 写入或孤立数据库记录。
- 数据限定在本机测试租户，且实际检验人必须属于目标 PQC 组长可见范围。

## Milestones

- [x] M1 核对本机运行态、测试租户/账号、正式 schema、发布 QA 规程、活跃订单与 PQC 组长人员范围。
- [x] M2 记录 BDD 与 RED，确认任务标识不存在，并排除并发写入冲突。
- [x] M3 为 5 个任务自有的单工序 ACTIVE 活跃订单各创建 1 条真实一线生产来源，再使用 Playwright 通过真实一线 PQC 页面各完成 1 次正式提交。
- [x] M4 通过真实 PQC 组长页面和只读 API/数据库复核新增 5 条数据及完整关联。
- [ ] M5 完成证据校验、经验沉淀和任务清理；等待 Git 提交与推送。

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

ready_for_closeout - 5 条真实一线 PQC 提交、PQC 组长页面验收、数据库一致性验证、经验沉淀、任务清理和本地提交均已完成；GitHub 代理 `127.0.0.1:7890` 未运行，推送暂时阻塞。

## Verification Evidence

- 本机前端 `8081`、后端 `48081` 与 Docker MySQL/Redis 运行正常，后端健康检查为 `UP`。
- tenant 1 的 PQC 检验员 `659` 在 PQC 组长 `512` 的启用人员范围内。
- 工单 `980022..980026` 的 5 条真实生产来源已生成事件 `166..170`；它们因路线其它 13 个工序缺 QA 规程而不能继续 PQC，作为已发生且可追踪的数据保留。
- 单工序正式路线复用已核对的工序 `922985`、工作站 `980010`、产品 `924008` 和账号 `659/shangmengying`，并使用独立代码前缀 `CODX-PQC-20260807-SP`。
- `RED`：正式事件中任务标识 `CODX-PQC-20260807` 命中 `0` 条；目标数据尚不存在。
- 目标工单 `980028..980032` 的真实生产来源事件为 `171..175`；正式一线 PQC 页面提交形成任务 `223..227`、PQC 事件 `181..185` 和 PQC 记录 `104..108`。
- 5 个任务均为 `SUBMITTED`，计划/实际检验数量均为 `3/3`，实际检验人均为 `659`，每个任务各有 3 条完整逐件明细，共 15 条。
- PQC 组长 `512/huzonggang` 的真实页面 `PQC管理` 已显示 5 个目标工单；只读分页接口按任务 ID 命中 5 条，业务码为 `0`。
- Playwright 截图：`output/playwright/20260807-pqc-leader-management-five-records.png`；结构化运行结果：`e2e-result.json`。
- 临时登录密码已在 `finally` 中恢复，账号 `512/659` 均无任务凭据标记残留。
- `task-closeout-cleanup` preview/apply 均通过，只保留任务三份核心记录；一次性 SQL、E2E 脚本、结果 JSON 和临时截图已删除。

## Remaining Blockers

- Git push blocker：全局 Git 配置 `http.https://github.com.proxy=http://127.0.0.1:7890`，端口检查为 `TcpTestSucceeded=False`，`git push origin int_main` 无法连接 GitHub。未禁用或绕过用户代理配置；分支仍领先 `origin/int_main`。
- 共享分支存在并发任务文件，本次提交已只包含任务自有记录、清理删除项和本次经验条目。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，使用正式一线 PQC 提交和 PQC 组长管理读模型链路。
- `是否存在临时补丁或绕过`：否；5 个待检轮次是可追踪的任务自有测试 fixture，最终 5 条提交仍由真实一线页面和正式后端事务产生。

## Cleanup Candidates

- `output/playwright/20260807-pqc-leader-management-five-records.png`
