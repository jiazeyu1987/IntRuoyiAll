# IntRuoyi E2E Rules

## 触发场景

- 编写、修改、运行或评审 Playwright E2E、真实用户路径验证、截图验收、登录后联调时，必须先读取本文件。
- 涉及登录、租户、账号时，还必须读取 `docs/login-access.md`。
- 涉及本机端口或 worktree 端口时，还必须读取 `docs/local-runtime.md` 和 `docs/worktree-restrictions.md`。

## 基本规则

- E2E 必须使用 Playwright 操作真实前端页面。
- API 只能用于最终状态核验或只读辅助检查，不得代替真实用户路径。
- 默认本机入口为 `http://localhost:8081` 或 `http://127.0.0.1:8081`。
- 写入型 E2E 必须使用已确认的测试租户和账号，并创建带任务标识、可追踪、可清理的数据。
- 只读验证必须说明使用的数据来源和只读范围。

## 缺入口处理

- 发布、审计或独立验证任务发现前端无入口时，必须 fail fast，不得临时扩大范围新增入口。
- 功能或修复任务只有在入口属于用户批准范围，且已完成 BDD + TDD 时，才允许补入口。

## 2026-07-25 E2E 请求失败分层门禁

- Trigger: Playwright `requestfailed` 中出现 `hm.baidu.com`、`api.iconify.design`、统计脚本、Iconify 图标外部请求 `net::ERR_ABORTED`，但目标本机页面和 `/admin-api/` 业务请求已完成。
- Preflight check: 真实 E2E 采集 `pageerror`、console error、requestfailed 时，必须按 URL 分层统计：本机入口、`localhost`、`127.0.0.1`、`/admin-api/` 属于业务访问；第三方统计、图标 CDN 等外部请求单独记录。
- Blocker: 任一本机页面资源、`/admin-api/` 请求、页面异常或控制台错误失败时必须失败；不得因为外部请求存在就宣称业务页面已完整通过。
- Verification: 证据 JSON 同时包含 `requestFailures`、`localRequestFailures`、`externalRequestFailures`，且最终断言 `localRequestFailures=[]`、`pageErrors=[]`、`consoleErrors=[]`。
- Forbidden action: 禁止直接清空全部 `requestfailed`、禁用错误采集、关闭网络失败监听或把 API-only 验证冒充页面通过。
- Evidence: `doc/tasks/20260725-process-flow-tab-e2e-fix/verification-report.md`。
## 禁止做法

- 禁止 mock 数据冒充真实 E2E。
- 禁止 API-only 代替前端路径。
- 禁止直接 SQL 或接口直塞绕过页面。
- 禁止修改生产租户、admin 基线数据或无关真实业务记录。
- 禁止为了测试额外添加产品上不需要的前端控件。

## 验证方式

- 记录 Playwright 命令、入口 URL、租户/用户标签、目标页面和关键断言。
- 写入型 E2E 记录测试数据标识和清理方式。
- 失败时记录实际失败位置、页面状态、网络响应或控制台错误。

## 表格行定位

- 当页面对列表进行本地排序、过滤或虚拟渲染时，Playwright 必须按页面可见的业务唯一文本定位目标行，再操作同一行的复选框或按钮。
- 不得直接用 API 返回数组下标映射前端表格行；接口排序和页面排序可能不同，会误选冻结行、错误行或无关业务数据。
