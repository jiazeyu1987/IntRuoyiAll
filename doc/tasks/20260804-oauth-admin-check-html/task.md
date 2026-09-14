# Task: OAuth 管理员权限检查 HTML 产物

## Task Goal

保存一个可运行的生产版 OAuth 管理员权限检查 HTML 文件，并补齐本地/生产可启动的同源授权辅助服务，避免直接 `file://` 打开时出现不明确的 `Failed to fetch`。

## Milestones

- [x] 明确产物范围：HTML 不把客户端密钥写入浏览器代码。
- [x] 写入 HTML 文件到 `output/oauth-admin-check-production.html`。
- [x] 验证 HTML 文件存在、UTF-8 可读且包含生产版接口入口。
- [x] 补齐零依赖 Node 授权辅助服务 `output/oauth-admin-check-helper-server.mjs`。
- [x] 让 HTML 在 `file://` 直接打开时提示正确启动方式。
- [x] 使用真实浏览器完成本机 OAuth 授权码 E2E。

## Expected Verification

- 使用 UTF-8 方式读取 `output/oauth-admin-check-production.html`。
- 检查文件包含 `/auth/login`、`/api/me`、`/auth/logout` 三个生产版服务端入口。
- 使用 `node --check output\oauth-admin-check-helper-server.mjs` 校验辅助服务语法。
- 使用 Playwright 真实浏览器完成 helper 首页 -> 授权登录 -> IntRuoyi SSO -> helper 权限展示链路。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，采用生产授权码模式前端产物，避免在浏览器暴露 OAuth 客户端密钥。
- `是否存在临时补丁或绕过`：否。

## Experience Gate Summary

- 已读取 `docs/task-closeout-rules.md` 与 `docs/powershell-encoding.md`。
- 已读取 `docs/experience-index.md`；本次仅生成静态 HTML 产物，未命中需要额外打开的业务门禁文档。

## Verification Result

- `Get-Content -Encoding utf8 -Raw output\oauth-admin-check-production.html` 标记检查通过。
- 文件包含 `/auth/login`、`/api/me`、`/auth/logout`、`hasAdminPermission`、`ADMIN_ROLE_CODES`。
- HTML 包含 `file://` 模式提示，不包含 `OAUTH_CLIENT_SECRET` 或 `client_secret` 标记。
- 辅助服务文件 `output\oauth-admin-check-helper-server.mjs` 已生成，启动时 fail fast 检查必需环境变量。
- `node --check output\oauth-admin-check-helper-server.mjs` 通过。
- `node --check doc\tasks\20260804-oauth-admin-check-html\oauth-admin-check-real-e2e.cjs` 通过。
- `git diff --check -- output\oauth-admin-check-production.html doc\tasks\20260804-oauth-admin-check-html\task.md doc\tasks\20260804-oauth-admin-check-html\execution-log.md` 通过。
- 使用 Playwright 真实浏览器访问 `http://127.0.0.1:18080`，点击“授权登录”，经本机 `http://127.0.0.1:8081/sso` 和默认本机身份完成授权后返回页面，并断言“管理员权限：有”。
- helper 必须配置 `INT_TENANT_ID`，并在 OAuth token 交换与权限信息接口请求中传递 `tenant-id`。
- GREEN: `node doc\tasks\20260804-oauth-admin-check-html\oauth-admin-check-real-e2e.cjs` -> PASS，结果文件 `output/playwright/20260804-oauth-admin-check-html/result.json`，最终页面显示 `管理员权限：有`，命中角色 `super_admin`，权限数量 `1282`。

## E2E Verification Scope

- 使用本机前端 `http://127.0.0.1:8081` 与本机后端 `http://127.0.0.1:48081/admin-api`。
- 使用 `IntRuoyiFronted/.env` 中的默认本机身份，仅记录身份标签，不记录密码或 token。
- OAuth client secret 仅由 E2E 脚本内存读取并传入 helper 子进程，不写入 HTML、结果 JSON 或任务日志。
- RED: 首次授权码回调已取得 `code`，但 token 交换返回“请求的租户标识未传递”，证明生产 helper 缺少正式多租户请求头。
- 本机运行态缺少示例默认的 `admin-check-prod` OAuth client；本轮 E2E 显式使用已有系统种子客户端 `yudao-sso-demo-by-code` 验证同一授权码 + refresh token + `user.read` + helper callback 链路。

## Cleanup Keep

- doc/tasks/20260804-oauth-admin-check-html/oauth-admin-check-real-e2e.cjs
- output/playwright/20260804-oauth-admin-check-html/result.json
- output/playwright/20260804-oauth-admin-check-html/initial-not-logged-in.png
- output/playwright/20260804-oauth-admin-check-html/admin-permission-pass.png
