# Execution Log

BDD: 缺少项目代码仍可申请下载 -> Given 注册证有正式可下载文件但项目代码为空 When 普通用户在详情页点击申请下载 Then 页面提交下载申请，不提示“缺少项目代码”，审批摘要中项目代码可为空。

BDD: 下载授权 24 小时有效 -> Given 普通用户下载申请已审批通过 When 在 24 小时内或超过 24 小时后查看文件操作 Then 24 小时内可下载，超过 24 小时需重新申请。

GREEN: `node IntRuoyiFronted/tests/registration-certificate-access-request-panel-static.spec.mjs; node IntRuoyiFronted/tests/registration-certificate-download-e2e-doc-static.spec.mjs; node IntRuoyiFronted/tests/registration-certificate-download-request-inline-ux-static.spec.mjs; node IntRuoyiFronted/tests/registration-certificate-chinese-copy-static.spec.mjs` -> PASS，`int_main` 本地复跑通过。

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateAccessRequestServiceTest,DccRegistrationCertificateFileDeliveryServiceTest,DccRegistrationCertificateBpmIntegrationTest,DccRegistrationCertificateApprovalRuntimeTest,DccRegistrationCertificateAccessPolicyTest,DccRegistrationCertificateGrantServiceTest" test` -> PASS，62 tests, 0 failures, 0 errors。

GREEN: `mvn -pl yudao-module-bpm "-Dtest=BpmMessageServiceImplTest" test` -> PASS，15 tests, 0 failures, 0 errors。

BLOCKED: `git push origin int_main` -> FAIL，连续两次返回 `TLS connect error: error:0A000126:SSL routines::unexpected eof while reading`。本地注册证下载融合提交已生成，远端 `origin/int_main` 仍未更新。

GREEN: `node IntRuoyiFronted/tests/registration-certificate-access-request-panel-static.spec.mjs; node IntRuoyiFronted/tests/registration-certificate-download-e2e-doc-static.spec.mjs; node IntRuoyiFronted/tests/registration-certificate-download-request-inline-ux-static.spec.mjs; node IntRuoyiFronted/tests/registration-certificate-download-search-static.spec.mjs; node IntRuoyiFronted/tests/registration-certificate-chinese-copy-static.spec.mjs` -> PASS，继续验证缺少项目代码可申请下载、24 小时口径、中文文案和下载搜索合同。

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateAccessRequestServiceTest,DccRegistrationCertificateFileDeliveryServiceTest,DccRegistrationCertificateBpmIntegrationTest,DccRegistrationCertificateApprovalRuntimeTest,DccRegistrationCertificateAccessPolicyTest,DccRegistrationCertificateGrantServiceTest" test` -> PASS，62 tests, 0 failures, 0 errors。

GREEN: `mvn -pl yudao-module-bpm "-Dtest=BpmMessageServiceImplTest" test` -> PASS，15 tests, 0 failures, 0 errors。

BLOCKED: `git fetch origin; git status --short --branch; git push origin int_main` -> FAIL，`git fetch origin` 与 `git push origin int_main` 均返回 `TLS connect error: error:0A000126:SSL routines::unexpected eof while reading`。影响：无法按用户要求同步到远端最新代码，且本地领先 `origin/int_main` 的 2 个提交无法推送。
