# 执行日志：一线提交电子签名按选择员工校验

BDD: 一线正式提交按选择员工签名 -> Given 设备端登录账号选择实际填写员工完成一线生产填写 / When 输入该选择员工的电子签名密码并正式提交 / Then 服务端按选择员工验证电子签名并生成签名记录，不因登录账号不是该员工而拒绝。

BDD: 错误员工签名仍被拒绝 -> Given 一线生产填写选择员工 A / When 输入不属于员工 A 的电子签名凭据 / Then 服务端拒绝提交并保留正式错误，不写入报工、记录本或工序池事件。

- INFO: task-start -> 已读取 `docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/backend-development.md`、`docs/frontend-development.md` 与 bug regression 技能契约。
- INFO: experience-gate -> 命中一线正式提交与项目口径门禁：签名主体是实际选择员工，不是设备端登录账号。

- ROOT_CAUSE: frontend-guard -> `FrontlineFixedTemplatePanel.vue` 在正式提交前要求 `signatureEmployeeId === currentLoginUserId`，直接产生“当前登录账号必须是实际填写员工”错误。
- ROOT_CAUSE: backend-guard -> `MesProFrontlineFeedbackSubmitServiceImpl` 通过 `validateSignatureActorMatchesLoginUser` 要求实际员工/签名员工等于登录用户。
- ROOT_CAUSE: signature-service -> `MesProBatchRecordExecutionSignatureService` 的生产提交签名只按登录用户落签名记录，未提供选择员工 actor 路径。
- RED: `node tests/e2e/frontline-formal-submit-selected-employee-static.spec.cjs` -> FAIL, expected reason: 前端源码仍包含登录账号拦截文案和签名员工等于当前登录人的本地 guard。
- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest,MesProBatchRecordExecutionSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: `recordProductionSubmitSignature(Long, String, String)` 尚不存在，后端提交服务无法按选择员工调用签名服务。
- INFO: implementation -> 前端移除登录账号拦截，签名弹窗文案改为所选员工签名密码；后端移除登录人匹配校验，正式提交调用 `recordProductionSubmitSignature(signatureEmployeeId, password, comment)`。
- INFO: implementation -> 签名服务新增显式 actor 的生产提交签名路径，校验并持久化选择员工签名快照；移除未使用的登录人版生产提交签名入口。
- INFO: implementation -> 后端拒绝客户端预传 `signatureId`，确保签名 ID 仅由服务端生成。
- GREEN: `node tests/e2e/frontline-formal-submit-selected-employee-static.spec.cjs` -> PASS。
- GREEN: `pnpm e2e:frontline-formal-submit:static` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest,MesProBatchRecordExecutionSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 22 tests, 0 failures, 0 errors。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest,MesP0ProductionSubmitClosedLoopContractTest,MesProFrontlineFeedbackRawLimitBypassTest,MesProFrontlineFeedbackRouteOrderGateTest,MesProFrontlineFeedbackSubmitDetailContractTest,MesProFrontlineFeedbackSubmitRollbackTest,MesProBatchRecordExecutionSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 31 tests, 0 failures, 0 errors。
- GREEN: `rg -n -g '!**/target/**' -g '!**/target_corrupt_m4_20260802_1327/**' "当前登录账号必须是实际填写员工|PRO_FRONTLINE_FEEDBACK_SIGNATURE_LOGIN_MISMATCH|validateSignatureActorMatchesLoginUser|recordProductionSubmitSignature\(String" IntRuoyiBackend IntRuoyiFronted` -> PASS, only match is the regression test's forbidden-text assertion。
- GREEN: `git diff --check` -> PASS。
- INFO: experience-consolidation -> 已更新 `docs/backend-development.md#一线生产正式提交必须单事务落链并按唯一组长归属可见` 和 `docs/experience-index.md`，沉淀选择员工签名不得绑定当前登录账号的门禁。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\fix-electronic-signature-selected-employee\bug-regression-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id fix-electronic-signature-selected-employee --mode preview` -> PASS, keep task/core evidence, delete none, blocked none。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id fix-electronic-signature-selected-employee --mode apply` -> PASS, deleted none, blocked none。
- INFO: task-complete -> 任务状态已更新为 completed；按项目 Git policy 未提交。
