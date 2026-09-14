# Execution Log

BDD: 注册部经理直接下载 -> Given 注册部经理登录芋道源码租户并进入注册证详情 When 点击注册证附件下载 Then 浏览器产生真实下载事件且文件名符合业务规则。

BDD: 普通用户申请下载 -> Given 普通用户能查看注册证详情但没有目标文件下载授权 When 点击附件区申请下载 Then 页面提示“已申请下载”，按钮变为“申请中”，不出现“没有该操作权限”或“缺少项目代码”。

BDD: 审批后下载 -> Given 注册部经理审批普通用户的下载申请 When 普通用户重新进入详情 Then 目标附件显示下载按钮，点击后产生真实下载文件。

BDD: 独立审批结果控件移除 -> Given 普通用户进入访问申请面板 When 页面渲染 Then 不出现审批结果页签、授权列表、独立下载或撤销授权控件。

RED: `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateAccessRequestServiceTest,DccRegistrationCertificateFileDeliveryServiceTest" test` -> FAIL, 发现 `DOWNLOAD_FILE` 无项目代码被前端、服务层与数据库约束共同拦截，文件名无项目代码时也未保留空项目代码段。

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateAccessRequestServiceTest,DccRegistrationCertificateFileDeliveryServiceTest" test` -> PASS, 26 tests, 0 failures, 0 errors；下载申请允许项目代码为空，下载文件名用空项目代码段加下划线分隔。

RED: Playwright 真实前端 E2E -> FAIL, 普通用户提交下载申请后，注册部经理在审批中心看不到对应下载审批任务；诊断发现审批候选人仍按授权公司范围筛选，缺少公司授权时不会把注册部经理加入候选人。

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateAccessRequestServiceTest,DccRegistrationCertificateFileDeliveryServiceTest,DccRegistrationCertificateBpmIntegrationTest,DccRegistrationCertificateApprovalRuntimeTest" test` -> PASS, 48 tests, 0 failures, 0 errors；注册证下载审批候选人改为按注册部经理角色及审批权限解析，不再依赖授权公司范围。

RED: Playwright 真实前端 E2E -> FAIL, 注册部经理可看到新下载审批任务，但点击审批通过后后端回滚并提示“手机号不存在”；日志显示注册证访问流程未匹配 DCC 站内通知模板，流程完成通知错误落入短信发送路径。

GREEN: `mvn -pl yudao-module-bpm "-Dtest=BpmMessageServiceImplTest" test` -> PASS, 15 tests, 0 failures, 0 errors；注册证访问流程 `dcc-registration-certificate-access` 补齐为 DCC 站内通知流程，不再依赖用户手机号发送短信。

GREEN: `powershell -ExecutionPolicy Bypass -File scripts\runtime\start-branch-backend.ps1 -Slot 21 -Build` -> PASS, worktree 后端构建成功并在 48155 启动，日志出现“项目启动成功”。

GREEN: `node doc\tasks\20260904-registration-download-e2e-flow\registration-download-e2e.cjs` -> PASS, Playwright 真实前端完整链路通过：注册部经理直接下载；普通用户未授权前下载按钮数量为 0 且只能看到“申请下载”；提交后 requestId=298、按钮变为申请中；注册部经理审批通过；普通用户重新进入详情后成功下载文件，文件名为 `IDI_20260101_注册证上传E2E产品-20260904084148-自产_E2E-UPLOAD-20260904084148-SELF.pdf`。

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateAccessPolicyTest,DccRegistrationCertificateGrantServiceTest,DccRegistrationCertificateFileDeliveryServiceTest" test` -> PASS, 29 tests, 0 failures, 0 errors；后端规则覆盖 24 小时下载授权过期、授权截止时间、变更文件命名追加“变更文件”、失效证件命名追加“已失效”。注意：这些属于后端规则验证，不等同于 Playwright 真实前端 E2E-6/E2E-7/E2E-8/E2E-9 通过。

GREEN: `node IntRuoyiFronted/tests/registration-certificate-access-request-panel-static.spec.mjs; node IntRuoyiFronted/tests/registration-certificate-download-e2e-doc-static.spec.mjs; node IntRuoyiFronted/tests/registration-certificate-download-request-inline-ux-static.spec.mjs; node IntRuoyiFronted/tests/registration-certificate-chinese-copy-static.spec.mjs` -> PASS, `int_main` 本地复跑通过；首次从 `IntRuoyiFronted` 目录运行因脚本按仓库根路径解析而失败，已按项目 E2E 规则改用仓库根目录重跑。

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccRegistrationCertificateAccessRequestServiceTest,DccRegistrationCertificateFileDeliveryServiceTest,DccRegistrationCertificateBpmIntegrationTest,DccRegistrationCertificateApprovalRuntimeTest,DccRegistrationCertificateAccessPolicyTest,DccRegistrationCertificateGrantServiceTest" test` -> PASS, 62 tests, 0 failures, 0 errors；`int_main` 本地验证下载申请、审批候选、文件命名、授权过期规则。

GREEN: `mvn -pl yudao-module-bpm "-Dtest=BpmMessageServiceImplTest" test` -> PASS, 15 tests, 0 failures, 0 errors；`int_main` 本地验证注册证访问流程通知走 DCC 站内通知通道。

BLOCKED: `git push origin int_main` -> FAIL, 连续两次返回 `TLS connect error: error:0A000126:SSL routines::unexpected eof while reading`。本地注册证下载融合提交已生成，远端 `origin/int_main` 仍未更新。
