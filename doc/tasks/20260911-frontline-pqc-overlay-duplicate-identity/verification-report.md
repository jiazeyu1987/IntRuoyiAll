# Verification Report

## Result

代码修复与定向回归通过；运行态重启复验待授权。

## Root Cause

一线 PQC 覆盖匹配遗漏 `routeProcessId/processId`，并且未比较已有的 `businessDate/shiftCode/roundNo`，因此跨生产工序或跨轮次的合法任务被误判为重复。

## Fix

- 完整任务身份加入生产工序和时序字段。
- 合法跨生产工序、跨日期/班次/轮次任务分别匹配。
- 完整身份重复仍明确抛出异常，不静默取第一条。

## Verification Evidence

- 基线源码合同 -> RED（缺少 routeProcessId）。
- 当前源码合同 -> GREEN。
- 定向 Maven 回归 -> PASS（21 tests, 0 failures, 0 errors）。
- 扩展定向 Maven 回归 -> PASS（26 tests, 0 failures, 0 errors）。
- 前端 TypeScript 检查 -> PASS。
- `git diff --check` -> PASS（仅 CRLF 提示）。
- 未重启 int_main；当前运行 Jar 早于修复代码，运行态复验未宣称通过。

## Residual Risk

- 需在获授权重启后，用真实一线 PQC 页面重新加载目标活跃订单并验证提交链路。
- 未执行 Git 提交/推送。
