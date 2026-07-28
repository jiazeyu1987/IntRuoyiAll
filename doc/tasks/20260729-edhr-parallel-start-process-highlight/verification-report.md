# Verification Report

## Summary

PASS。批次执行详情页已按后端任务门禁 `available=true` 标记所有当前可执行工序组；“工序开始”后的并行第一组工序会一起显示黄色运行态。填写权限仍由 `OPEN_FORM` 控制。

## Commands

- `node tests/e2e/edhr-batch-parallel-current-process-highlight-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-admin-current-process-highlight-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-process-state-background-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-admin-filler-visibility-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-process-companion-forms-static.spec.js` -> PASS
- `node tests/e2e/edhr-batch-product-info-virtual-process-static.spec.js` -> PASS
- `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_allowsValidMultiStartMergeRouteGraphWhenBatchBindingsExist" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- `pnpm ts:check` -> PASS
- clean worktree `node tests/e2e/edhr-batch-parallel-current-process-highlight-static.spec.js` -> PASS
- clean worktree `node tests/e2e/edhr-batch-admin-current-process-highlight-static.spec.js` -> PASS
- clean worktree `node tests/e2e/edhr-batch-process-state-background-static.spec.js` -> PASS
- clean worktree `node tests/e2e/edhr-batch-admin-filler-visibility-static.spec.js` -> PASS
- clean worktree `node tests/e2e/edhr-batch-process-companion-forms-static.spec.js` -> PASS
- clean worktree `node tests/e2e/edhr-batch-product-info-virtual-process-static.spec.js` -> PASS
- clean worktree `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openOrCreate_allowsValidMultiStartMergeRouteGraphWhenBatchBindingsExist" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- clean worktree `pnpm install --frozen-lockfile --reporter append-only` -> PASS
- clean worktree `pnpm ts:check` -> PASS
- `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS
- `git push origin HEAD:int_main` -> PASS，implementation commit `6423023d`

## Evidence

- `isCurrentExecutableProcessGroup` 读取任务 `available === true`，并要求未完成、非可选。
- `resolveProcessGroupStateClass` 在完成态之后优先把当前可执行工序组标为 `is-in-progress`。
- `isProductInfoProcessGroup` 仍排除产品信息虚拟工序。
- `canOpenTask` 仍检查 `hasAllowedTaskAction(row, 'OPEN_FORM')`。

## Residual Risk

真实登录态 Playwright 未运行；本任务没有启动本地服务，也没有写入业务数据。当前验证覆盖前端状态逻辑、相邻静态合同、后端多起点路线创建回归、前端类型检查和提交前端口守卫。
