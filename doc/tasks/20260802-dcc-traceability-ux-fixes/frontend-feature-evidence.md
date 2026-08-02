# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 提升 DCC 受控文件签核追溯页面对上传、审批、签名、文件证据和异常诊断的业务可读性。
- Non-goal: 不改变后端审批/签名流程，不新增 API-only 路径，不修改其它 DCC 场景。

## Requirements And Acceptance IDs

- DCC-TRACE-UX-01: 签名留痕权限提示必须说明签核追溯摘要仍可查看。
- DCC-TRACE-UX-02: 目标文件操作日志空态必须指向签核追溯/生命周期证据。
- DCC-TRACE-UX-03: 签核追溯节点必须合并显示审批意见与签名证据。
- DCC-TRACE-UX-04: 发布/盖章文件证据必须提供页面可点击验证入口。
- DCC-TRACE-UX-05: 签名失败提示必须包含原因、处理建议和责任入口。

## UI Entry Points And Owned Files

- `/dcc/controlled-file/detail/:id`
- `/dcc/controlled-file/logs?controlledFileId=:id`
- `IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue`
- `IntRuoyiFronted/src/views/dcc/controlled-file/detail/approval-actions.ts`
- `IntRuoyiFronted/src/views/dcc/controlled-file/logs/index.vue`
- `IntRuoyiFronted/tests/e2e/dcc-traceability-ux-static.spec.js`

## API Contracts And Data States

- 复用 `ControlledFileVO.signatureSummaries[].comment`、`publishedFileId`、`stampedFileId`。
- 不新增后端 API，不改变请求 payload。
- 签名错误沿用既有 API 错误解析，不吞异常。

## BDD Scenarios

- 详见 `execution-log.md`。
- BDD: DCC traceability UX -> Given an ACTIVE controlled file with approval signatures When a non-admin user opens traceability Then the page shows uploader, approvers, approval comments, signature evidence, clickable file evidence, and contextual empty/error states.

## RED Command And Expected Failure

- RED: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> FAIL, old permission copy and missing traceability UX fields.
- `node tests/e2e/dcc-traceability-ux-static.spec.js` -> FAIL。
- Expected reason: 权限提示仍为旧文案，签核追溯表缺少审批意见和文件证据按钮。

## GREEN Command And Passing Result

- GREEN: `node tests/e2e/dcc-traceability-ux-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- `node tests/e2e/dcc-traceability-ux-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。
- 相关 DCC 静态回归 -> PASS。
- 真实 Playwright E2E -> `traceability-ux-real-e2e-result-20260802112712.json`，主链路 PASS。

## Verification

- Verification: static contract, type check, DCC static regression, real Playwright E2E, readonly API comparison, and password literal scan completed.

## UX Checks

- Permission: 静态合同 PASS；真实低权限触发 BLOCKED，原因是低权限账号看不到目标文件行。
- Empty state: 操作日志 `code=0,total=0` 时显示签核追溯/生命周期指引。
- Error state: 错误密码、缺授权、签名图片、证据快照失败文案均包含原因、处理建议和责任入口。
- E2E path: 非 admin `wangsiyu` 通过受控浏览进入详情，验证追溯表、导出、文件证据按钮、只读 API 一致性。

## Blockers And Follow-Up Skills

- Wrong-password real diagnostic is blocked unless a safe task-owned pending signature task exists.
- Low-permission prompt real trigger is blocked unless a user can both see the target file and lack advanced signature management permission.
