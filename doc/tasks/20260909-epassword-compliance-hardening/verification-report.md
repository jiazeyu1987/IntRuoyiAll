# Verification Report

## Summary

本次整改已完成定向验证和静态分析修复：正式电子签名禁止人工选择/回填签名时间；原生 BPM 待办审批签名缺少流程实例时拒绝生成正式签名；非流程直签业务不被 BPM 顺序门禁误伤；eDHR Stage1 模拟记录改为 `SIMULATION_SESSION`，草稿保存记录改为 `DRAFT_SESSION`，降低与正式电子签名混淆风险。

## Passed Verification

- PASS after rebase onto `int_main` (`9ce630b8e`):
  - `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1`
  - `git diff --check`
  - `node tests/e2e/edhr-formal-signature-time-compliance-static.spec.js`
  - `node tests/e2e/edhr-signature-time-optional-static.spec.js`
  - `node tests/e2e/edhr-tail-four-goals-static.spec.js`
  - `node --check tests/e2e/edhr-tail-four-goals-real-flow.e2e.js`
  - `pnpm ts:check`
  - `mvn -pl yudao-module-bpm -am '-Dtest=ApprovalSignatureRecordServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`，5 tests
  - `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordExecutionSignatureServiceTest,MesProBatchRecordExecutionFieldAuditServiceTest#saveChanges_withSelectedSignatureTimePassesTimeCommandAndStoresDualTimeSignature' '-Dsurefire.failIfNoSpecifiedTests=false' test`，15 tests
- PASS: `node tests/e2e/edhr-formal-signature-time-compliance-static.spec.js`
- PASS: `node tests/e2e/edhr-signature-time-optional-static.spec.js`
- PASS: `node tests/e2e/edhr-tail-four-goals-static.spec.js`
- PASS: `node --check tests/e2e/edhr-tail-four-goals-real-flow.e2e.js`
- PASS: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordExecutionSignatureServiceTest#recordSubmitSignature_withSelectedTimeFailsFast' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- PASS: `mvn -pl yudao-module-bpm -am '-Dtest=ApprovalSignatureRecordServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`，5 tests
- PASS: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordExecutionSignatureServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`
- PASS: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordExecutionSignatureServiceTest,MesProBatchRecordExecutionFieldAuditServiceTest#saveChanges_withSelectedSignatureTimePassesTimeCommandAndStoresDualTimeSignature' '-Dsurefire.failIfNoSpecifiedTests=false' test`，15 tests
- PASS: `pnpm install --frozen-lockfile`
- PASS: `pnpm ts:check`
- PASS: `git diff --check`

## Static Analysis Findings

- FIXED: `ApprovalSignatureRecordServiceImpl` 原先将 `processInstanceId` 设为所有 BPM 模块签名必填，静态调用点分析发现会误伤 `BUSINESS_APPROVAL_POLICY_SWITCH` 等无流程实例的直签业务；已收口为仅 `BPM/BPM_TASK_TODO` 原生有序审批强制流程实例。
- FIXED: `signatureTime.ts` 保留了已停用的人工签名时间表单构造、时区解析和格式化方法；已删除死代码，正式签名请求统一返回 `undefined`，不再生成 `selectedSignedAt` 载荷。
- FIXED: `Stage1` 模拟记录授权说明仍含“正式签名记录”字样；已改为“非正式模拟审计记录”，并增加测试防止回退。
- FIXED: 草稿字段变更保存仍使用 `LOGIN_SESSION` 签名模式；已改为 `DRAFT_SESSION`，避免被审查口径误判为正式电子签名。
- FIXED: 历史 E2E/静态脚本仍要求选择人工签名时间或读取已删除组件路径；已按当前合规目标更新为“不出现人工签名时间控件、请求不携带人工签名时间载荷”。

## Resolved Verification Blocker

- RESOLVED: `pnpm ts:check` 初次被前端 worktree 缺少 `node_modules/cross-env` 阻塞；已按锁文件执行 `pnpm install --frozen-lockfile` 补齐当前 worktree 依赖，随后 `pnpm ts:check` 通过。`node_modules` 为 ignored 产物，不进入提交清单。

## Merge Blocker

- BLOCKED: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260909-epassword-compliance-hardening --mode preview` 返回 `Main worktree is dirty and cannot receive ff-only merge: E:\IntRuoyi`。主工作区存在多项与本任务无关的 tracked/untracked 改动；按 worktree closeout 规则，未执行 cleanup apply、未快进合并、未删除 worktree。

## E2E Status

未执行真实 Playwright E2E。本轮用户要求为“静态代码分析”，未明确要求 E2E；项目规则要求 E2E 仅在当轮明确要求时执行。
