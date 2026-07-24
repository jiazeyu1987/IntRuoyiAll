BDD: 打开工序进入填写界面 -> Given 用户在批记录详情页选中当前工序 / When 点击打开工序、打开填写或打开返工 / Then 前端先调用 `openEdhrBatchTask`，再跳转 `/mes/pro/feedback/edhr-execution/form` 并携带 `id/executionId/workTaskId/returnPath`。
BDD: 审签归档入口不受影响 -> Given 用户点击签名记录、审批记录或单表归档 / When 跳转到当前工序证据入口 / Then 仍保持此前详情/审签/归档定位逻辑。
GREEN: experience-preflight -> PASS，已读取 PowerShell、经验索引、前端交付与统一前端样式门禁；本轮不执行真实 E2E。RED: node tests/e2e/edhr-open-process-form-route-static.spec.js -> FAIL，handleOpenTask 仍跳转执行详情页。
GREEN: node tests/e2e/edhr-open-process-form-route-static.spec.js -> PASS。
GREEN: node tests/e2e/edhr-signature-change-execution-entry-static.spec.js -> PASS。
GREEN: node tests/e2e/edhr-side-action-buttons-static.spec.js -> PASS。