# Required Password Change Before Login

## Goal

修复管理员将后台用户密码重置为 `RESET_REQUIRED` 后，用户正确输入现有密码却无法完成首次/重置后改密的问题。提供公开的 pre-login 改密入口，但必须先验证租户、启用状态、锁定状态及现有密码；不得签发 token，成功后由前端使用新密码正常登录。

## BDD

- BDD: INITIAL 用户完成首次改密 -> Given 启用且未锁定的 INITIAL 用户在正确租户中提供正确现有密码 -> When 提交符合正式密码策略且确认一致的新密码 -> Then 正式密码策略/历史限制执行、密码状态变为 ACTIVE、接口不签发或写入 token，前端随后只通过正常登录建立会话。
- BDD: RESET_REQUIRED 用户完成重置后改密 -> Given 管理员重置后的启用且未锁定用户 -> When 正确提供现有临时密码并提交有效新密码 -> Then 复用正式密码更新服务后状态变为 ACTIVE，随后正常登录成功。
- BDD: 错误凭据复用登录失败控制 -> Given 用户名/租户/旧密码不匹配或账号禁用/锁定 -> When 调用 pre-login 改密 -> Then 响应不泄露账号是否存在，复用登录认证失败计数/锁定行为，不更新密码且不签发 token。
- BDD: ACTIVE 用户不可使用 pre-login 改密 -> Given 正常 ACTIVE 用户 -> When 调用 pre-login 改密 -> Then 明确拒绝且不更新密码或认证状态。
- BDD: 新密码仍受正式策略约束 -> Given INITIAL/RESET_REQUIRED 用户正确验证现有密码 -> When 新密码弱或命中历史密码 -> Then 复用正式策略拒绝且状态不变。
- BDD: 必改密码登录页面 -> Given login 返回 `AUTH_LOGIN_PASSWORD_CHANGE_REQUIRED` -> When 前端进入强制改密 -> Then 展示现有密码、新密码、确认密码表单；成功后使用新密码调用正常 login，只有 login 成功才保存 token；密码不得进入 URL/query/localStorage。

## Milestones

1. Review auth/user/password-policy/controller/frontend boundaries and existing tests.
2. Add RED service/controller and frontend static-contract tests.
3. Implement pre-login password-change service/controller and required-change UI/API.
4. Run GREEN and regression tests, TypeScript check, static contracts, and diff check.
5. Complete verification report and closeout state consistent with the explicit no-Git constraint.

## Expected Verification

- Targeted system auth/user tests with Maven `-am`, covering INITIAL and RESET_REQUIRED success, wrong old password and lockout accounting, disabled/locked user rejection, ACTIVE rejection, weak/reused password rejection, and no token issuance.
- Frontend static tests for required-change form, normal login after change, and no password in URL/query/localStorage.
- `pnpm ts:check`.
- `git diff --check` is requested by the task owner but this task explicitly prohibits Git operations; confirm the repository-approved non-Git whitespace checker or record the conflict without executing a Git command.
- Structural validation of task documents.

## 设计约束检查 / Design Constraints

- Only `INITIAL`, `RESET_REQUIRED`, and explicitly recognized expired-password status may use the endpoint; `ACTIVE` is denied.
- Validate current tenant/user enabled/login lock/password match exactly through the established authentication behavior and failure accounting.
- Unknown user and wrong old password must not reveal account existence.
- Call `updateUserPassword(Long, UserProfileUpdatePasswordReqVO)` or the same formal service policy for complexity/history/old-password rules and status transition to `ACTIVE`.
- No token/session issuance on the pre-login endpoint. The browser must perform a standard login with the new password afterward.
- No bypass, status-setting endpoint, admin impersonation, database writes, external service operations, or Git operations.
- Never persist passwords in task logs, evidence, URL/query, or local storage.

## Current Status
blocked

实现与指定验证已通过。按照用户本轮要求，未执行任何 Git 操作；仓库 `task-closeout-rules.md` 将 Git/cleanup 收尾设为任务完成门槛，因此该任务保留 blocked 状态，不宣称已完成 closeout。

## Cleanup Keep
- doc/tasks/20260928-auth-required-password-change/task.md
- doc/tasks/20260928-auth-required-password-change/execution-log.md
- doc/tasks/20260928-auth-required-password-change/verification-report.md
