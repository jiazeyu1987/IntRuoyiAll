# Verification Report

## Summary

- 修复点：提交失败时，结果弹窗不再只显示“提交失败”，会展示 `resolveErrorMessage` 解析出的真实失败原因。
- 范围：`IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`。
- 状态：实现、定向验证、cleanup 和经验沉淀通过；等待提交和推送。

## Bug

提交失败主结果弹窗只显示“提交失败”，缺少具体失败原因；用户无法判断是签名、审批、门禁还是后端业务规则导致失败。

## Expected

提交失败弹窗必须在失败状态之外显示真实失败原因，错误文本来自外层 catch 的 `resolveErrorMessage`，不得用默认失败状态掩盖。

## Reproduction

- 用户截图路径：eDHR 填写页提交失败后，弹窗仅显示“刘子良 提交失败”。
- 静态复现：旧实现中 `catch (error)` 已计算 `submitErrorMessage`，但只调用 `showFillActionResultDialog('submit-failed')`，弹窗状态没有失败原因参数。

## Root Cause

提交失败真实原因只进入 toast；主结果弹窗状态没有稳定的失败原因字段，也没有从 catch 把 `submitErrorMessage` 传入弹窗。

## Commands

- RED: `node tests/e2e/edhr-fill-workspace-action-result-dialog-static.spec.js` -> FAIL，缺少 `showFillActionResultDialog('submit-failed', submitErrorMessage)`。
- GREEN: `node tests/e2e/edhr-fill-workspace-action-result-dialog-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-fill-workspace-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue` -> PASS。

## Verification

- `node tests/e2e/edhr-fill-workspace-action-result-dialog-static.spec.js` -> PASS。
- `node tests/e2e/edhr-fill-workspace-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue` -> PASS。

## Runtime Evidence

- 前端端口：`http://127.0.0.1:8081/` -> HTTP 200。
- 后端健康：`http://127.0.0.1:48081/actuator/health` -> `UP`。

## Real E2E

- 未运行真实提交失败路径 E2E。
- 原因：真实写入型失败 E2E 需要单独准备可提交执行记录、失败签名场景和清理链路；现有真实脚本只覆盖提交成功/审批策略。为遵守 no-mock、no-API-only 和真实数据清理规则，本次未用接口拦截或 mock 替代真实页面路径。

## Risk

- 风险低：修复仅把已解析出的提交失败错误文本传入同一结果弹窗并渲染，成功状态显式清空失败原因。

## Blockers

- 无实现阻塞。
- 真实提交失败路径 E2E 未运行，原因见 `Real E2E`；当前验收以聚焦静态合同、相邻静态合同和类型检查为准。
