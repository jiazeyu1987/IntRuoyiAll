# Execution Log

## 2026-09-28 强制改密与权限边界最终复验
- 运行环境：前端 `8062`、后端 `48062`，测试租户“芋道源码”；浏览器为 Playwright 独立 context。仅通过真实页面操作，未调用 `fetch`、APIRequest、axios、curl 或数据库；未执行 Git、服务启停或 DCC 业务写操作。
- Owner A `dccE2EOwnerA09286704`：先有一次人工转录错误导致登录 HTTP 200 / code `1002000000`；随后使用用户提供的准确临时凭据重试，登录 HTTP 200 / code `1002000011` 并进入 required-change 表单。改密自然请求 `/admin-api/system/auth/change-password-before-login` HTTP 200 / code 0，紧随的普通登录 `/admin-api/system/auth/login` HTTP 200 / code 0；页面进入 `/index`。关闭会话后在新 context 仅用新密码登录，HTTP 200 / code 0，再次进入 `/index`。
- Owner B `dccE2EOwnerB09284620`：登录 HTTP 200 / code `1002000011` 并进入 required-change 表单；改密、自动普通登录及新 context 再登录分别为 HTTP 200 / code 0，页面进入 `/index`。
- 两账号新 context 中均未发现 localStorage/sessionStorage 的密码键。新密码只存在于 Playwright 进程内存并用于真实表单；没有写入结果、日志、截图文字或任务文档。
- 通过 A/B 各自真实菜单展开并点击“文控中心 → 受控浏览”。`GET /admin-api/dcc/directories/tree` 均 HTTP 200 / code 0；页面“暂无可见目录”，切换“全域”后 0 行并显示“无权限或无匹配当前有效文件”。
- 按用户许可通过真实浏览器导航检查两个既有详情。A/B 对 `2054545668044084024` 与 `2054545668044084022` 的自然详情读取均 HTTP 200 / business code `1080000012`；页面显示 `Current user cannot access this controlled file`，文件元数据受限。详情未显示批准/拒绝/签名操作，未点击任何业务动作。
- 经真实侧栏打开审批中心待办。`GET /admin-api/approval-center/modules` 和 `/admin-api/approval-center/tasks/page` 均 HTTP 200 / code 0；页面稳定显示“暂无审批任务”，0 行，无审批动作可查看或执行。
- 最终判断：强制改密 E2E A/B 均 PASS；权限边界页面检查已取得负向拒绝证据，但缺少任何可见的本人部门授权文件/待办用于正向验证，整体 `BLOCKED_POSITIVE_SCOPE_NOT_AVAILABLE`。这不是 auth 流程缺陷；未修改产品代码。
- 综合脱敏结果：`doc/tasks/20260928-dcc-owner-permission-e2e/run-2026-09-27T22-20-34-232Z/result.json`。截图：`ownerA-fresh-login.png`、`ownerB-fresh-login.png`、`ownerA-dcc-controlled-browser-final.png`、`ownerB-dcc-controlled-browser-final.png`、`ownerA-detail-2054545668044084024-settled.png`、`ownerB-detail-2054545668044084024-settled.png`、`ownerA-detail-2054545668044084022-settled.png`、`ownerB-detail-2054545668044084022-settled.png`、`ownerA-approval-center-todo-settled.png`、`ownerB-approval-center-todo-settled.png`。
- Playwright 浏览器已在收尾时关闭；JSON 凭据扫描未发现明文临时/新密码，也未发现 token/cookie 等敏感字段。

