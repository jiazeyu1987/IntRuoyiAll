# DCC Owner Permission Boundary E2E

## Current Status
blocked

### 2026-09-28 本轮真实页面验收
- Owner A、Owner B 均通过真实登录页完成强制改密、改密后自动登录，并在独立浏览器上下文中仅使用新密码再次登录；相关自然请求均 HTTP 200 / code 0，页面进入 `/index`。
- 两个账号都能从真实菜单进入“文控中心 → 受控浏览”和“审批中心 → 待办”。受控浏览目录树为空，全域视图 0 行；页面显示“无权限或无匹配当前有效文件”。
- 真实浏览器直达既有详情 `2054545668044084024` 和非本人链文件 `2054545668044084022` 时，详情读取均 HTTP 200 / business code `1080000012`；页面明确显示当前用户不能访问受控文件，文件编号/负责人快照受限。没有可见审批动作。
- 待办页面自然查询 HTTP 200 / code 0，稳定空态为“暂无审批任务”，0 行且无批准/拒绝/签名动作。
- 权限正向范围仍未验证：当前租户没有可见的本人部门受控文件或待办样本。因此账号改密验收 PASS，权限边界整体验收 `BLOCKED_POSITIVE_SCOPE_NOT_AVAILABLE`，不能推断部门授权正确或错误。证据位于 `run-2026-09-27T22-20-34-232Z/`；综合 JSON 不含任何凭据。

### 2026-09-28 当前轮授权与验收口径

本轮最新授权 supersede 前文“不得改用管理员重置”的限制：先由 admin 在 `/system/user` 对 Owner A/B 分别使用真实页面“重置密码”设置强随机临时密码；随后每个 Owner 真实登录，如出现强制改密表单，则在该真实 UI 内改为另一组强随机最终密码。不得调用 `fetch`、APIRequest、axios、curl、直接 HTTP 写入、数据库或 Git。

若重置后没有可用的 Owner 自助强制改密 UI，不得改用短信、个人中心、另一次管理员重置或其它路径替代；记录页面实际结果并将改密及后续权限 E2E 标为 BLOCKED。临时密码、最终密码、toast 中的密码及任何 token/cookie 不得写入脚本、报告、JSON、截图或命令输出；密码只在内存中流转，完成后仅私下回传父线程。

通过改密后，Owner A/B 分别使用独立浏览器上下文，只读检查受控浏览、目标详情、审批中心待办和操作投影；不点击批准、拒绝、签名、分发、作废或任何业务写操作。不更改角色、部门负责人或 DCC 文件。

本轮结果：A/B 的 admin 页面临时密码重置均 PASS（自然 `PUT /admin-api/system/user/update-password` HTTP 200，成功 toast 出现并消失）；A/B 本人登录均停在 `/login` 并看到强制改密提示，但页面没有本人改密控件。权限 E2E 未开始，当前状态 `blocked`。证据：`admin-reset-permission-2026-09-28/result.json`。

## 2026-09-28 首次改密与权限边界续验

### 本轮授权和边界
- 前次授权仅允许登录页上真实呈现的首次改密表单；该限制已由本轮顶部最新授权明确 supersede。
- 改密请求及任何受控浏览、审批动作只能由 Playwright 页面交互触发；不得调用 `fetch`、APIRequest、axios、curl 或直接业务接口。
- 新密码只保存在当前进程内存，并仅作为单独敏感凭据字段返回；不写入脚本、JSON、截图、Markdown、命令记录或控制台普通摘要。
- 首次改密后，Owner A 与 Owner B 必须分别使用独立浏览器上下文真实登录；权限验证限只读页面与动作投影，不点击批准、拒绝、签名、分发、作废等最终业务动作。
- 禁止数据库写入、Git 操作、服务启停和更改部门负责人/角色。

### BDD: 首次登录强制改密 -> Given/When/Then
- Given Owner A 或 Owner B 使用首次/重置凭据通过租户登录页提交登录。
- When 后端返回 `1002000011`。
- Then 登录页提供可由该账号本人提交的新密码表单；表单完成后新凭据可重新登录，且普通证据不包含任何密码。

### BDD: 负责人权限边界 -> Given/When/Then
- Given A 为生产负责人、B 为质量负责人且各自已完成本人登录。
- When 分别从真实页面进入 DCC 受控浏览、目标详情和审批中心待办。
- Then 记录本部门/跨部门可见数据、任务归属、按钮可见与禁用状态；无权限对象不得暴露可执行动作，且不执行任何审批写操作。

