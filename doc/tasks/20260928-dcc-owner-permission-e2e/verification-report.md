# Verification Report

## Status
BLOCKED

## 2026-09-28 实际页面 E2E

### 强制改密：PASS
- Owner A 与 Owner B 均从真实登录页进入 required-change 表单。A 一次人工输入转录错误返回 HTTP 200 / code `1002000000`；用用户提供的准确临时凭据重试后正常进入改密流程。
- 两账号改密自然请求 `/admin-api/system/auth/change-password-before-login`、随后自动普通登录 `/admin-api/system/auth/login` 均为 HTTP 200 / code 0，页面离开 `/login` 到 `/index`。
- 每个账号关闭原 context 后，以新 context 仅使用新密码再次登录；普通登录均 HTTP 200 / code 0，进入 `/index`。检查未发现 localStorage/sessionStorage 密码键。
- 新密码只在 Playwright 进程内存中生成和使用；结果 JSON 仅包含用户名、路由、请求路径、HTTP/业务码及业务状态。运行目录 JSON 的敏感值检查为 0 命中；没有在文档或截图说明中记录凭据。

### DCC 权限边界：部分观察，整体 BLOCKED
- A/B 均从真实菜单进入受控浏览；目录树请求 HTTP 200 / code 0，但页面目录树为空。通过页面切换至“全域”后可见 0 行，并显示“无权限或无匹配当前有效文件”。
- A/B 分别在真实浏览器导航既有文件详情 `2054545668044084024` 和 `2054545668044084022`。详情自然读取均 HTTP 200 / business code `1080000012`，UI 明确提示 `Current user cannot access this controlled file`；文件编号与受控文件详情/负责人快照受限。页面没有可见批准、拒绝、签名动作；没有点击写操作。
- A/B 均从真实侧栏进入审批中心待办；模块和任务查询为 HTTP 200 / code 0。页面稳定空态“暂无审批任务”，0 行、无审批动作。
- 因为没有可见的本人部门授权文件或本人待办，本轮无法证明 A/B 对各自部门文件的正向可见性/任务动作。故权限边界总体状态为 `BLOCKED_POSITIVE_SCOPE_NOT_AVAILABLE`；已观察到的负向文件拒绝不能代替正向授权验收，也不能据此断言部门负责人配置错误。

### 证据
- 综合结果：`run-2026-09-27T22-20-34-232Z/result.json`。
- 页面截图均位于同一 run 目录，包括 A/B 独立新密码登录、受控浏览全域空态、两份既有详情拒绝和待办稳定空态。
- 全程未使用直接 API/fetch、数据库、Git 或服务重启；只有登录改密通过页面自然发出的授权写请求。没有提交签名、审批、分发、作废或其它 DCC 业务动作。

## 2026-09-28 当前轮
- 当前用户已明确授权 admin 通过真实用户列表“重置密码”页面为 Owner A/B 设置强随机临时密码；此授权 supersede 下文记录的早期限制。
- 之后只允许 Owner 本人在真实登录/强制改密 UI 中设置独立最终密码；如没有该交互界面，则保留 BLOCKED，不以其他重置路径替代。
- 成功认证后才执行 Owner A/B 的 DCC 只读权限边界 E2E。全过程禁止直接 API/fetch、数据库、Git 和业务写操作。
- admin 真实 UI 对 A/B 分别执行密码重置；两次自然 `PUT /admin-api/system/user/update-password` 均 HTTP 200，成功 toast 均观察到并消失，用户行保持可见。实际 reset prompt 只有新密码字段，没有安全理由字段。
- A/B 分别用各自临时密码从独立真实登录页面提交后，均停留在 `/login` 并显示首次/重置后必须修改密码的提示。两页均只有一个普通登录密码框，没有本人改密表单；所以没有最终密码提交，也没有任何 Owner 权限页访问。
- 当前轮结果：密码重置阶段 PASS；Owner 自助改密阶段 BLOCKED；权限边界 E2E BLOCKED_UPSTREAM_AUTH。不能把未登录账号的页面权限推断为通过或失败。
- 证据：`admin-reset-permission-2026-09-28/result.json`。A/B 重置后列表截图 LastWriteTime 分别为 2026-09-28 04:28:50 和 04:29:05；Owner 登录截图及脱敏 JSON 为 04:31:45。JSON 不含密码、token、cookie 或响应体。
- 未使用 `fetch`、APIRequest、axios、curl、数据库写入、Git、短信重置或第二次 admin 重置；未修改部门负责人/角色，未触碰 DCC 业务文件。
- 以下 BLOCKED 结论记录的是前序尝试，不与当前轮成功执行 admin 重置矛盾；当前轮的具体证据以上述 run 文件为准。

