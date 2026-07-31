# Bug Regression Evidence

## Bug Summary

Vite ESLint overlay failed on `FormTemplateFillConfigDialog.vue` with `vue/no-dupe-keys` because the component declared both an `assistRows` prop and a local `assistRows` ref. In `<script setup>`, both names are exposed to the script/template scope, so the duplicate key can collide at runtime.

## Expected Behavior

The form-template fill-config dialog should compile without duplicate exposed keys while preserving the public `assistRows` prop, emitted `assistRows` save payload, and template `jimuSchemaJson` field names.

## Reproduction

RED: `pnpm exec eslint --ext .vue src/views/form-center/template/components/FormTemplateFillConfigDialog.vue` -> FAIL, expected reason: duplicate key `assistRows` at line 521.

## Root Cause

The component used `assistRows` for both inbound props and editable local dialog state. The saved schema contract also uses `assistRows`, so the fix must rename only the local editable state and not the external prop or payload fields.

## Fix

Renamed the local ref to `editableAssistRows` and updated only internal template/script references. The `props.assistRows` input and emitted `assistRows` output remain unchanged.

## Verification

GREEN: `pnpm exec eslint --ext .vue src/views/form-center/template/components/FormTemplateFillConfigDialog.vue` -> PASS.

GREEN: `node tests/e2e/form-template-fill-config-static.spec.js` -> PASS.

GREEN: `pnpm ts:check` -> PASS.

NOTE: Direct `pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json --pretty false` failed with Node heap OOM because it bypassed the repo script's `NODE_OPTIONS=--max-old-space-size=8192`; rerun through `pnpm ts:check` passed.

## Risk And Regression Scope

Risk is low because the change is a local variable rename only. Regression scope covers the Vite ESLint overlay, the form-template fill-config static contract, and relaxed TypeScript checking.

## Blockers

No blocker for this lint regression. The broader `20260728-form-template-fill-config` task still has its pre-existing unrelated `form-center-static` route `activeMenu` blocker and local backend real-E2E blocker.
