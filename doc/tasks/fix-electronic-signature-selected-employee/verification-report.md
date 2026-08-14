# 验证报告：一线提交电子签名按选择员工校验

## 结论

PASS。正式提交电子签名已从“当前登录账号”改为“页面选择的实际填写员工”。当设备端登录账号与实际填写员工不同，只要 `signatureEmployeeId == actualEmployeeId` 且签名密码匹配该选择员工，就可以完成签名；错误员工签名仍由统一电子签名校验拒绝。

## 修复摘要

- 前端移除 `signatureEmployeeId` 必须等于 `currentLoginUserId` 的本地 guard，签名弹窗文案改为“所选员工签名密码”。
- 后端移除实际员工/签名员工必须等于登录用户的校验，保留 `actualEmployeeId == signatureEmployeeId` 的正式数据一致性校验。
- 签名服务新增显式 actor 的生产提交签名入口，并移除未使用的登录人版生产提交入口，避免后续误按登录账号签名。
- 后端拒绝客户端预传 `signatureId`，确保签名记录只由服务端在事务内生成。

## 验证命令

- `node tests/e2e/frontline-formal-submit-selected-employee-static.spec.cjs` -> PASS。
- `pnpm e2e:frontline-formal-submit:static` -> PASS。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest,MesProBatchRecordExecutionSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，22 tests，0 failures，0 errors。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest,MesP0ProductionSubmitClosedLoopContractTest,MesProFrontlineFeedbackRawLimitBypassTest,MesProFrontlineFeedbackRouteOrderGateTest,MesProFrontlineFeedbackSubmitDetailContractTest,MesProFrontlineFeedbackSubmitRollbackTest,MesProBatchRecordExecutionSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，31 tests，0 failures，0 errors。
- `rg -n -g '!**/target/**' -g '!**/target_corrupt_m4_20260802_1327/**' "当前登录账号必须是实际填写员工|PRO_FRONTLINE_FEEDBACK_SIGNATURE_LOGIN_MISMATCH|validateSignatureActorMatchesLoginUser|recordProductionSubmitSignature\(String" IntRuoyiBackend IntRuoyiFronted` -> PASS，仅命中新回归测试中的禁止文案断言。
- `git diff --check` -> PASS。

## 风险与未执行项

- 未运行真实 Playwright 写入型 E2E；本次未启动本地运行态、未创建测试租户数据。当前验证覆盖前端静态契约、后端服务行为、签名服务 actor 快照和相邻正式提交回归。
- 工作区存在大量非本任务并发改动；本任务未提交 Git，未回滚或清理无关改动。