## 2026-09-28 授权续验结论
- Owner A 与 Owner B 均经真实 Playwright 登录页提交凭据，收到 HTTP 200 / 业务码 `1002000011`，页面提示首次/重置后必须修改密码。
- 页面实际只有普通登录表单（一个密码输入框），没有新密码与确认密码的首次改密表单；两个账号均未登录成功，未设置新密码。
- 没有触发短信找回或验证码。该路径需要手机号和 SMS code，且不等价于后端要求的首次改密；依照用户当轮指示立即停止。
- 受控浏览、详情、审批中心待办和允许/拒绝动作投影均未执行，权限结论为 `BLOCKED_UPSTREAM_AUTH`，不能标记为 PASS 或 FAIL。
- 新证据：`doc/tasks/20260928-dcc-owner-permission-e2e/first-password-live-2026-09-27T20-16-39-408Z/result.json`；页面截图：`ownerA-forced-password-page.png`、`ownerB-forced-password-page.png`。
- 运行后端 `48062` health 为 `UP`。未调用直接 API、`fetch`、APIRequest；未写数据库、未执行 Git、未重启服务。

## 阻塞依据
- `AdminAuthServiceImpl.authenticate` 对 `INITIAL`/`RESET_REQUIRED` 在创建登录 token 前返回 `AUTH_LOGIN_PASSWORD_CHANGE_REQUIRED`。
- 当前登录页面没有首次改密控件。个人中心 `update-password` 需要已认证会话；短信 reset 需要手机号与验证码，且本轮未获准继续该流程。
- 因此在不绕过登录、不使用短信 OTP、不直接改库/API 的约束下，无法完成首次改密，也无法继续两名负责人各自的权限边界 E2E。

## Scope
真实 Playwright 只读权限边界验证，租户“芋道源码”，前端 `8062`，后端 `48062`。本轮未使用 `fetch`、APIRequest、数据库写入、业务文件写入、负责人配置修改或 Git 操作。

## Result
- Owner A: `BLOCKED` before DCC navigation. A fresh real login returned HTTP 200 with business code `1002000011`; the page visibly required a password change after first login/reset.
- Owner B: `BLOCKED` before DCC navigation for the same explicit condition.
- After the user explicitly authorized password-change UI work, Playwright opened the real “忘记密码” page. It required a mobile number and SMS code; no authorized phone number or received code was available. The script did not request an SMS, submit a reset, or change either account.
- Neither identity reached an authenticated page. DCC visibility, cross-department task ownership, action projection, and 403/business-denial checks were not executed and must not be inferred.

## Evidence
- Combined result: `doc/tasks/20260928-dcc-owner-permission-e2e/evidence-2026-09-27T19-43-04-133Z/result.json`
- Owner A result: `doc/tasks/20260928-dcc-owner-permission-e2e/evidence-2026-09-27T19-43-04-133Z/ownerA-result.json`
- Owner B result: `doc/tasks/20260928-dcc-owner-permission-e2e/evidence-2026-09-27T19-43-04-133Z/ownerB-result.json`
- Login-page screenshots: `ownerA-last-page.png`, `ownerB-last-page.png` in the same evidence directory.
- First-password UI recheck: `doc/tasks/20260928-dcc-owner-permission-e2e/first-password-ui-2026-09-27T20-05-27-840Z/result.json`.
- Recheck screenshots: `ownerA-login-result.png`, `ownerA-reset-password-requirements.png`, `ownerB-login-result.png`, `ownerB-reset-password-requirements.png` in the recheck evidence directory.

## Review
The blocker is explicit and reproducible for both supplied identities. It is not a 403, not an empty DCC page, and not evidence of incorrect department authorization. The UI exposes SMS-based password reset but not an authenticated first-login change flow; continuing through reset requires an authorized mobile number and SMS code. No alternate route was used.

## Credentials Handling
No password, token, cookie, or secret response content is included in this report.
