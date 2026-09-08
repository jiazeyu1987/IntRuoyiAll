# 2.9 可信时间最小闭环

## Task Goal

用最少改动实现服务器时间巡检、正式签名时间不可被业务时间覆盖，以及运行控制台一键导出时间戳审查证据。

## Scope

- 修正 MES 正式签名时间与用户业务时间的展示边界。
- 复用 Runtime Control 巡检、文件存储、探针和权限。
- 采集正式服、审查服 chrony、系统时间和数据库时间状态。
- 导出包含 HTML、JSON、SHA-256 清单的 ZIP。

## Milestones

- [x] M1：签名时间边界修复。
- [x] M2：时间巡检和证据 ZIP 导出。
- [x] M3：运行控制台状态与导出按钮。
- [x] M4：定向回归、远程只读核查与真实页面 E2E 闭环完成。
- [x] M5：测试阶段停用偏差阈值并复验现有证据导出按钮。
- [x] M6：静态代码分析、阻塞问题修复及独立复审放行。

## Expected Verification

- MES 签名服务定向测试。
- Runtime Control 时间解析、巡检聚合和 ZIP 合同测试。
- 前端静态测试。
- 用户明确授权后才执行真实 E2E 和远程服务器验证。

## Design Constraints Check

- `signedAt` 是服务器生成的正式签名时间。
- `selectedSignedAt` 只表示业务发生时间，不得覆盖正式签名展示。
- 导出只读取指定巡检 ID 的已保存结果，不重新巡检。
- 缺少时间证据时明确失败，不生成默认通过报告。
- 不新增数据库表、可信时间平台、数字签名或 WORM。

## Current Status

ready_for_closeout：P6 静态代码分析已通过第三轮独立复审；远程重启仍等待目标及 `PROD` 确认。

## Cleanup Keep

- doc/tasks/20260907-trusted-time-audit-evidence/prd.md
- doc/tasks/20260907-trusted-time-audit-evidence/development-plan.md
- doc/tasks/20260907-trusted-time-audit-evidence/test-plan.md
- doc/tasks/20260907-trusted-time-audit-evidence/task-state.json
- doc/tasks/20260907-trusted-time-audit-evidence/test-report.md
