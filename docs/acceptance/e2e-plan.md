# Codex 测试管理 E2E 计划

## Purpose and Scope

本文档定义测试管理功能的真实用户路径验收。E2E 必须使用 Playwright 操作真实前端页面，从登录、菜单进入、测试项维护、租户选择、执行按钮到结果查看全链路验证。API 只能用于最终核验或只读辅助检查。

## Evidence Reviewed

- `docs/e2e-rules.md`：E2E 必须使用 Playwright 真实前端路径。
- `docs/login-access.md`：本机入口、默认登录来源和凭据不外泄规则。
- `IntRuoyiFronted/tests/e2e/system-config-package-real.e2e.js`：系统管理真实页面 E2E 模式。
- `IntRuoyiFronted/tests/e2e/mes-schedule-order-replan-881mo090863-real-flow.e2e.js`：排产工单手动重排真实流程参考。
- `IntRuoyiFronted/package.json`：Playwright 依赖和 E2E script 组织方式。

## User Paths

- 路径 1：测试管理员登录后进入 `系统管理 > 测试管理`。
- 路径 2：新增测试项，填写自然语言测试方法和多个期待检查点。
- 路径 3：编辑测试项，修改用户手写工单号并确认保存。
- 路径 4：选择顶层测试租户，执行单个测试项。
- 路径 5：选择多个并行安全测试项，发起并行执行。
- 路径 6：查看执行详情，确认通过检查点绿色勾和失败检查点红色叉。
- 路径 7：打开失败截图预览，确认截图来自后端临时 artifact 接口。
- 路径 8：使用非测试管理员账号登录，确认菜单不可见且接口权限失败。

## Browser or Client Steps

1. 使用 Playwright 打开 `http://localhost:8081/login?redirect=/index`。
2. 选择测试租户或默认本机租户，输入已授权的测试管理员账号。
3. 等待登录成功并进入首页。
4. 从真实侧边栏展开 `系统管理`，点击 `测试管理`。
5. 等待测试管理页面加载，断言表格、租户选择器和新增按钮可见。
6. 点击新增，填写测试项名称、自然语言方法、测试数据文本。
7. 在检查点编辑器中新增多条期待结果，保存并回到列表。
8. 选择顶层测试租户，勾选测试项，点击执行。
9. 等待执行批次出现在执行记录中。
10. 等待 Runner 回写结果，打开详情抽屉。
11. 断言每个检查点有明确状态文本；通过为绿色勾，失败为红色叉。
12. 对失败检查点点击截图，确认图片可显示或过期错误可显示。
13. 使用非授权账号重复登录，确认菜单不可见。

## API Verification

- 登录后只读调用权限信息接口，确认 `permissions` 包含或不包含 `system:codex-test:query`。
- 执行完成后只读调用执行详情接口，确认批次 `targetTenantId` 与页面选择一致。
- 对失败截图只读调用 artifact 接口，确认 HTTP 状态与 UI 展示一致。
- 对 Runner 回写结果不使用 API 替代页面操作；API 仅验证最终状态和证据完整性。

## Console and Log Checks

- Playwright 采集 `pageerror` 和 console error，执行结束时断言没有未解释的前端错误。
- 记录失败时页面 URL、执行批次 ID、测试项 ID、检查点名称和截图路径。
- 网络响应中 `/admin-api/system/codex-test-*` 失败时记录业务码和 message。
- 日志和控制台输出不得包含密码、token、cookie 或 Authorization 明文。

## Test Blockers

- 本机前端或后端未运行时，真实 E2E 阻塞。
- 目标测试租户或测试管理员账号缺失时，真实 E2E 阻塞。
- Runner 未在线或缺少 Playwright 浏览器时，执行路径 E2E 阻塞。
- 如果目标业务测试项使用用户手写工单号且工单不存在，则该业务检查点按失败或阻塞记录，不能改用其他工单号。

