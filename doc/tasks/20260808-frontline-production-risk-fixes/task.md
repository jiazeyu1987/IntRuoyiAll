# 一线生产不符合/风险项修复

## Task Goal

修复一线生产静态分析中发现的不符合/风险项：所选员工电子密码校验来源、前端签名文案、无设备工序提交上下文、运行态/后端参数规则一致性、组长报工可见性依赖人员 scope 的闭环。

## Milestones

- [x] 记录 BDD 场景并补充 RED 回归合同
- [x] 修复后端一线提交、签名、设备参数和组长员工 scope 链路
- [x] 修复前端一线提交上下文和签名提示文案
- [x] 执行定向 GREEN/REGRESSION 验证
- [x] 更新验证报告和任务状态
- [x] 修复一线生产设备卡片前端最多显示 3 个的额外限制
- [x] 复跑设备卡片静态合同和相关回归

## Expected Verification

- `node IntRuoyiFronted/tests/e2e/frontline-production-risk-fixes-static.spec.cjs`
- `node IntRuoyiFronted/tests/e2e/frontline-production-device-row-density-static.spec.cjs`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordExecutionSignatureServiceTest,MesFrontlineDeviceParameterValidatorTest,MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260808-frontline-production-risk-fixes/bug-regression-evidence.md`
- `git diff --check`

## Applicable Gates

- 一线生产正式提交必须单事务落链并按唯一组长归属可见：生产提交允许无工单，但必须保留正式路线、工序、工作站、实际员工、签名员工、设备参数和组长可见性校验。
- 一线运行态 route-start 生产组长来源必须独立于班组设备绑定：工作站正式设备不得强制替代班组设备维护配置。
- MES 生产人员档案正式工重复关联门禁：一线生产员工弹窗、运行配置员工和切换员工校验必须同源于当前负责生产组长启用的生产人员档案。
- 前端写入成功与列表刷新失败分层门禁：本任务不改刷新链路，但正式提交不得用前端默认值或隐藏错误掩盖后端失败。
- 设备卡片展示不得前端截断：一线生产设备卡片必须显示运行态返回的全部工序设备，不能只取前三个设备。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是统一所选员工身份、设备上下文、参数规则和组长 scope 数据链路。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Completed Work

- 后端正式提交签名改为优先按所选 actor 处理：系统用户继续走统一电子签名授权，临时人员档案走人员档案电子密码哈希校验。
- 后端设备参数提交校验改为与运行态同源，只接受当前 `routeProcessId` 的参数规则，不再把空路线工序历史规则强加到提交。
- 人员管理新增/关联/启停员工时同步生产组长 `PRODUCTION + EMPLOYEE` scope，正式工使用 `systemUserId`，临时工使用人员档案 `id`。
- 前端正式提交设备上下文只来自当前可见设备卡片；无设备工序不再回填路线/工作站候选设备；签名提示改为所选员工电子签名密码。
- 本轮重新打开任务，补充修复“最多 3 个设备卡片”的额外前端限制。

## Verification Summary

- `node IntRuoyiFronted/tests/e2e/frontline-production-risk-fixes-static.spec.cjs`：PASS。
- `node IntRuoyiBackend/yudao-module-mes/src/test/js/mes-frontline-production-risk-fixes-static.spec.cjs`：PASS。
- `node IntRuoyiBackend/yudao-module-mes/src/test/js/mes-frontline-production-extra-restrictions-removed-static.spec.cjs`：PASS。
- `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordExecutionSignatureServiceTest,MesFrontlineDeviceParameterValidatorTest,MesTeamLeaderRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：PASS，36 tests。
- `git diff --check`（本任务相关路径）：PASS，仅 LF/CRLF 提示，无 whitespace error。
- `task_closeout.py --task-id 20260808-frontline-production-risk-fixes --mode preview/apply`：PASS，无删除项、无阻塞、无 warnings。

## 2026-08-08 Reopen Verification Summary

- `visibleDeviceCards` 已改为直接返回 `configuredDeviceCards.value`，删除 `slice(0, 3)` 前端截断。
- `frontline-production-risk-fixes-static.spec.cjs` 增加负向合同，禁止重新引入前三台截断。
- 前端设备卡片密度和参数范围展示相邻静态合同均已通过。
## Cleanup Keep

- doc/tasks/20260808-frontline-production-risk-fixes/bug-regression-evidence.md
## Final Closeout

- `task_closeout.py --task-id 20260808-frontline-production-risk-fixes --mode preview`：PASS，keep 为任务文档和 bug 证据，delete/blocked/warnings 均为 none。
- `task_closeout.py --task-id 20260808-frontline-production-risk-fixes --mode apply`：PASS，deleted_paths 为 none，当前主工作区非 linked worktree，无 merge/remove 操作。
- 最终状态：completed。