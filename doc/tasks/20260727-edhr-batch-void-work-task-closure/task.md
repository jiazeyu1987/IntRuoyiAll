# eDHR 批次作废后工作任务闭环设计

## Task Goal

先完成“批次执行作废后，负责人工作台任务同步闭环”的文档设计，覆盖作废终态、工作台待办移除、活动工作任务取消、旧入口阻断、历史审计保留、以及后续只能走受控重开/补录/重执行流程六项能力。

## Milestones

- [x] 建立任务目录与规则/技能前置
- [x] 复核既有作废、工作任务、个人工作台和终态待办门禁证据
- [x] 编写系统设计文档
- [x] 编写 BDD/TDD/E2E/测试数据验收设计
- [ ] 进入代码实现前补 RED 测试
- [ ] 实施后端作废生效点任务取消闭环
- [ ] 完成 GREEN、回归、真实路径验证与收尾

## Expected Verification

- 后端 RED 先证明作废生效后仍存在待处理、处理中、逾期中的工作任务。
- GREEN 后作废批次状态为 `VOIDED`，同批次活动工作任务为 `CANCELED`，已完成历史任务保持 `DONE`。
- 个人控制台、统计、审批中心待办、候选签名待办不展示或计入作废批次任务。
- `openTask` 对作废批次继续 fail-fast，不能为了可点击而放松终态保护。
- 作废事件、签名、归档失效、工作任务取消原因和权限回收均可追溯。
- 后续处理仅允许走受控重开、补录、重执行或重新建批流程。

## Current Status

design_ready

## Design Artifacts

- `docs/system/backend-api-design.md`
- `docs/system/frontend-design.md`
- `docs/system/data-model.md`
- `docs/system/config-security-deployment.md`
- `docs/acceptance/bdd-scenarios.md`
- `docs/acceptance/tdd-plan.md`
- `docs/acceptance/e2e-plan.md`
- `docs/acceptance/test-data.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；作废闭环必须在正式作废生效事务内完成，失败即回滚或明确失败。
- `是否从根因和长期维护角度解决`：是；设计目标是在作废生效点取消活动工作任务，不只依赖工作台列表过滤。
- `是否存在临时补丁或绕过`：否；不得通过前端隐藏、API-only 打开、静默忽略取消失败或物理删除任务来绕过。

## 经验门禁

- 命中 `docs/e2e-rules.md#eDHR 终态批次个人待办门禁`：批次已关闭/归档/驳回/作废时，`openTask` 阻断正确，个人控制台列表和统计必须从源头过滤终态批次残留待办。
- 命中 `docs/backend-development.md`：后端行为变更必须 BDD + RED/GREEN/REGRESSION，缺少依赖或测试数据时 fail fast。
- 命中 `docs/database-rules.md`：本设计不新增迁移；若实现阶段发现字段缺失，必须先核对 schema，不得猜测写 SQL。
- 命中 `docs/frontend-development.md` 与 `docs/e2e-rules.md`：真实工作台验证必须走 Playwright 真实页面，API 仅做最终只读核验。
