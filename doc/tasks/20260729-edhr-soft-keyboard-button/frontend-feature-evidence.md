# Frontend Feature Evidence

## Feature Goal

Add a soft keyboard icon button to the eDHR fill workspace left rail red-box area and open a page-local soft keyboard panel on click.

## Non-Goals

- Do not change backend APIs, batch record data contracts, `assistRows`, `formBindings`, route process binding, permissions, or submit/save behavior.
- Do not introduce mock data, compatibility fallback, or hidden default success.

## Requirements And Acceptance

- R1: The fill workspace left rail includes a keyboard icon button in the empty red-box area below existing display/fill mode controls.
- R2: Clicking the button opens a soft keyboard panel inside the current page.
- R3: The panel provides stable controls for numbers, common letters, space, delete and close.
- R4: Clicking keys writes to the currently focused editable input/textarea/contenteditable field and dispatches normal input/change events.

## UI Entry Points

- Route/component scope: `IntRuoyiFronted/src/views/mes/pro/edhr/ExecutionPage.vue`
- User-visible area: eDHR fill workspace left rail.

## API Contracts And Data States

- No backend API changes.
- No persistence changes.
- No permission changes.

## BDD Scenarios

- BDD: soft keyboard sidebar entry -> Given fill workspace mode, When the rail renders, Then the keyboard icon entry is visible and existing controls remain.
- BDD: soft keyboard popup -> Given the entry is visible, When clicked, Then the soft keyboard panel is rendered with close/delete/space and key buttons.
- BDD: soft keyboard input -> Given a field is active, When keys are clicked, Then active editable content updates through normal DOM events.

## Verification Plan

- RED/GREEN static contract: `node tests/e2e/edhr-soft-keyboard-button-static.spec.js`
- Adjacent regression contract: `node tests/e2e/edhr-assist-fill-mode-static.spec.js`
- TypeScript check: `pnpm ts:check`
- Evidence validation: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260729-edhr-soft-keyboard-button/frontend-feature-evidence.md`

## Verification Results

- RED: `node tests/e2e/edhr-soft-keyboard-button-static.spec.js` failed before implementation on missing soft keyboard section.
- GREEN: `node tests/e2e/edhr-soft-keyboard-button-static.spec.js` passed.
- GREEN: `node tests/e2e/edhr-assist-fill-mode-static.spec.js` passed.
- GREEN: `pnpm ts:check` passed.
- Real E2E: not run; no local frontend/backend runtime was started or modified for this single-component UI change.

## Responsive, Accessibility, State Checks

- Button has accessible label/title.
- Panel remains page-local and can be closed.
- Popover uses `:teleported="false"` so it stays inside the fill workspace/fullscreen context.
- Existing left rail action layout remains intact.
- Empty/loading/error states are unchanged because no data loading path changes.

## Blockers

- None currently.
