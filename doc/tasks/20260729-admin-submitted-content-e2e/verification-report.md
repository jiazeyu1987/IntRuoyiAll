# Verification Report

## Scope

验证批记录管理员在批次执行主区域是否读取当前已提交内容；重点确认主区域不读取草稿、不使用未提交预览。

## Environment

- Frontend: `http://127.0.0.1:8081`, PID `39032`, `E:\IntRuoyi\IntRuoyiFronted` Vite.
- Backend: `http://127.0.0.1:48081`, PID `53320`, `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260729-093236.jar`.
- Tenant/User: `芋道源码/admin`.
- Data source: local Docker MySQL `int-ruoyi-mysql/ruoyi-vue-pro`.

## Results

- PASS: Static contract `node tests/e2e/edhr-batch-admin-preview-runtime-fix-static.spec.js` confirms the detail main area uses submitted execution reviews and no longer calls `task/preview` for main-area content.
- PASS: Login preflight succeeded through the real frontend login page with `芋道源码/admin`.
- PASS: Current screenshot batch E2E succeeded. For work order `881MO090935`, latest batch `900000000909`, task `7206`, execution `1589` is draft status `0`; admin main area shows `暂无已提交批记录内容`, does not render the readonly original sheet, does not request `/task/preview`, and sends no MES write requests.
- BLOCKED: Submitted-content E2E could not be completed because the only local submitted non-empty historical samples fail current backend `review-timeline` validation with `eDHR 批次执行缺少工艺流程批记录配置流程配置或默认批记录`.
- BLOCKED: User-provided `测试租户/auteman` login preflight failed with `登录失败，账号密码不正确`; password was used only as a runtime parameter and not recorded.

## Evidence

- Current unsubmitted artifact: `doc/tasks/20260729-admin-submitted-content-e2e/admin-submitted-content-e2e-output/admin-current-unsubmitted-main-area.json`.
- Current unsubmitted screenshot: `doc/tasks/20260729-admin-submitted-content-e2e/admin-submitted-content-e2e-output/admin-current-unsubmitted-main-area.png`.
- Submitted-content blocker command: `EDHR_ADMIN_SUBMITTED_VERIFY_MODE=submitted-content node doc/tasks/20260729-admin-submitted-content-e2e/admin-submitted-content-real.e2e.js`.

## Conclusion

当前截图里的主区域不显示内容是符合新规则的：该批次只有草稿执行记录，管理员主区域被验证为不读取草稿。代码静态合同显示已改为读取已提交 execution review；但“其他账号提交后管理员看到填写内容”的完整真实 E2E 仍阻塞，缺少当前可用的已提交样本或有效可登录的写入测试账号闭环。
