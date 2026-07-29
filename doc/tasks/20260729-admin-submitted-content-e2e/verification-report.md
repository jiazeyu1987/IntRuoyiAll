# Verification Report

## Scope

验证批记录管理员在批次执行主区域是否读取当前已提交内容；重点确认主区域不读取草稿、不使用未提交预览。

## Environment

- Frontend: `http://127.0.0.1:8081`, PID `39032`, `E:\IntRuoyi\IntRuoyiFronted` Vite.
- Backend: `http://127.0.0.1:48081`, PID `48740`, `E:\IntRuoyi\output\runtime\int_main` Java runtime.
- Tenant/User: `测试租户/aoteman`.
- Data source: local Docker MySQL `int-ruoyi-mysql/ruoyi-vue-pro`.

## Results

- PASS: Static contract `node tests/e2e/edhr-batch-admin-preview-runtime-fix-static.spec.js` confirms the detail main area uses submitted execution reviews and no longer calls `task/preview` for main-area content.
- PASS: Login preflight succeeded through the real frontend login page with `测试租户/aoteman`.
- PASS: Current screenshot batch E2E succeeded. For work order `881MO090935`, latest batch `900000000909`, task `7206`, execution `1589` is draft status `0`; admin main area shows `暂无已提交批记录内容`, does not render the readonly original sheet, does not request `/task/preview`, and sends no MES write requests.
- SUPERSEDED: User-provided `测试租户/auteman` login preflight failed with `登录失败，账号密码不正确`; user corrected the username to `aoteman`.
- PASS: Write-path E2E created a task-owned sample through the real frontend. `aoteman` filled `M7-EDHR-EXEC-BPM_REQUIRED-20260729ADMINSUBMIT15-已提交内容`, saved field audit changes, submitted execution `1605`, and completed BPM approval. Terminal evidence is `status=3/APPROVED`, `fieldAuditRevision=1`, and a non-empty `cell_values_json`.
- PASS: Submitted-content admin E2E opened batch execution `900000000925`, task `7269`, execution `1605`; the main readonly original sheet displayed the submitted text, the empty state was absent, `/task/preview` was not requested, and no MES write request was sent.

## Evidence

- Current unsubmitted artifact: `doc/tasks/20260729-admin-submitted-content-e2e/admin-submitted-content-e2e-output/admin-current-unsubmitted-main-area.json`.
- Current unsubmitted screenshot: `doc/tasks/20260729-admin-submitted-content-e2e/admin-submitted-content-e2e-output/admin-current-unsubmitted-main-area.png`.
- Submit/approval artifact: `doc/tasks/20260729-admin-submitted-content-e2e/admin-submitted-content-e2e-output/edhr-batch-execution-submit-review-20260729ADMINSUBMIT15-BPM_REQUIRED.json`.
- Submitted-content artifact: `doc/tasks/20260729-admin-submitted-content-e2e/admin-submitted-content-e2e-output/admin-submitted-content-main-area.json`.
- Submitted-content screenshot: `doc/tasks/20260729-admin-submitted-content-e2e/admin-submitted-content-e2e-output/admin-submitted-content-main-area.png`.

## Conclusion

PASS。当前能力已经能实现：其他账号在真实页面提交并审批完成后，批记录管理员主区域读取 `review-timeline` 返回的已提交 execution review，并显示提交后的单元格内容；本次验证未发现读取草稿、调用 `/task/preview` 或触发 MES 写请求。
