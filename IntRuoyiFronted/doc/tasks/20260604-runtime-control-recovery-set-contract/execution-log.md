# 执行日志：运行控制前端恢复集与兼容性证据展示

- BDD: 前端展示恢复集候选 -> Given 后端返回完整恢复集候选 / When 操作员打开运行控制台 / Then 页面展示恢复集 ID、状态、程序版本、Redis 策略、配置清单、manifest hash 和组件摘要。
- BDD: 前端标记发布已测试必须绑定恢复集 -> Given 操作员选择发布候选 / When 未选择恢复集候选 / Then 提交被阻断且不会发送降级请求。
- BDD: 前端展示回滚兼容性证据 -> Given 后端返回回滚候选 / When 操作员查看候选 / Then 页面展示兼容性状态、证据路径、检查时间和摘要。
- BDD: 前端文案表达正确操作边界 -> Given 操作员查看恢复与回滚操作 / When 页面渲染 / Then 文案体现恢复同一恢复集、兼容性成立后只回滚程序。

## Evidence

- 待补充 RED/GREEN/REGRESSION 记录。
- RESUME: 用户已明确要求继续实施恢复集计划，本任务解除阻塞并恢复为进行中。
- RED: `node tests/e2e/runtime-control-recovery-set-contract-static.spec.js` -> FAIL, expected reason: missing recovery-set request field, restore candidate fields, rollback compatibility fields, and UI display/copy.
- GREEN: `node tests/e2e/runtime-control-recovery-set-contract-static.spec.js; node tests/e2e/runtime-control-restore-target-static.spec.js; node tests/e2e/runtime-control-foolproof-static.spec.js; node tests/e2e/runtime-control-ops-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` -> PASS.
- GREEN: frontend feature evidence validator -> PASS.
- GREEN: UTF-8 readback -> PASS.
- GREEN: task-closeout-cleanup preview -> PASS, keep only; no delete, blocked or warnings.
- REGRESSION: `git diff --check` -> PASS.
- GREEN: `$env:RUNTIME_CONTROL_E2E_BASE_URL='http://localhost:8081'; $env:RUNTIME_CONTROL_E2E_VERIFY_TENANT='芋道源码'; $env:RUNTIME_CONTROL_E2E_VERIFY_USERNAME='admin'; $env:RUNTIME_CONTROL_E2E_VERIFY_PASSWORD='admin123'; node tests/e2e/runtime-control-yudao-admin-readonly.e2e.js` -> PASS, `AC-04 rollbackCandidates=22`, `AC-05 restoreCandidates=8`, `YUDAO_ADMIN_READONLY_PASS`; script asserted no non-GET runtime-control requests.
