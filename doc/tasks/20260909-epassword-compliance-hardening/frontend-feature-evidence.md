# Frontend Feature Evidence

## Feature Goal and Non-goals

- 目标：正式电子签名弹窗不再允许用户选择或回填签名时间。
- 非目标：不重做页面布局，不删除历史签名记录中的旧字段展示。

## Acceptance

- 正式签名弹窗不得包含人工选择签名时间输入框。
- 正式签名弹窗不得收集人工签名时间原因。
- 前端正式签名 payload 不得携带 `selectedSignedAt`。

## UI Entry Points

- `src/views/mes/pro/edhr/ApprovalDetailPage.vue`
- `src/views/mes/pro/edhr/ExecutionPage.vue`
- `src/views/mes/pro/edhr/SignaturePage.vue`
- `src/views/mes/pro/edhr/signatureTime.ts`

## API Contracts and Data States

- 前端正式签名请求通过 `buildSignatureTimePayload()` 返回 `undefined`，不再提交 `selectedSignedAt`。
- 历史记录中的 `USER_SELECTED` 仅作为历史停用状态展示为“历史手动时间（已停用）”。

## BDD Scenarios

- BDD: 正式签名弹窗不允许人工选择时间 -> Given 用户打开 eDHR 正式签名弹窗, When 查看签名时间区域, Then 页面提示签名时间由系统自动生成，不显示人工时间选择框。
- BDD: 正式签名 payload 不携带人工时间 -> Given 用户提交正式签名, When 前端构造签名时间 payload, Then 返回 undefined，由后端生成签名时间。

## RED

- RED: `node tests/e2e/edhr-formal-signature-time-compliance-static.spec.js` -> FAIL，`ApprovalDetailPage.vue` 仍包含 `placeholder="可选择人工签名时间"`。

## GREEN

- GREEN: `node tests/e2e/edhr-formal-signature-time-compliance-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-signature-time-optional-static.spec.js` -> PASS。

## Verification

前端签名时间静态契约验证已通过；未执行真实 E2E。

## Responsive, Accessibility, Loading, Empty, Error, and Permission Checks

- 本次未新增复杂布局；原输入区替换为 Element Plus `el-alert` 信息提示。
- 权限、加载、空状态未改变。

## Blockers

- `node tests/e2e/edhr-tail-four-goals-static.spec.js` 当前被既有缺失路径 `src/views/mes/pro/route/RouteFlowConfigPanel.vue` 阻塞，未作为本次完成门禁。
