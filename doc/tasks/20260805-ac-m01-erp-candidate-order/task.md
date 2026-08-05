# AC-M01 ERP 确认生产订单候选查询

## Task Goal

实现并验证 AC-M01：计划排产员 / 生产班组长确认生产订单时，仅允许 ERP 已确认且具备正式 ID/编号的生产订单进入候选，并能按正式 ID/编号查询；未确认、缺正式 ID 或越权订单不得进入候选。

## Milestones

- [x] M1 明确 ERP 已确认、正式 ID/编号、权限/租户边界的现有字段与数据来源。
- [x] M2 先补充失败测试覆盖 AC-M01 正向和边界行为。
- [x] M3 实现最小正式后端候选查询能力，不引入 fallback、默认成功或静默降级。
- [x] M4 补齐前端真实入口/API 合约与静态验证。
- [ ] M5 更新 AC-M01 真实 E2E 覆盖证据和验收报告。
- [ ] M6 完成 evidence validator、回归验证和任务收尾。

## Expected Verification

- Backend：目标 JUnit/服务或控制器测试先 RED 后 GREEN，覆盖 ERP 已确认正式订单、未确认订单、缺正式 ID/编号订单、越权/跨租户订单。
- Frontend：目标 API/UI 合约测试或静态测试先 RED 后 GREEN，确认查询参数、候选展示、空/错误态和权限入口一致。
- E2E：通过真实前端路径记录 AC-M01 action evidence；如运行环境、账号、菜单或测试数据缺失，记录明确 blocker，不用 API-only 替代通过。
- Evidence：`backend-api-evidence.md`、`frontend-feature-evidence.md` 和 QA/验证报告通过对应 validator 或记录阻塞原因。

## Current Status

in_progress

当前已完成后端候选准入硬门禁和前端静态合同：admission-diff 会把缺 ERP 正式同步身份的工单标记为 `BLOCKED_ERP_SYNC_RECORD_MISSING` 且不可勾选，批量加入接口会 fail-fast 拒绝未确认或缺正式 ERP ID/编号的工单。真实 E2E、提交推送和任务收尾仍未完成，原因见验证报告 Blockers。

## BDD Scenarios

- BDD: ERP confirmed formal order candidate -> Given ERP 已同步并确认的生产订单存在正式 ERP ID/编号和本租户权限；When 计划排产员或生产班组长按正式 ID/编号查询候选订单；Then 该订单进入候选结果，结果保留同一正式 ID/编号供后续确认生产订单使用。
- BDD: ERP unconfirmed order excluded -> Given ERP 生产订单尚未达到确认状态；When 用户查询生产订单候选；Then 该订单不得出现在候选结果中。
- BDD: Missing formal ERP identity excluded -> Given 本地工单缺少正式 ERP ID/编号或缺少同步记录；When 用户查询生产订单候选；Then 该订单不得出现在候选结果中。
- BDD: Unauthorized or cross-tenant order excluded -> Given 订单属于其他租户或当前用户无查询权限；When 用户查询生产订单候选；Then API/UI 不返回该订单并保留现有权限错误语义。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；本任务禁止用默认状态、空值补全、API-only 替代 E2E 或 mock 成功覆盖 AC-M01。
- `是否从根因和长期维护角度解决`：是；目标是补齐正式候选查询的数据源、权限和测试证据，而不是在 E2E 结果中手工标记通过。
- `是否存在临时补丁或绕过`：否；若发现正式字段、测试数据、菜单入口或运行环境缺失，将记录 blocker 并停止对应验证。

## Experience Gate

- 已读取 `docs/experience-index.md`；本任务命中 PowerShell Maven `-D` 参数引号门禁、前端静态契约隔离门禁、真实 E2E 入口存在性门禁和严格 no-fallback 门禁。
- 本任务暂不修改数据库 schema，不使用 API-only 或 mock 代替真实 E2E；若本机运行态、账号、菜单或 ERP 同步样本缺失，将记录为 E2E BLOCKED。
