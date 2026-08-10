# 任务：一线提交电子签名按选择员工校验

## Task Goal

修复一线员工正式提交时电子签名错误校验当前登录账号的问题。电子签名主体应为页面选择的实际填写员工；只要签名密码与该选择员工的签名数据一致，即可完成提交。

## Milestones

- [x] M1：定位一线正式提交电子签名校验链路并复现错误契约。
- [x] M2：补充 RED 回归测试，证明选择员工与登录账号不一致但签名密码匹配时应通过。
- [x] M3：实施最小后端/前端修复，不引入 fallback、默认成功或吞异常。
- [x] M4：运行定向 GREEN 与相关回归验证，记录剩余阻塞。
- [x] M5：整理验证报告并进入任务收尾。

## Expected Verification

- 后端定向测试覆盖一线正式提交签名主体为选择员工，不要求当前登录账号等于实际填写员工。
- 前端静态契约如受影响，覆盖提交载荷仍只传正式签名密码和实际选择员工上下文，不传伪造签名 ID。
- `git diff --check` 通过。

## Applicable Experience Gates

- 一线生产正式提交门禁：正式接口应只传 `signaturePassword`，签名、报工、记录本和工序池事件必须同事务落链；缺少签名或签名员工应 fail fast，不得用前端 `signatureId` 或当前登录人替代。
- 项目口径门禁：设备端登录账号只是现场入口；实际填写员工、电子签名员工和工序池提交事件责任主体必须是员工本人。
- 前端写入/签名前置门禁：签名前先校验正式结构字段，失败显式暴露，不得默认成功或重复提交掩盖错误。

## Current Status

completed

已完成代码修复、回归测试、验证报告、经验门禁更新和 task-closeout-cleanup preview/apply 收尾；未执行 Git 提交。

## Cleanup Keep

- `doc/tasks/fix-electronic-signature-selected-employee/bug-regression-evidence.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。已将一线生产正式提交签名主体改为实际选择员工，并移除未使用的登录人版生产提交签名入口，避免后续误用。
- `是否存在临时补丁或绕过`：否。

## Final Verification

- `node tests/e2e/frontline-formal-submit-selected-employee-static.spec.cjs` -> PASS。
- `pnpm e2e:frontline-formal-submit:static` -> PASS。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest,MesProBatchRecordExecutionSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，22 tests。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProFrontlineFeedbackSubmitServiceTest,MesP0ProductionSubmitClosedLoopContractTest,MesProFrontlineFeedbackRawLimitBypassTest,MesProFrontlineFeedbackRouteOrderGateTest,MesProFrontlineFeedbackSubmitDetailContractTest,MesProFrontlineFeedbackSubmitRollbackTest,MesProBatchRecordExecutionSignatureServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，31 tests。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\fix-electronic-signature-selected-employee\bug-regression-evidence.md` -> PASS。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id fix-electronic-signature-selected-employee --mode preview` -> PASS，无删除、无阻塞。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id fix-electronic-signature-selected-employee --mode apply` -> PASS，无删除、无阻塞。
- `git diff --check` -> PASS。
