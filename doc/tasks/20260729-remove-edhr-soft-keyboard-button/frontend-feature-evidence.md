# Frontend Feature Evidence

## Feature Goal

Remove the eDHR fill workspace custom soft keyboard button and popup implementation.

## Non-Goals

- Do not change backend APIs, permissions, form values, submit/save behavior, `assistRows`, `formBindings`, batch record form binding, or process-start configuration.
- Do not replace the deleted button with fallback UI, mock behavior, or hidden controls.

## Requirements And Acceptance

- R1: The left rail no longer renders the soft keyboard icon button in the red-box position.
- R2: The page no longer renders a soft keyboard popover or key controls.
- R3: Soft keyboard state, key rows, focus tracking, input insertion handlers and CSS are removed.
- R4: Existing display/fill mode buttons and save/submit/fullscreen actions remain.

## UI Entry Points

- Route/component scope: `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`
- Test scope: `IntRuoyiFronted/tests/e2e/edhr-soft-keyboard-button-static.spec.js`

## API Contracts And Data States

- No backend API changes.
- No persistence changes.
- No permission changes.

## BDD Scenarios

- BDD: remove soft keyboard entry -> Given the eDHR fill workspace left rail renders, When the user views the former red-box position, Then no soft keyboard icon button or popover entry is rendered.
- BDD: remove soft keyboard implementation -> Given the eDHR execution page source is loaded, When static contracts inspect it, Then `softKeyboard*`, keyboard rows, input insertion handlers and soft keyboard CSS are absent.
- BDD: preserve fill workspace controls -> Given the soft keyboard is removed, When the fill workspace renders, Then display mode, fill mode, save, submit, fullscreen and assist switching controls remain.

## Verification Plan

- RED/GREEN static contract: `node tests/e2e/edhr-soft-keyboard-button-static.spec.js`
- Adjacent regression contract: `node tests/e2e/edhr-assist-fill-mode-static.spec.js`
- TypeScript check: `pnpm ts:check`
- Evidence validation: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260729-remove-edhr-soft-keyboard-button/frontend-feature-evidence.md`

## Verification Results

- RED: `node tests/e2e/edhr-soft-keyboard-button-static.spec.js` failed before implementation because `edhr-fill-workspace__soft-keyboard-section` still existed.
- GREEN: `node tests/e2e/edhr-soft-keyboard-button-static.spec.js` passed.
- GREEN: `node tests/e2e/edhr-assist-fill-mode-static.spec.js` passed.
- GREEN: `pnpm ts:check` passed.
- GREEN: source residual scan for `softKeyboard`, `soft-keyboard`, `keyboard-outline`, `data-soft-keyboard`, `打开软键盘`, `关闭软键盘` returned no matches in `ExecutionPage.vue`.
- Real E2E: not run; this is a deletion-only local UI cleanup with no runtime, data, permission, save or submit path changes.

## Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- No loading, empty, error, permission or data states are changed.
- Removing the button removes its accessibility surface; existing controls keep their labels and behavior.
- No local runtime or real E2E path is required for this deletion-only UI cleanup unless static/type checks reveal a behavior risk.

## Blockers

- None currently.

