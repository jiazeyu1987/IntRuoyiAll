# Execution Log

BDD: pending version review list -> Given Word 升版导入已生成并提交 `PENDING_APPROVAL` 版本, When 审核人打开 eDHR 版本治理页, Then 页面展示该待审版本、定义、版本号、提交人和提交时间。
BDD: approver approves pending version -> Given 待审升版版本由用户 A 提交, When 非提交人用户 B 点击通过, Then 版本状态变为 `APPROVED`，批记录定义 currentVersion 切换到该版本，并记录审批事件。
BDD: approver rejects pending version -> Given 待审升版版本由用户 A 提交, When 非提交人用户 B 点击驳回, Then 版本状态变为 `REJECTED`，批记录定义 currentVersion 保持旧版本，并记录审批事件。
BDD: submitter cannot self approve -> Given 待审升版版本由用户 A 提交, When 用户 A 尝试通过或驳回, Then 后端明确拒绝，不改变版本状态和当前版本。
GREEN: experience-preflight -> PASS, 已读取 PowerShell、经验索引、前端统一样式、后端/前端交付技能；真实 E2E 前仍需单独读取登录门禁并跑登录预检。

RED: backend-head-contract -> FAIL, `node -e "git show HEAD:...MesProBatchRecordReportController.java / MesProBatchRecordReportService.java"` 断言旧 HEAD 必须包含 `/version-approval/pending` 与 `reviewBatchRecordVersionApproval`，实际失败：旧后端缺少待审升版列表和人工审核接口。
RED: frontend-head-contract -> FAIL, `node -e "git show HEAD:...batchrecordreport/index.ts / VersionGovernancePage.vue"` 断言旧 HEAD 必须包含 `getPendingBatchRecordVersionApprovals` 和“升版审批”，实际失败：旧前端缺少待审升版 API 与页面入口。
GREEN: `node tests\e2e\batch-record-word-import-dialog-ui-static.spec.js` -> PASS，Word 导入弹窗不再显示文件名/格式提示，预检后重建产线候选保持未选中。
GREEN: `node tests\e2e\batch-record-version-approval-closure-static.spec.js` -> PASS，版本治理页具备升版审批列表、刷新、通过、驳回、驳回原因输入和权限契约。
GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportServiceImplDbTest#batchRecordVersionApproval_pendingListAndManualApproveCloseLoop,MesProBatchRecordReportServiceImplDbTest#batchRecordVersionApproval_manualRejectKeepsCurrentVersionAndRecordsReason" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，2 tests，覆盖待审列表、提交人自审阻断、人工通过切当前版本、人工驳回保留旧版本并记录原因。
INFO: admin-write-e2e-scope -> PASS, 按 AGENTS 门禁，芋道源码/admin 不执行写入型审批 E2E；完整写入链路应在测试租户执行，芋道源码仅做只读复验。
GREEN: evidence validation -> PASS, `validate_backend_api.py` 与 `validate_frontend_feature.py` 均通过。
GREEN: task-closeout-cleanup preview/apply -> PASS, 无删除项、无阻塞；保留 task、execution-log、verification-report、backend/frontend evidence。

INFO: admin-write-e2e-override -> PASS, 用户 2026-07-13 明确授权本任务在本机 `芋道源码/admin` 执行写入型完整审批 E2E，Word 文件为 `C:\Users\BJB110\Desktop\文档\批记录压力泵.doc`；授权范围仅限 Word 升版导入、提交升版审批、版本治理页审核通过与结果核验。
GREEN: experience-preflight -> PASS, 已读取 `docs/login-access.md`、`docs/powershell-memory.md`、`docs/experience-index.md`、批记录识别经验和项目防错经验；E2E 前置要求为先跑官方 `login-preflight.mjs`，确认 `tenant-id=1`/`admin` 后只通过真实前端业务入口写入，不使用 SQL 或接口直写绕过页面路径。
GREEN: static-ui-contract -> PASS, `node tests\e2e\batch-record-word-import-dialog-ui-static.spec.js`，确认导入弹窗不显示文件名/格式提示且重建产线候选不默认选中。
GREEN: static-approval-contract -> PASS, `node tests\e2e\batch-record-version-approval-closure-static.spec.js`，确认版本治理页存在待审列表、刷新、通过、驳回与权限契约。
GREEN: backend-target-regression -> PASS, `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportServiceImplDbTest#batchRecordVersionApproval_pendingListAndManualApproveCloseLoop,MesProBatchRecordReportServiceImplDbTest#batchRecordVersionApproval_manualRejectKeepsCurrentVersionAndRecordsReason" "-Dsurefire.failIfNoSpecifiedTests=false" test`，2 tests。
GREEN: admin-login-preflight -> PASS, 官方 `login-preflight.mjs` 真实登录本机 `芋道源码/admin` 进入 `/mes/pro/feedback/edhr-version-governance`，目标页显示“升版审批”。
BLOCKER: admin-write-full-approval-e2e -> 缺少可登录的非 admin 提交账号。`smokeappr1/111111` 与 `smokeappr1/admin123` 官方登录预检均失败；离线哈希比对确认 `smokeerp1/smokeplan1/smokeappr1/smokeread1` 均不匹配 `admin123` 或 `111111`；只读查询确认当前无非 admin 提交的 `PENDING_APPROVAL` 待审版本。因后端自审阻断为正式设计，不能用 admin 自审或 SQL 改 `submitted_by` 绕过。

