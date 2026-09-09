# 统一 CSV 页签导出审批证据入口

## Task Goal
将所有导出审批证据的用户入口集中到统一电子签名治理的 CSV 质量包页签下，避免用户分散到 DCC 签名记录、运行控制台、备份计划等页面寻找审查/审批证据导出能力。

## Milestones
- [x] 识别现有导出审批/审查/签名证据入口和权限边界。
- [x] 先写 BDD 场景和 RED 静态契约，锁定统一 CSV 页签应呈现所有入口。
- [x] 实现 CSV 页签统一入口，保持原 API/权限/失败暴露契约。
- [x] 按 2026-09-09 clarified target 将旧页面导出/下载按钮清理到 CSV 质量包。
- [x] 完成项目级前端类型验证和真实页面 E2E 验证。
- [x] 完成项目级/定向 ESLint 验证。

## Expected Verification
- RED/GREEN: 任务专用静态契约验证 CSV 页签包含 DCC 签名证据、可信时间证据、备份审查证据、账号安全、eDHR 签名记录、字段审计、审批顺序、批记录归档和权限矩阵入口。
- REGRESSION: 前端定向测试或静态检查确认导出按钮只出现在 CSV 质量包，旧页面不再残留证据导出/下载按钮，不使用 mock、fetch 绕过或静默成功。

## Current Status
completed

Implementation updated for the clarified 2026-09-09 target: all visible approval/evidence export buttons are centralized in the CSV quality package. Static contract, real Playwright E2E, residual-entry scan, `pnpm ts:check`, Node API ESLint verification, backend targeted tests, MES/ERP compile, diff check and branch port guard pass. Main implementation commit `bcbdee838` has been created; closeout records are complete.

## Design Constraints Check
- 遵守无 fallback、无静默降级、缺少入口/权限/真实路由时 fail fast。
- 不改变既有后端导出 API 合同，导出按钮唯一可见位置为 CSV 质量包；旧业务页面仅保留查看、查询、校验、打印、生成等非导出动作。
- E2E 非本轮明确要求，不作为本次完成门禁；若后续要求 E2E，必须通过 Playwright 真实前端操作。
- 当前工作区已有未提交改动，本任务仅修改任务目录和审批证据入口相关前端/测试文件。
