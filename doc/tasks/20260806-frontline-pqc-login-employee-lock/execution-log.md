# Execution Log

## User Intent

- 用户要求一线 PQC 的生产订单来自活跃订单池，工序来自所选活跃订单工艺路线，员工显示当前登录 PQC 员工或 PQC 组长本人且不可切换。

## BDD Scenarios

- BDD: PQC 活跃订单与工序正式来源 -> Given 一线 PQC 打开填写页 When 选择生产订单 Then 订单候选来自活跃订单池，工序候选来自所选活跃订单对应工艺路线。
- BDD: PQC 员工锁定登录人 -> Given PQC 员工账号登录一线 PQC 填写页 When 页面加载和切换订单/工序 Then 员工显示登录人姓名，页面不提供员工选择入口。
- BDD: PQC 组长锁定登录人 -> Given PQC 组长账号登录一线 PQC 填写页 When 页面加载和切换订单/工序 Then 员工显示 PQC 组长本人姓名，不能切换为其他 PQC 员工。
- BDD: 后端拒绝非本人切换 -> Given 登录人调用 PQC 切换员工接口 When 请求的 actualEmployeeId 不是登录人 Then 后端返回业务错误，不返回其他人员模板。

## RED / GREEN Evidence

- RED: `node tests/e2e/mes-frontline-pqc-login-employee-lock-static.spec.cjs` -> FAIL, expected reason: `PQC 员工卡必须标记为登录员工只读卡。`
- GREEN: `node tests/e2e/mes-frontline-pqc-login-employee-lock-static.spec.cjs` -> PASS.
- GREEN: `node tests/e2e/mes-frontline-pqc-active-order-switching-static.spec.js` -> PASS.
- GREEN: `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs` -> PASS, output: `PASS: eDHR frontline fill tabs static contract`.
- BLOCKED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` first hung in Maven incremental compile cleanup at `IncrementalBuildHelper.beforeRebuildExecution`.
- BLOCKED: `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> FAIL before tests during `yudao-module-mes` compile because unrelated existing classes reference missing nested types: `MesProBatchRecordSharedRowTypeRules.RowType`, `MesProBatchRecordSharedPageTitleRules.SharedPageTitleType`, and `CapacityWindowAllocator.ScheduleWindowResult`.
- GREEN: `git diff --check` -> PASS with CRLF warnings only; no whitespace error lines were reported.

## Milestone Updates

- M1 completed: 新增前端静态合同 `IntRuoyiFronted/tests/e2e/mes-frontline-pqc-login-employee-lock-static.spec.cjs`，先验证旧实现 RED。
- M2 completed: `FrontlineFixedTemplatePanel.vue` 中 PQC 员工卡改为登录员工只读卡，拦截 PQC 员工 picker，初始化只选择当前登录员工，并阻止路由 `actualEmployeeId` 覆盖 PQC 登录人。
- M3 completed: 后端 PQC 人员列表和切换接口改为传入 `loginUserId`，人员候选只返回登录人，非本人 `actualEmployeeId` fail fast。
- M4 blocked: 前端静态合同和回归合同已通过；后端 Maven 目标测试被既有 MES 编译错误阻塞。
- M5 completed: 已补充前后端证据和验证报告；任务状态保持 blocked，等待先修复既有编译错误后重跑后端测试。

## Blockers

- 当前工作区已有大量既有脏改动且分支落后 `origin/int_main` 7 个提交；本任务将只修改任务范围文件，不回滚或清理并行改动。
- 后端目标测试当前无法完成：`yudao-module-mes` 编译失败位置在批记录报表和排程类，不属于本任务 PQC 登录员工锁定范围。