## 2026-09-28 本轮恢复授权
- 本轮最新授权允许 admin 通过真实 `/system/user` 用户管理页为 Owner A/B 分别设置强随机临时密码；该授权 supersede 先前“不得使用管理员重置”的限制。
- 每个 Owner 首次登录后，若真实 UI 显示强制改密表单，再由该 Owner 改为另一组强随机最终密码。若不存在该表单，停止并标记 BLOCKED，不用短信、个人中心、第二次管理员重置或其他路径代替。
- 全程禁止 `fetch`、APIRequest、axios、curl、直接 HTTP 写入、数据库和 Git。权限验证仅真实页面只读导航，不点击审批、签名、分发或作废动作。
- 密码只在进程内存中使用；不得写入脚本、报告、JSON、截图或普通命令输出，只在完成后私下回传父线程。
- 预检：8062/48062 当前有监听，前端 Playwright 依赖存在。源码显示 admin 重置是页面 prompt，无安全理由字段；登录表单只定义租户、用户名、单个登录密码字段，`1002000011` 当前处理为页面登录错误。
- 通过 admin 的真实用户管理列表分别点击 Owner A、Owner B 行“重置密码”，在页面 prompt 输入强随机临时密码并确认；用户列表源码/实际 prompt 未要求安全理由，因此未出现理由字段可填写。
- 两次页面自然 `PUT /admin-api/system/user/update-password` 均 HTTP 200；成功 toast 出现后等待其消失，未读取/记录 toast 文本，随后确认目标用户行仍可见。未调用独立 API/fetch，也未读取响应体。
- 使用独立 Playwright browser context 分别以 Owner A/B 临时密码从真实登录表单提交。两人均停留 `/login` 并显示强制改密提示；每个可见登录表单仅 1 个密码输入框，没有新密码/确认密码或本人改密提交控件。两人状态均为 `BLOCKED_UPSTREAM_AUTH`。
- 依照边界，未尝试短信、个人中心、第二次 admin 重置或其它替代路径；未打开 Owner 的 DCC 受控浏览、详情或审批中心页面，因此权限边界结论未取得。未做业务文件写入、数据库写入或 Git 操作。
- 证据：`admin-reset-permission-2026-09-28/result.json`；列表截图 A/B 时间分别为 2026-09-28 04:28:50、04:29:05；Owner 登录截图及 result JSON 时间为 04:31:45。结果 JSON 不含密码、token、cookie 或响应体。
- 当前任务状态：`blocked`。可继续条件：应用提供 Owner 可实际使用的强制改密 UI，或用户给出另一项明确且符合系统权限模型的操作授权。

## 2026-09-28 授权续验启动
- 用户本轮授权通过登录页 UI 修改 Owner A、Owner B 首次密码；强随机密码仅能在内存中生成和使用，不得落入任何普通证据、日志或脚本。
- 已重新读取 `AGENTS.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md` 和 `docs/task-closeout-rules.md`。
- 既有真实登录结果：A/B 均被业务码 `1002000011` 阻止；历史脚本进入的“忘记密码”页需要手机号与短信验证码，不等同首次改密，不会用该路径替代。
- 源码只读核对：`AdminAuthServiceImpl.authenticate` 在签发登录响应前拒绝 `INITIAL`/`RESET_REQUIRED`；个人中心改密要求已认证用户；SMS reset 是独立手机号/验证码流程。本轮先由真实登录页判断是否存在强制改密 UI 表单。
- 服务只读预检：任务运行后端 `48062` health 返回 `UP`；前端 PID `49364`、后端 PID `26908`，对应 `C:\IntRuoyi\20260923-dcc-three-workflows-runtime` 任务运行目录。未启停服务。
- 未使用数据库、业务 API、`fetch`、APIRequest、Git 或其他账号；尚未改变账号密码。

