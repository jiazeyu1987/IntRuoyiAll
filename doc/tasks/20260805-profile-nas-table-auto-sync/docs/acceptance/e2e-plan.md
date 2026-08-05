# NAS 表格自动同步 E2E Plan

## Purpose and Scope

真实 E2E 覆盖用户从个人工作台进入配置页签、打开“NAS表格自动同步”、保存配置、测试 NAS 写入和查看运行日志的路径。API 只用于最终辅助核对，不替代页面操作。

## Evidence Reviewed

- worktree 已登记 `int_main slot=7`：frontend `8088`、backend `48088`。
- 项目 E2E 规则要求 Playwright 操作真实前端。
- 个人工作台已有统一列表真实路径 E2E 可作为登录和入口风格参考，具体文件在前端 `tests/e2e` 下按 `profile-unified-*-real.e2e.js` 匹配。

## User Paths

- 登录具备 `mes:pro-batch-record-execution:golden-finger` 的测试账号。
- 进入个人工作台 `/profile`。
- 点击顶层“配置”页签。
- 点击内部“NAS表格自动同步”页签。
- 选择至少一个 ERP 表、设置每日开始时间、填写 NAS 目录，保存配置。
- 点击“测试NAS写入”，看到成功路径或明确失败。
- 点击“立即执行一次”，在日志中看到本次 run 成功或明确失败。

## Browser or Client Steps

- 使用 Playwright 启动浏览器访问 `http://127.0.0.1:8088`。
- 监听 console error、pageerror、目标接口请求和写请求。
- 所有配置写入必须通过页面按钮触发，禁止直接 API-only 创建配置。

## API Verification

- 页面保存后，可只读调用 `/erp/nas-table-sync/plan/get` 核对 `enabled`、`dailyStartTime`、`syncTypes`、`jobId`。
- 执行后，可只读调用 `/erp/nas-table-sync/run/page` 核对最新 run 与页面一致。

## Console and Log Checks

- console error/pageerror 为 0，或只记录与目标链路无关且按规则归因的外部资源异常。
- 后端 run 失败时必须有 `failureMessage`，前端必须可见。
- 目标写请求数量限定为保存、测试写入、立即执行三类。

## Test Blockers

- 缺登录账号、验证码未关闭、NAS 配置不可写、前后端端口未按 slot 7 启动，均阻塞真实 E2E。
- 如果 NAS 环境只允许失败验证，则 E2E 可通过“失败可见且无假成功”验收，但不得写成 NAS 成功。
