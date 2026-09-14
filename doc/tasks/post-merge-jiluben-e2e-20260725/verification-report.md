# Verification Report

## Summary

本任务完成 `jiluben_20260722_clean` 融合后记录本/eDHR 相关真实验证闭环。真实 E2E 使用管理员只读模式访问本机 `int_main` 前端 `http://localhost:8081` 和后端 `48081`，覆盖批次详情、右侧同工序表单列表、历史执行 tracking 展示和返回批次详情上下文。

## Environment

- Branch: `int_main`。
- Frontend: `http://localhost:8081`。
- Backend: `http://127.0.0.1:48081` health PASS。
- Backend runtime note: 未改配置文件；临时 JVM 参数指向 Docker MySQL `127.0.0.1:23306` 和 Redis `127.0.0.1:26379`。
- E2E account label: 管理员只读复验，租户 `芋道源码`，用户 `admin`；密码仅通过环境变量传入，未写入文档。

## Results

- PASS: `node tests\e2e\edhr-batch-process-companion-forms-real.e2e.js`，管理员只读结构化模式，batch `900000000505`，routeProcessId `923557`。
- PASS: `node tests\e2e\edhr-open-process-form-route-static.spec.js`。
- PASS: `node tests\e2e\edhr-batch-process-companion-forms-static.spec.js`。
- PASS: `node tests\e2e\edhr-batch-detail-hide-red-box-static.spec.js`。
- PASS: `node tests\e2e\mes-edhr-batch-review-signoff-summary-static.spec.js`。
- PASS: `node tests\e2e\edhr-recordbook-batch-sync-static.spec.js`。
- PASS: `node tests\e2e\edhr-batch-process-state-background-static.spec.js`。
- PASS: `node --check tests\e2e\edhr-batch-process-companion-forms-real.e2e.js`。
- PASS: `pnpm ts:check`。
- PASS: `git diff --check`。
- PASS: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1`。

## Fixes Verified

- `ExecutionPage.vue` toolbar 对填写模式和 tracking 模式都可见，避免填写页标题/返回按钮被 tracking 条件隐藏。
- 真实 E2E 不再手写旧填写 URL 打开历史执行；历史执行记录使用正式 `viewMode=tracking` 只读路径，避免绕过 `openEdhrBatchTask` 的当前活动表单语义。
- 伴随表单静态合同同步当前 `resolveTaskGateText` 门禁 resolver 与 `readRouteQueryString` 返回上下文逻辑。

## Remaining Notes

- 测试租户 `aoteman` 对历史执行 `BATCH_RECORD_EXECUTION:1276` 无 VIEW 权限，且直接打开旧 `executionId` 填写路径会触发“非当前活动表单”。这不是本次产品缺陷；只读历史审计路径已用管理员只读 tracking 模式覆盖。
- 写入型 E2E 仍需任务专用测试数据和环境变量；本任务未执行写入型流程，且目标真实 E2E 断言未产生 MES 写请求。