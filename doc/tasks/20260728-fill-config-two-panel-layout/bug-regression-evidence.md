# Bug Regression Evidence

## Bug Summary

Vite HMR reported `[plugin:vite:vue] Element is missing end tag` for `IntRuoyiFronted/src/views/form-center/template/components/FormTemplateFillConfigDialog.vue` at the newly introduced `<main class="batch-record-cell-rules-editor__main-panel">`.

## Expected Behavior

The form-template fill-config dialog must compile as a valid Vue SFC, keep the left preview in the `main` panel, keep all field / assist-row configuration and save actions in the right `aside` panel, and preserve the existing template-owned save contract.

## Reproduction

RED: Vite HMR overlay（user-provided）-> FAIL, expected reason: `Element is missing end tag` at `FormTemplateFillConfigDialog.vue:10:7`.

RED ATTEMPT: `pnpm exec eslint --ext .vue src/views/form-center/template/components/FormTemplateFillConfigDialog.vue` -> TIMEOUT before the fix attempt completed; not accepted as a pass signal.

## Root Cause

The two-panel template restructuring moved the side configuration panel out of the original workspace and left the `main` / `aside` wrapper structure without a compiler-level static guard. The source now normalizes the wrapper closure and the regression test compiles the Vue template directly so missing end tags fail before Vite HMR.

## Regression Test

Updated `IntRuoyiFronted/tests/e2e/form-template-fill-config-static.spec.js` to resolve the same Vue compiler used by `@vitejs/plugin-vue`, parse `FormTemplateFillConfigDialog.vue`, and compile its template.

## Verification

GREEN: `node tests/e2e/form-template-fill-config-static.spec.js` -> PASS.

GREEN: Vite 同源 `@vue/compiler-sfc` parse + template compile for `FormTemplateFillConfigDialog.vue` -> PASS.

GREEN: `pnpm exec eslint --ext .vue src/views/form-center/template/components/FormTemplateFillConfigDialog.vue` -> PASS.

GREEN: `pnpm ts:check` -> PASS.

## GREEN Evidence

- `node tests/e2e/form-template-fill-config-static.spec.js` -> PASS.
- Vite 同源 `@vue/compiler-sfc` parse + template compile for `FormTemplateFillConfigDialog.vue` -> PASS.
- `pnpm exec eslint --ext .vue src/views/form-center/template/components/FormTemplateFillConfigDialog.vue` -> PASS.
- `pnpm ts:check` -> PASS.

## Risk And Scope

Risk is limited to the form-template fill-config dialog layout and its static contract. No API wrapper, backend route, permission, save payload, mock data, or batch-record report linkage was changed.

## Blockers

Task closeout, commit, and push are pending because the shared `int_main` workspace contains many unrelated concurrent dirty changes outside this overlay repair.
