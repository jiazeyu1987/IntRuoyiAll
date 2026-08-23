# Production Release Yudao Role Baseline Authorization

## Request Summary And Source

- Source: 用户于 2026-08-17 在主任务中回复“授权”。
- Context: 该回复直接对应主 Agent 的阻塞问题，即是否允许在本机“芋道源码”通过正式权限页面创建生产放行所需的两个专用角色，并分别分配给朱利江和徐建海。
- Follow-up source: 菜单前置暴露三个必需按钮权限缺失后，用户再次回复“授权”，明确批准通过正式菜单页面创建这三个权限节点并继续角色基线操作。
- Authorized scope: 仅在现有 eDHR 工作任务页面下创建 `mes:pro-production-release:query`、`mes:pro-production-release:pqc-approve`、`mes:pro-production-release:pqc-reject` 三个按钮权限；仅创建 `MES_PQC_RELEASE_OWNER`、`MES_MANAGEMENT_REPRESENTATIVE`，按已合入正式定义配置最小权限，并分别绑定 `zhulijiang`、`xujianhai`。
- Excluded scope: 不修改其它用户、角色、租户基线或无关业务数据；不使用 SQL/API 代替真实页面操作；不访问远程或生产环境。

## Current Baseline Reviewed

- Task: `doc/tasks/20260814-production-release-flow-implementation`，P1-P10 已完成，P11 因角色基线缺失而阻塞。
- Runtime: 本机 `int_main` 的 8081/48081 当前可访问，运行包包含生产放行核心实现。
- Page evidence: “芋道源码”权限角色页面精确查询两个角色标识均返回 0；用户页面中朱利江、徐建海均未显示对应角色。
- Formal definition: `IntRuoyiBackend/sql/mysql/20260814_mes_production_release_roles.sql` 已定义两个角色、目标账号和最小权限集合。

## Classification

- Type: environment and authorization baseline change required for real E2E acceptance.
- This does not change product behavior, PRD, API contract or development milestone scope.

## Impact Analysis

- Product: no product code change.
- Design: no design change; applies the already approved strict candidate-role design.
- Data: creates two tenant-scoped role records, their formal menu-permission assignments and two user-role bindings through real administration pages.
- API: no direct API operation; requests are emitted only by the real frontend workflow.
- Test: unblocks account permission preflight; order fixtures remain prohibited until role creation, assignment, relogin and exact-permission checks pass.
- Release: no deployment, push or remote operation.
- Operations: local “芋道源码” only; all affected role codes and usernames are fixed and auditable.
- Security: no password, token, cookie or authorization header is recorded; no admin role is assigned to business accounts.

## Decision

- Decision: `ACCEPT`.
- Reason: 用户已明确批准处理唯一已报告 blocker，且范围可被两个角色、两个账号和正式最小权限集合精确限定。

## Required Approvals

- User approval: received twice in current thread，分别覆盖两个角色与固定绑定、三个缺失的全局按钮权限。
- Additional approval: required before any expansion beyond the two named roles, two named users or their formal permissions.

## Downstream Skill Reruns

- Resume `development-plan-supervisor` and `development-plan-delivery` at P11 only.
- Use `playwright` for real role creation, menu-permission assignment, user-role assignment and read-only re-verification.
- After the baseline passes, resume the existing P11 zero-write prerequisite inventory before creating task-owned order fixtures.

## Blockers And Next Action

- Resolved blocker: the user explicitly authorized creating the three missing button-permission menu records under the existing eDHR work-task page through the formal menu page.
- Safety baseline before execution: no role, menu, user binding, order, attachment or fixture has yet been created or changed.
- Next action: create and verify the three button permissions, then create/configure the two roles, bind the fixed users, relogin as each account, and verify the exact permission set before any order data is created.
