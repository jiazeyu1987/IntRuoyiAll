# Execution Log

BDD: 签名记录动作显示中文 -> Given 后端返回 actionType 为 `APPROVE`、`SUBMIT`、`FORM_REVIEW`、`FIELD_CHANGE` 的真实签名记录 / When 用户打开 eDHR 签名记录页 / Then 动作列显示 `审批通过`、`提交审批`、`表单复核`、`字段变更`，不直接显示英文动作编码。

BDD: 签名记录时间显示年月日 -> Given 后端返回 `signedAt` 为毫秒时间戳或可解析时间 / When 用户查看签名记录列表 / Then 签名时间显示为 `YYYY年M月D日` 格式，不显示原始毫秒值。

RED: `node tests\e2e\edhr-tracking-signature-real-flow.e2e.js --static-display-format` -> FAIL，预期原因：`SignaturePage.vue` 缺少 `SIGNATURE_ACTION_LABELS`，动作列仍直接展示 `actionType`。

GREEN: `node tests\e2e\edhr-tracking-signature-real-flow.e2e.js --static-display-format` -> PASS，源码检查确认动作列使用中文映射函数，签名时间列使用 `YYYY年M月D日` 格式化函数。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS，签名记录页和 E2E 类型相关改动通过 relaxed 类型检查。

GREEN: `node --check tests\e2e\edhr-tracking-signature-real-flow.e2e.js` -> PASS，真实 E2E 脚本语法检查通过。

GREEN: `node tests\e2e\edhr-tracking-signature-real-flow.e2e.js` with `EDHR_TRACKING_SIGNATURE_PASSWORD=<redacted>` and evidence file override -> PASS，真实测试租户 Playwright 路径验证追踪、详情、签名记录和动作筛选通过，证据写入 `doc/tasks/20260611-edhr-signature-display-format/real-e2e-evidence.md`。
