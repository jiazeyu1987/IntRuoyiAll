# 注册证下载 E2E 验证报告

## 结论

真实前端主下载链路已通过：注册部经理直接下载、普通用户未授权前只能申请、申请后按钮变为“申请中”、注册部经理在审批中心审批、普通用户获批后成功下载真实文件。

本轮发现并修复 3 个实际问题：

1. 无项目代码时下载申请/下载命名被错误拦截。
2. 下载审批候选人错误依赖授权公司范围，导致注册部经理看不到审批任务。
3. 注册证访问流程通知未走 DCC 站内通知，错误落入短信路径，审批完成时因账号无手机号提示“手机号不存在”。

## 真实前端 E2E 覆盖

| 验证项 | 结果 | 证据 |
| --- | --- | --- |
| 注册部经理可直接下载 | PASS | `e2e-artifacts/result.json` 中 `managerDownload.size=37120` |
| 普通用户未授权前看不到下载按钮 | PASS | `userBeforeRequest.downloadButtonCount=0` |
| 普通用户可看到申请下载按钮 | PASS | `userBeforeRequest.requestButtonText=申请下载` |
| 点击申请后按钮变为申请中 | PASS | 脚本断言按钮文本包含“申请中”，截图 `screenshots/user-request-pending.png` |
| 注册部经理可在审批中心审批 | PASS | `approval.requestId=298`，截图 `screenshots/manager-approval-success.png` |
| 普通用户获批后可下载 | PASS | `userAfterApprovalDownload.size=37120` |
| 文件名基础规则 | PASS | `IDI_20260101_注册证上传E2E产品-20260904084148-自产_E2E-UPLOAD-20260904084148-SELF.pdf` |

## 未作为真实前端 E2E PASS 声明的项

以下规则已由后端测试覆盖，但本轮 Playwright 脚本未完成真实前端扩展场景覆盖，因此不声明为 E2E PASS：

- 超过 24 小时后授权失效并需要重新申请。
- 仅下载变更文件时文件名包含“变更文件”。
- 下载失效证件时文件名包含“已失效”。
- 变更文件且证件失效的组合命名。

原因：本轮真实前端脚本选中的数据 A 是当前有效注册证，`changeFileCount=0`、`oldStatus=false`；也未使用正式页面时间推进能力或任务自有历史授权样本来构造超过 24 小时的过期授权。

## 自动化测试证据

- `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateAccessRequestServiceTest,DccRegistrationCertificateFileDeliveryServiceTest,DccRegistrationCertificateBpmIntegrationTest,DccRegistrationCertificateApprovalRuntimeTest" test` -> PASS，48 tests。
- `mvn -pl yudao-module-bpm "-Dtest=BpmMessageServiceImplTest" test` -> PASS，15 tests。
- `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateAccessPolicyTest,DccRegistrationCertificateGrantServiceTest,DccRegistrationCertificateFileDeliveryServiceTest" test` -> PASS，29 tests。
- `node doc\tasks\20260904-registration-download-e2e-flow\registration-download-e2e.cjs` -> PASS。

## int_main 融合后复验

- `node IntRuoyiFronted/tests/registration-certificate-access-request-panel-static.spec.mjs; node IntRuoyiFronted/tests/registration-certificate-download-e2e-doc-static.spec.mjs; node IntRuoyiFronted/tests/registration-certificate-download-request-inline-ux-static.spec.mjs; node IntRuoyiFronted/tests/registration-certificate-chinese-copy-static.spec.mjs` -> PASS。
- `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateAccessRequestServiceTest,DccRegistrationCertificateFileDeliveryServiceTest,DccRegistrationCertificateBpmIntegrationTest,DccRegistrationCertificateApprovalRuntimeTest,DccRegistrationCertificateAccessPolicyTest,DccRegistrationCertificateGrantServiceTest" test` -> PASS，62 tests。
- `mvn -pl yudao-module-bpm "-Dtest=BpmMessageServiceImplTest" test` -> PASS，15 tests。

## 运行环境

- worktree：`D:\IntRuoyiWorktree\20260904-registration-download-e2e-flow`
- 后端端口：48155
- 前端端口：8155
- 租户：芋道源码
- 账号标签：注册部经理 `chudongchuan`，普通用户 `wanglixuan`

当前 `int_main` 仍存在 MES 生产组长相关未提交改动，未纳入本任务融合范围。本地注册证下载融合提交已生成；`git push origin int_main` 连续两次失败，错误为 GitHub HTTPS TLS connect unexpected EOF，远端尚未更新。
