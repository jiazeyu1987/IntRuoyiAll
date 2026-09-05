# 注册证完整下载 E2E 修复与验证报告

## Objective

基于 `e2e_test/registration/download/registration-certificate-download-e2e-acceptance.md`，在独立 worktree `D:\IntRuoyiWorktree\20260905-registration-download-simulated-e2e-verify` 使用真实前端 `http://127.0.0.1:8158` 和后端 `http://127.0.0.1:48158` 复现、修复并复验注册证下载 E2E。2026-09-05 用户明确要求删除 E2E-6，因此当前验收范围为 E2E-1、E2E-2、E2E-3、E2E-4、E2E-5、E2E-7、E2E-8、E2E-9。

## Runtime Evidence

- Worktree: `D:\IntRuoyiWorktree\20260905-registration-download-simulated-e2e-verify`
- Branch: `codex/20260905-registration-download-simulated-e2e-verify`
- Frontend: `http://127.0.0.1:8158/` -> HTTP 200
- Backend health: `http://127.0.0.1:48158/actuator/health` -> `UP`
- Tenant/account labels: `芋道源码 / wanglixuan`, `芋道源码 / chudongchuan`

## Code Fixes

- 下载授权有效期：`DccRegistrationCertificateGrantService` 按 `approvedAt + 24h` 生成授权截止时间。
- OLD 详情可达性：`DccRegistrationCertificateQueryServiceImpl` 不再用旧证查看授权阻塞详情页打开，下载仍由文件级授权控制。
- 变更批件入口：注册证详情页仅对 `BOUND + APPLIED` 变更批件显示下载/申请下载，避免待审批变更文件触发后端归属冲突。
- OLD 详情版本隔离：OLD 详情只展示当前版本对应的变更履历，避免跨版本串显。
- OLD 失效命名：前端对 OLD 注册证主文件和同版本变更批件追加 `_已失效`，组合场景同时保留 `变更文件`。
- 变更审批摘要：变更申请写入 BPM 摘要所需的证件编号、分类、产品名称和所属企业名称，避免审批中心只显示技术 ID。

## Results

| Case | Result | Evidence |
| --- | --- | --- |
| E2E-1 注册部经理直接下载当前有效注册证文件 | PASS | `e2e-artifacts/result.json`; 文件 `manager-IDI_20260101_注册证上传E2E产品-E2E-UPLOAD-20260905-165416-ENTRUSTED_E2E-UPLOAD-20260905-165416-ENTRUSTED.pdf`, size `37120`. |
| E2E-2 普通用户未授权前只能申请下载 | PASS | `downloadButtonCount=0`, `requestButtonText=申请下载`; screenshot `e2e-artifacts/screenshots/user-before-request.png`. |
| E2E-3 普通用户提交下载申请 | PASS | requestId `443`; POST HTTP 200; screenshot `e2e-artifacts/screenshots/user-request-pending.png`. |
| E2E-4 注册部经理审批下载申请 | PASS | taskId `BPM:BPM_TASK_TODO:ceb64ad4-a935-11f1-9c8a-00155da805d9`; screenshot `e2e-artifacts/screenshots/manager-approval-success.png`. |
| E2E-5 普通用户获批后下载当前有效注册证文件 | PASS | 文件 `user-IDI_20260101_注册证上传E2E产品-E2E-UPLOAD-20260905-165416-ENTRUSTED_E2E-UPLOAD-20260905-165416-ENTRUSTED.pdf`, size `37120`. |
| E2E-7 普通用户仅申请并下载变更文件 | PASS | `e2e-artifacts/change-file-e2e-8-9/result.json`; requestId `446`; 文件名包含 `变更文件`, size `130409`. |
| E2E-8 普通用户申请并下载失效证件 | PASS | `e2e-artifacts/old-combo/result.json`; requestId `441`; 文件名以 `_已失效.pdf` 结尾, size `37120`. |
| E2E-9 变更文件且证件已失效的组合命名 | PASS | `e2e-artifacts/old-combo/result.json`; requestId `442`; 文件名同时包含 `变更文件` 和 `已失效`, size `130409`. |

## Verification

- `node --check doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-change-file-e2e-8-9.cjs` -> PASS.
- `node --check doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-old-combo-e2e.cjs` -> PASS.
- `node doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-download-e2e.cjs` -> PASS for E2E-1 through E2E-5.
- `node doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-change-file-e2e-8-9.cjs` -> PASS for current-scope E2E-7.
- `node doc/tasks/20260905-registration-download-simulated-e2e-verify/registration-old-combo-e2e.cjs` -> PASS for E2E-8 and E2E-9.
- Earlier code regression gates retained: `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateGrantServiceTest,DccRegistrationCertificateQueryServiceTest" test` -> PASS, 36 tests; static frontend contracts and relaxed `vue-tsc` -> PASS.
- BPM summary regression: `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateChangeServiceTest" test` -> PASS, 12 tests.

## Time Simulation Note

- 当前 worktree 后端和前端 API wrapper 存在 `/dcc/registration-certificates/business-time/simulate-daily-run`，但注册证列表页面没有正式可见的“注册测试”页签；`node tests/registration-certificate-business-time-simulation-static.spec.mjs` 失败于“注册证页面必须新增注册测试页签”。由于用户已删除 E2E-6，授权过期时间推进不再属于当前下载 E2E 完成门禁。

## Overall Conclusion

Overall result: `PASS` for the current acceptance scope.

Passed: E2E-1, E2E-2, E2E-3, E2E-4, E2E-5, E2E-7, E2E-8, E2E-9.

Removed from current acceptance scope: E2E-6.
