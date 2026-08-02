# 执行日志：DCC 文控签核追溯真实 E2E 验证

## User Intent

- 用户要求在 `E:\IntRuoyi` 对 DCC 文控“签核追溯”做真实 Playwright E2E 验证。
- 用户补充要求：只验证本场景，不顺手修其它场景；缺页面入口、权限、测试数据或运行态问题先记录 BLOCKED 和影响，不用 API-only、SQL 改状态或 admin 账号绕过。

## Rule Reads

- 已读取 `AGENTS.md`。
- 已读取 `docs/e2e-rules.md`。
- 已读取 `docs/login-access.md`。
- 已读取 `docs/frontend-development.md`。
- 已读取 `docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md`、`docs/database-rules.md`、`docs/powershell-encoding.md`、`docs/task-closeout-rules.md`。
- 已读取 Playwright skill：`C:\Users\BJB110\.codex\skills\playwright\SKILL.md`。
- 已读取 `docs/experience-index.md` 并命中 DCC 文控审批、Playwright 浏览器、目标链路归因和 artifact 清理门禁。

## BDD

- BDD: DCC 签核追溯页面可见 -> Given 非 admin 上传人通过真实页面准备任务自有受控文件并完成原版发布或升版审批签名链路 / When DCC/QA/文控查看账号进入受控文件详情并打开签核追溯、审批记录、操作日志或版本历史入口 / Then 页面必须能看到上传人、每级审批人、签名人、签名时间、签名结果和审批意见。
- BDD: 页面证据与只读后端一致 -> Given 页面展示签名证据、文件 hash、stampedFileId 和 publishedFileId 或等价证据 / When 使用只读 API/DB 核验同一文件 ID 和版本 / Then 页面展示的人、时间、任务状态、签名证据、文件 hash/发布文件 ID 必须一致。
- BDD: 受控签名失败诊断可见 -> Given 一个缺签名授权或签名密码错误的非主链路账号 / When 在不破坏主链路的前提下尝试签名 / Then 页面必须明确提示失败原因；若系统无入口或前置权限缺失，记录 E2E BLOCKED。

## Preflight Log

- `git status --short --branch`：当前 `int_main` 工作区已有大量非本任务脏改动；本任务不修改其代码，不进行基线提交或修复。
- `Get-Command npx`：本机 `npx` 可用，路径为 `D:\Programs\npx.ps1`。
- 本机前端 `http://127.0.0.1:8081/`：HTTP 200。
- 本机后端 `http://127.0.0.1:48081/actuator/health`：`status=UP`。
- 端口归属：8081 为 `E:\IntRuoyi\IntRuoyiFronted` 的 Vite `env.local`；48081 为 `E:\IntRuoyi\output\runtime\int_main` 的后端运行 Jar。后端命令行包含 datasource secret，未写入日志。
- Playwright 浏览器：本机 Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe` 可用。
- 非 admin 账号前置参考：既有 `20260802-dcc-five-account-role-setup` 已记录 `pengyunfeng`、`zhaohaichen`、`zhaojie`、`zhaomingyu`、`wangsiyu` 五个非 admin 账号、角色和审批路线前置通过。
- 可复用任务自有文件参考：既有 `20260802-dcc-upload-original-e2e` 已记录原版受控文件 `2054545668044070262`；既有 `20260802-dcc-upload-revision-e2e` 已记录 V2 升版受控文件 `2054545668044070261`。
- 密码环境变量检查：当前进程环境变量名称中未发现 `DCC_E2E_PASSWORD` 或本场景专用非 admin 密码变量；按用户要求不能在命令中写明文密码或改用 admin。

## Verification Evidence

- E2E BLOCKED：未执行登录、上传、审批签名、追溯页面查看、导出/打印或最终 API/DB 核验。
- 未使用 API/SQL 制造审批记录或签名记录。
- 未使用 admin 账号验证页面。
- 未把既有历史结果冒充为本轮页面追溯通过。

## Blockers

- BLOCKED: `DCC_E2E_PASSWORD` 或等价本场景非 admin 密码环境变量缺失。
- Impact: 无法登录上传人、审批/签名人或 DCC/QA/文控查看账号，无法进入受控文件详情页查看签核追溯/审批记录/操作日志/版本历史，无法证明页面上用户能看到上传人、审批人、签名人、签名时间、签名方式、证据状态、文件 hash、盖章文件或发布文件。

