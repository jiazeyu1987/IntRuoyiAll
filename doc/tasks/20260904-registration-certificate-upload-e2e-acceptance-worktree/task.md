# 注册证上传 E2E 验证任务

## Task Goal
在 worktree 环境中，根据 `e2e_test/registration/upload/registration-certificate-upload-e2e-acceptance.md` 对注册证上传正确分支执行 E2E 验证，逐项记录用例结果；根据用户后续授权，修复阻断前端 E2E 主链路的问题，并重新通过真实前端验证。

## Current Status
blocked

## Scope
- 验证对象：注册证上传、审批、当前列表、附件下载、下载申请及有效期相关验收流程。
- 运行环境：`D:\IntRuoyiWorktree\20260904-dcc-upload-related-files-e2e-worktree`。
- 约束：仅通过真实前端和 Playwright 执行业务动作；不直接调用业务 API、数据库或 `fetch/apiGet` 承担验收动作；业务代码修改仅限用户明确要求的修复范围；不提交 Git。

## Milestones
- [x] 读取项目规则、验收文档和运行环境约束。
- [x] 完成 worktree 前端、后端、上传文件、账号入口和浏览器前置检查。
- [x] 执行并记录 E2E-1、E2E-2、E2E-3、E2E-4、E2E-6、E2E-7、E2E-8、E2E-9。
- [x] 对失败或阻塞场景做代码分析。
- [x] 修复用户确认范围内的阻断问题并补充回归测试。
- [x] 通过真实前端重新执行注册证上传审批主链路。
- [x] 输出 `verification-report.md` 并汇总最终结论。

## Expected Verification
- 每个 E2E 用例都有 PASS / FAIL / BLOCKED 结果、证据和失败原因。
- 失败原因基于真实页面表现、Playwright 证据、后端日志和代码分析。
- 用户授权范围内的修复需有单元回归和真实前端 E2E 证据。

## Blockers
- 当前注册证上传审批主链路、审批前后状态、生产方式回显、注册经理直接下载无阻塞：最终完整脚本使用 `E2E-UPLOAD-20260904101823-SELF` 和 `E2E-UPLOAD-20260904101823-ENTR` 两组真实前端数据验证通过。
- E2E-8 仍阻塞：验收要求普通用户 C 进入详情、无直接下载特权、提交下载申请并由注册经理审批；当前上下文没有可通过前端确认并登录的同租户普通用户 C 凭据，不能用 API/DB 或相似账号替代。
- E2E-9 仍阻塞：依赖 E2E-8 已完成授权，并要求超过 3 天后重新申请；当前没有自然等待 3 天的结果，也没有产品认可的业务日期推进入口证据，不能直接改库或调用接口推进时间。
- 历史阻塞已分析并记录在 `execution-log.md` 与 `verification-report.md`：旧运行 Jar 嵌入过期 DCC 模块、旧公司授权要求、提醒接收人读取旧 Quartz 参数、签名图片前置条件、审批待办可见性。
