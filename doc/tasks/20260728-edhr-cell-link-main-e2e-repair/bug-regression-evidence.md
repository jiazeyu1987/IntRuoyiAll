# Bug Regression Evidence

## Bug Summary

主端口 `int_main` 复验发现 eDHR 执行页仍调用 `/batch-record-cell-link/prefill` 并把未落库值注入本地草稿，违反“创建/打开执行记录时自动落库预填值”的正式语义。

## Expected Behavior

执行页只从执行详情已保存的 `detail.cellValues` / `cellValuesJson` hydrate 草稿状态；如果后端没有落库，前端不得调用 `/prefill` 兜底展示成功态。

## Reproduction

- RED: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js` -> FAIL。

## Root Cause

待修复后补充。

## Regression Test

- `IntRuoyiFronted/tests/e2e/edhr-cell-link-auto-persist-static.spec.js`

## RED

- RED: `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js` -> FAIL，执行页仍保留旧 `/prefill` 草稿注入路径。

## GREEN

- 待补充。

## Risk And Regression Scope

风险集中在 eDHR 执行页草稿 hydrate；修复不得改变已保存执行详情、字段审计、附件和只读追踪模式的既有读取链路。

## Blockers

- 真实 Playwright E2E 仍缺少当前本地数据库可打开正式批记录任务夹具。
