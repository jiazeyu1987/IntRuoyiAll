# DCC 产品目录项目排序真实 E2E 验证

## Task Goal

通过真实前端页面验证 DCC 产品目录点击“项目名称 / 项目代码”降序后，空项目字段记录排在非空记录之后。

## Milestones

- [x] 确认 E2E、登录、运行态和端口规则
- [x] 启动或确认 D-Main 前后端运行态
- [x] 执行真实 Playwright 页面验证
- [x] 记录验证证据并完成收尾

## Expected Verification

- `http://127.0.0.1:8101` 前端可访问。
- `http://127.0.0.1:48101/actuator/health` 后端为 `UP`。
- Playwright 登录 `芋道源码/admin`，进入正式菜单路由 `/mdm/product-catalog`。
- 点击“项目名称”和“项目代码”表头降序，真实请求包含 `sortField` / `sortOrder=desc`，响应和页面首屏非空项目字段排在空字段之前。

## Current Status

completed

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否，本任务只读验证，不修改产品代码。
- 是否从根因和长期维护角度解决：是，验证已交付的后端空值标记排序在真实页面链路生效。
- 是否存在临时补丁或绕过：否。

## Cleanup Candidates

- doc/tasks/20260730-dcc-product-catalog-sort-real-e2e/e2e-product-catalog-sort.mjs
- doc/tasks/20260730-dcc-product-catalog-sort-real-e2e/artifacts
- doc/tasks/20260730-dcc-product-catalog-sort-real-e2e/backend-runtime.out.log
- doc/tasks/20260730-dcc-product-catalog-sort-real-e2e/backend-runtime.err.log
- doc/tasks/20260730-dcc-product-catalog-sort-real-e2e/frontend-runtime.out.log
- doc/tasks/20260730-dcc-product-catalog-sort-real-e2e/frontend-runtime.err.log
- IntRuoyiBackend/hs_err_pid14876.log
- IntRuoyiBackend/replay_pid14876.log

## Final Verification

- 真实 E2E 已通过：`项目名称` 与 `项目代码` 降序均验证 115 条非空项目字段排在 98 条空项目字段之前，总记录数 213。
- 页面安全证据：`writeRequests=[]`，`pageErrors=[]`。
- task-closeout-cleanup preview/apply 已通过，临时脚本、截图、运行日志和 JVM crash/replay 文件已清理，仅保留 `task.md`、`execution-log.md`、`verification-report.md`。
- D-Main 任务自有运行态已停止，`8101` 和 `48101` 端口已释放。