## 2026-09-28 首次改密与权限边界实测
- Playwright 脚本：`verify-owner-forced-password-ui-real.cjs`；Node 语法检查通过。
- 真实登录页地址：`http://127.0.0.1:8062/login?redirect=/approval-center/todo`；租户页面选择“芋道源码”。每个账号均由独立 Playwright browser context 操作。
- Owner A：登录响应 HTTP 200、业务码 `1002000011`，页面可见“首次或重置后必须修改密码后再登录”；仍在 `/login`，可见表单仅有一个普通登录密码输入框、没有新密码/确认密码表单。状态 `BLOCKED`。
- Owner B：同一真实页面结果，HTTP 200、业务码 `1002000011`；没有强制改密表单。状态 `BLOCKED`。
- 被动响应记录每账号出现两条同路径登录响应，均为相同业务码；脚本只执行一次可见“登录”按钮点击。重复响应来源未查明，不影响两账号均未获得会话的结论。
- 本次没有生成或设置新密码；没有触发“忘记密码”、发送短信、请求短信验证码或提交重置；没有打开 DCC 浏览/详情/审批中心页面，也没有执行批准、拒绝、签名、分发、作废等操作。
- 权限边界结果：`BLOCKED_UPSTREAM_AUTH`，不能推断 A/B 的菜单、数据、跨部门可见性或动作投影。
- 证据：`doc/tasks/20260928-dcc-owner-permission-e2e/first-password-live-2026-09-27T20-16-39-408Z/result.json`、`ownerA-forced-password-page.png`、`ownerB-forced-password-page.png`。
- 服务健康只读检查 `48062/actuator/health` 为 `UP`；未启停服务。整个操作无数据库写入、直接 API/`fetch`/APIRequest 或 Git 操作。
- 阻塞判定：登录认证在发放 token 前拒绝首次密码状态，登录页没有自助首次改密 UI；个人中心改密必须先登录。短信找回需手机号和验证码，用户要求遇此情形立即停止，因此本轮不等待或尝试 OTP。

## 2026-09-28 Preflight
- Target: `int_qms` task runtime, frontend `8062`, backend `48062`.
- Tenant: 芋道源码.
- Test identities: Owner A and Owner B; credentials supplied only through the current execution environment.
- Read-only boundary: no proactive API calls, no database writes, no business-file writes, no owner configuration changes, no Git operations.
- Runtime process check: frontend and backend listeners confirmed on 8062/48062; backend PID/source recorded in the local verification output, not treated as business evidence.

## Verification Runs

### Run 1: 2026-09-28 real Playwright login preflight
- Script: `verify-owner-permission-real-ui.cjs`.
- Owner A: `BLOCKED` at real login. Natural login response HTTP 200 with business code `1002000011`; page message says the account must change its password after first login/reset.
- Owner B: `BLOCKED` at real login for the same business code/message.
- No DCC page was opened, and no approval/signature/distribution/obsolete action was clicked.
- Evidence directory: `doc/tasks/20260928-dcc-owner-permission-e2e/evidence-2026-09-27T19-43-04-133Z/`.
- This is an identity-state blocker, not a permission PASS/FAIL. Password change is a write operation and is outside this read-only run, so no bypass or alternate account was used.
- Passwords, tokens, cookies, and response secrets are not recorded here.

### Run 2: 2026-09-28 real login and password-reset UI recheck
- Script: `verify-owner-first-password-ui-real.cjs`.
- Explicit current-turn authorization allowed using the supplied accounts to complete first-password change through the real UI; no API/fetch/DB/Git was used.
- Owner A and Owner B were each submitted through a fresh Playwright browser context. Both natural login responses were HTTP 200 with business code `1002000011`; the login page visibly said `首次或重置后必须修改密码后再登录` and neither session entered the application.
- The visible login error did not expose an inline first-login password-change form. The real “忘记密码” page instead required a mobile number, SMS code, new password, and confirmation. The test did not have an authorized/known mobile number or received SMS code, did not click “获取验证码”, and did not submit a reset. No credentials were changed.
- No DCC/approval-center page was opened as Owner A/B. Permission visibility, action projection, and owner/non-owner boundaries remain untested, not failed.
- Evidence: `first-password-ui-2026-09-27T20-05-27-840Z/result.json` and the per-user login/reset screenshots in the same directory.
- Passwords, tokens, cookies, SMS codes, and response secrets are not recorded here. The Playwright result stores only username labels, login code/message, UI field presence, status, and screenshot paths.