BDD: upgrade dialog displays latest generated version -> Given 批记录当前生效源版本仍为 V1.0 且最新已生成版本为 V2.0, When 用户确认继续升版导入生成 V3.0, Then 确认弹窗必须把“最新批记录版本 V2.0”作为用户看到的当前版本，并仅把 V1.0 表述为“当前生效源版本”。
RED: `node tests\e2e\batch-record-word-import-preflight-static.spec.js` -> FAIL, 新增断言失败，页面确认文案仍使用“当前生效版本为 ${currentVersion}，最新已生成版本为 ${latestVersion}”，会让用户看到当前版本被写成 V1.0。
GREEN: `node tests\e2e\batch-record-word-import-preflight-static.spec.js && node tests\e2e\batch-record-word-import-dialog-ui-static.spec.js && node tests\e2e\batch-record-version-approval-closure-static.spec.js` -> PASS，升版确认文案已改为“最新批记录版本为 V2.0，当前生效源版本为 V1.0，确认后将生成 V3.0”，并保留文件提示移除、候选不默认选中和审批入口契约。
GREEN: `mvn.cmd -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportControllerTest#preflightUploadedRoute_returnsBatchAndRouteVersionContract,MesProBatchRecordReportServiceImplDbTest#preflightUploadedRoute_returnsCurrentBatchRecordAndRouteVersionsWithProductOptions,MesProBatchRecordReportServiceImplDbTest#preflightUploadedRoute_returnsLatestGeneratedVersionWhenPendingVersionAlreadyExists" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests，确认预检响应透传 latest 字段、服务层在 V1.0 生效且 V2.0 已生成时返回最新版本 V2.0 与下一版本 V3.0。

BDD: approval center folds every pending item into todo -> Given 审批中心存在普通审批、候选签名、待审核或批记录升版审批任务, When 用户进入审批中心, Then 前端和后端只暴露“待办”作为所有待处理事项入口，不再提供 `SIGNATURE_PENDING` 独立菜单、页签、provider 声明或工作台跳转。
GREEN: approval-center-pending-fold-static-scan -> PASS, 活跃后端 Java 已无 `SIGNATURE_PENDING` 视图引用；前端业务源码不再声明或跳转 `SIGNATURE_PENDING`，仅保留测试负向断言和 eDHR 表单内部 CSS 类 `is-signature-pending`。
GREEN: `node tests\e2e\approval-center-phase4-static.spec.mjs && node tests\e2e\approval-center-standard-list-template-static.spec.js && node tests\e2e\batch-record-version-approval-closure-static.spec.js` -> PASS，审批中心只暴露四个可见视图，签名待处理合并进待办，升版审批入口合同仍通过。
GREEN: `mvn.cmd -pl yudao-module-bpm,yudao-module-dcc,yudao-module-mes,yudao-module-showroom -am "-Dtest=ApprovalModuleIntegrationGuardTest,DccApprovalTaskAdapterTest,MesProEdhrApprovalTaskAdapterTest,ShowroomApprovalTaskAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，BUILD SUCCESS；DCC/eDHR/Showroom provider 不再声明 `SIGNATURE_PENDING`，eDHR 候选签名和升版审批仍由 `TODO` 返回。
GREEN: approval-center-pending-fold-evidence-validation -> PASS, `validate_frontend_feature.py` 与 `validate_backend_api.py` 均通过；UTF-8 回读任务文档与请求命令日志未发现替换字符。

BDD: admin import should form version approval -> Given 用户在本机 `芋道源码/admin` 通过真实页面导入 `C:\Users\BJB110\Desktop\文档\批记录压力泵.doc` 并选择“升版导入 V3.0”, When 导入完成, Then 如果闭环成立，版本应进入 `PENDING_APPROVAL`，并能在 `/approval-center` 的 `TODO` 待办中看到 eDHR 批记录升版任务。
RED: admin-word-import-approval-formation.e2e -> FAIL, 真实页面导入完成但未形成审批。结果：`versionId=75`, `versionNo=V2.0`, `versionStatus=PRECHECK_PASSED`, `sourceBatchRecordVersionId=67`, `submitRequestCount=0`, `pendingFound=false`, `todoFound=false`, `pendingCount=0`, `todoCount=0`。导入期间未命中 `/version-approval/submit`，`/version-approval/pending` 为空，`/approval-center/tasks/page?viewType=TODO&moduleCode=EDHR` 无匹配任务。
