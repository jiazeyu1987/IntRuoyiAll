# Bug Regression Evidence

## Bug Summary

eDHR 填写工作台左侧栏仍显示“关闭前可修改”和“金手指测试权限”两条说明，占据用户截图红框区域。

## Expected Behavior

左侧栏不渲染这两条说明性提示；真实错误、字段审计门禁和版本锁定告警继续显示。

## Reproduction

`node tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js`

## Root Cause

`ExecutionPage.vue` 左侧栏显式渲染 `preReleaseEditNotice` 与 `goldenFingerNotice` 两个说明性 `el-alert`，既有隐藏红框合同未覆盖这两个节点。

## Regression Test

扩展 `edhr-fill-workspace-hide-side-panels-static.spec.js`，断言左侧栏不包含两个说明性提示绑定，同时继续包含版本锁定、字段审计门禁和保存错误告警。同步更新 `edhr-golden-finger-static.spec.js`，保留权限行为合同但禁止侧栏提示。

## RED

RED: `node tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js` -> FAIL，首个失败为左侧栏仍包含 `v-if="preReleaseEditNotice"`。

## GREEN

GREEN: `node tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js` -> PASS。
GREEN: `node tests/e2e/edhr-golden-finger-static.spec.js` -> PASS。
GREEN: `node tests/e2e/edhr-execution-fill-workspace-submit-static.spec.js` -> PASS。

## Verification

- `node tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js; node tests/e2e/edhr-golden-finger-static.spec.js; node tests/e2e/edhr-execution-fill-workspace-submit-static.spec.js; node tests/e2e/edhr-fill-workspace-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。
- `pnpm build:local` -> FAIL，Vite/Rollup 在产物生成后返回 `TypeError: Cannot set property code of  which has only a getter`，退出码 1。

## Risk And Regression Scope

改动范围限定为 `ExecutionPage.vue` 左侧栏提示节点、两个仅展示用计算属性和对应聚焦静态合同。金手指权限、提交门禁、版本锁定与错误告警逻辑未改变。

## Blockers And Follow-up

暂无。