### 本轮验收结果要求
- 改密阶段必须由登录页真实表单完成，否则记为 BLOCKED 并保存实际页面提示及截图。
- 权限 E2E 只有 A、B 都能通过真实 UI 登录后才执行；不得用 admin 或另一用户替代。
- 结果与截图位于独立 run 目录；报告不含密码、token、cookie、手机号或短信验证码。

## Task Goal
使用测试租户“芋道源码”中的两个已配置部门负责人账号，通过真实 Playwright 页面做只读权限边界验收。验证生产负责人和质量负责人分别能看到的 DCC 页面、受控文件数据、审批中心待办及详情动作投影，并确认非本人部门/非本人任务不会被误授权。

## Scope
- Environment: `http://127.0.0.1:8062`, backend `48062`.
- Accounts: Owner A and Owner B; credentials are supplied at runtime only and are not recorded here.
- Read-only navigation, DOM assertions, screenshots, and passive response listeners only.
- No database writes, business-file writes, owner configuration changes, Git operations, `fetch`, `APIRequest`, `axios`, or direct API calls.

## BDD

### BDD: Owner A page and data visibility -> Given/When/Then
- Given Owner A logs into tenant “芋道源码” through the real login page.
- When Owner A opens DCC controlled browsing, a known controlled-file detail, and approval-center todo pages through the UI.
- Then the report records visible menus, rows, detail sections, and whether every passive page response is successful or explicitly rejected.

### BDD: Owner B page and data visibility -> Given/When/Then
- Given Owner B logs into tenant “芋道源码” through the real login page.
- When Owner B opens the same read-only DCC and approval-center pages through the UI.
- Then the report records the same facts independently and does not reuse Owner A session state.

### BDD: Non-owner boundary -> Given/When/Then
- Given each owner is responsible for a different department.
- When each owner inspects files/tasks belonging to the other department or not assigned to the owner.
- Then the UI must either omit the object/action or show a clear backend authorization/business denial; a blank page, silent empty result, unexpected 403, or visible unauthorized action is recorded as a failure or blocker with evidence.

### BDD: Action projection without execution -> Given/When/Then
- Given the test is strictly read-only.
- When an approval/detail page is opened and action controls are inspected without clicking final business actions.
- Then the report records whether approve/reject/sign/distribute/obsolete controls are visible, disabled, or absent, and captures passive responses only.

## Expected Verification
- Fresh browser context and real login for Owner A and Owner B.
- DCC controlled browser page and visible table state.
- At least one known controlled-file detail page per account, including route/owner snapshot and action controls.
- Approval center todo page, task rows, and task-detail/action projection.
- Passive response log for HTTP status, 403, 500, 503, business error envelopes, and empty misleading states.
- Screenshot and JSON result for each account plus a combined verification report.

## Design Constraints Check
- [x] Read `AGENTS.md`, E2E, login, runtime, worktree, and closeout rules.
- [x] Use real Playwright UI navigation only.
- [x] Keep credentials out of task documents and ordinary reports.
- [x] No API/DB action substitutes.
- [x] No writes or Git operations.
- [x] After explicit authorization, rechecked first-password handling only through the real login/reset UI; no SMS was requested and no password reset was submitted.
- [x] Reset both test credentials through the admin user-list UI; each natural request returned HTTP 200 and the success toast appeared.
- [x] Confirmed both Owner login pages have one ordinary password field and no self-service forced-change form.
- [ ] Complete read-only owner permission E2E; blocked upstream because neither identity can authenticate.

## Cleanup Keep
- doc/tasks/20260928-dcc-owner-permission-e2e/task.md
- doc/tasks/20260928-dcc-owner-permission-e2e/execution-log.md
- doc/tasks/20260928-dcc-owner-permission-e2e/verification-report.md
- doc/tasks/20260928-dcc-owner-permission-e2e/verify-owner-permission-real-ui.cjs
- doc/tasks/20260928-dcc-owner-permission-e2e/verify-owner-first-password-ui-real.cjs
- doc/tasks/20260928-dcc-owner-permission-e2e/verify-owner-forced-password-ui-real.cjs
- doc/tasks/20260928-dcc-owner-permission-e2e/verify-owner-first-password-ui-real.cjs
- doc/tasks/20260928-dcc-owner-permission-e2e/run-2026-09-27T22-20-34-232Z/result.json
