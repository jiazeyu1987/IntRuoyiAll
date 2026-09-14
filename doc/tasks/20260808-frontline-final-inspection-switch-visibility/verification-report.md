# Verification Report

## Scope

一线 PQC 填写页“末检”按钮必须只在对应产品发布态 QA 规程 `finalInspectionApplicable=true` 时显示并可选；关闭时隐藏且选择函数不可切换到末检。

## Verified Behavior

- 发布态 QA 规程的 `finalInspectionApplicable` 由后端正式版本读取，并通过一线工序响应传给前端。
- 前端仅在 `selectedProcess.finalInspectionApplicable === true` 时渲染“末检”按钮。
- 即使被代码路径直接调用，`selectPqcInspectionType('FINAL')` 也会在未开启末检时拒绝选择。
- 静态合同明确禁止用检验项目数量或其它前端推断状态作为末检可见性 fallback。

## Commands

- `node tests/e2e/frontline-pqc-final-inspection-switch-static.spec.cjs`：PASS。
- `pnpm ts:check`：PASS。
- `mvn -pl yudao-module-mes -am "-DskipTests" compile`：PASS。
- `git diff --check`：PASS，仅存在既有 LF/CRLF 提示。

## Blocked Adjacent Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：MES 测试源整体 `testCompile` 被既有缺失类阻塞，无法进入目标单测执行；生产源码编译已通过。
- `node src/views/mes/pro/feedback/frontline-template-render.spec.cjs`：既有 `production UI must have a no-device full-width layout` 断言失败，未进入本次末检断言。

## Result

本次需求的可验证链路已完成；剩余失败均为相邻既有测试阻塞，不作为本次末检开关行为的代码失败证据。
