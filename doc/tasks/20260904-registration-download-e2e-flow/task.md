# 注册证下载 E2E 验证与修复

## Task Goal

在独立 worktree 中按 `e2e_test/registration/download/registration-certificate-download-e2e-acceptance.md` 执行注册证下载完整 E2E 验证，发现真实前端链路问题后按 BDD/TDD 修复，并记录验证证据。

## Milestones

1. completed - 建立 worktree、登记端口、同步当前下载验收文档与相关已确认改动。
2. completed - 启动 worktree 后端和前端，确认端口与服务健康。
3. completed - 用 Playwright 通过真实前端执行注册部经理直接下载、普通用户申请、注册部经理审批、普通用户获批下载、24 小时过期策略可验证部分。
4. completed - 对发现的错误补回归测试并修复。
5. completed - 复跑定向测试和 E2E，记录结果。
6. completed - 融合进 `int_main` 后复跑前端静态合同和后端定向单测。

## Expected Verification

- Playwright 真实前端路径执行，业务动作不得由 API 或 SQL 替代。
- 注册部经理 `chudongchuan` 可直接下载。
- 普通用户 `wanglixuan` 未授权前看不到下载按钮，只能申请下载。
- 点击申请下载后提示“已申请下载”，按钮变为“申请中”。
- 注册部经理审批通过后普通用户可下载真实文件。
- 下载文件名符合下划线命名、注册证批准日期、项目代码可为空、变更文件和已失效规则。
- 定向静态/后端测试通过。

## Current Status

ready_for_closeout - worktree 中已完成修复，并通过真实前端主下载链路 E2E。已融合进 `int_main` 并完成本地注册证下载融合提交，定向前端静态合同、DCC 下载/审批后端单测、BPM 通知单测均通过。远端推送连续两次失败，错误为 GitHub HTTPS TLS connect unexpected EOF，因此暂不能标记为 completed。当前仍有 MES 生产组长相关未提交改动，未纳入本任务范围。

## Final Verification

- `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateAccessRequestServiceTest,DccRegistrationCertificateFileDeliveryServiceTest,DccRegistrationCertificateBpmIntegrationTest,DccRegistrationCertificateApprovalRuntimeTest" test` -> PASS, 48 tests, 0 failures, 0 errors。
- `mvn -pl yudao-module-bpm "-Dtest=BpmMessageServiceImplTest" test` -> PASS, 15 tests, 0 failures, 0 errors。
- `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateAccessPolicyTest,DccRegistrationCertificateGrantServiceTest,DccRegistrationCertificateFileDeliveryServiceTest" test` -> PASS, 29 tests, 0 failures, 0 errors。
- `powershell -ExecutionPolicy Bypass -File scripts\runtime\start-branch-backend.ps1 -Slot 21 -Build` -> PASS，后端 48155 启动成功。
- `node doc\tasks\20260904-registration-download-e2e-flow\registration-download-e2e.cjs` -> PASS，真实前端主下载链路通过，结果见 `doc/tasks/20260904-registration-download-e2e-flow/e2e-artifacts/result.json`。
- `node IntRuoyiFronted/tests/registration-certificate-access-request-panel-static.spec.mjs; node IntRuoyiFronted/tests/registration-certificate-download-e2e-doc-static.spec.mjs; node IntRuoyiFronted/tests/registration-certificate-download-request-inline-ux-static.spec.mjs; node IntRuoyiFronted/tests/registration-certificate-chinese-copy-static.spec.mjs` -> PASS，`int_main` 本地复跑通过。
- `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateAccessRequestServiceTest,DccRegistrationCertificateFileDeliveryServiceTest,DccRegistrationCertificateBpmIntegrationTest,DccRegistrationCertificateApprovalRuntimeTest,DccRegistrationCertificateAccessPolicyTest,DccRegistrationCertificateGrantServiceTest" test` -> PASS，`int_main` 本地 62 tests, 0 failures, 0 errors。
- `mvn -pl yudao-module-bpm "-Dtest=BpmMessageServiceImplTest" test` -> PASS，`int_main` 本地 15 tests, 0 failures, 0 errors。

## 设计约束检查

- worktree 路径位于 `D:\IntRuoyiWorktree\20260904-registration-download-e2e-flow`。
- runtime profile 为 `int_main`，slot 21，前端端口 8155，后端端口 48155。
- 不占用 `int_main` 固定端口 8081/48081。
- E2E 必须使用真实页面操作；API/DB 仅允许只读核验。
- 不记录密码、token、cookie。
