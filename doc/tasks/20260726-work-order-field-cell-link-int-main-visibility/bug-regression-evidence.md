# Bug Regression Evidence

## Bug Summary

The source selector in the batch record cell link workbench did not show `生产工单`, so `芋道源码/admin` could not choose production work order fields as the left-side source.

## Expected Behavior

The existing source selector must directly include `生产工单`. Selecting it should switch the left pane to production work order fields once the backend runtime provides `sourceFields`.

## Reproduction

- Real path: `芋道源码/admin` -> `MES 系统` -> `eDHR批记录` -> `批记录表单` -> `链接` -> source selector.
- Static reproduction before sync: `rg "PRODUCTION_WORK_ORDER|生产工单" IntRuoyiFronted/src/views/mes/pro/batchrecordcelllink/index.vue` returned no match.

## Root Cause

`int_main` had not received the verified work-order cell-link implementation. The frontend source selector only listed batch record forms and the backend workbench context did not expose production work order source fields.

## Regression Test

- `node tests\e2e\mes\batch-record-cell-link-static.spec.js`
- `node tests\e2e\mes\batch-record-cell-link-work-order-field-readonly.e2e.mjs`

## Verification

- Static contract, frontend type check, backend Maven target tests, and int-main source selector visibility E2E passed.
- The visible-source regression is fixed on `http://127.0.0.1:8081` for `芋道源码/admin`.

## Blockers

- Full readonly field-selection E2E is blocked until `48081` is served by the current `E:\IntRuoyi` backend code.
- Current blocker impact: the source selector option is visible, but the running backend cannot prove `sourceFields` and the production work order field matrix on `int_main`.

## RED / GREEN

- `RED: node tests\e2e\mes\batch-record-cell-link-static.spec.js -> FAIL, source selector lacked the direct 生产工单 option before the verified branch fix`
- `GREEN: node tests\e2e\mes\batch-record-cell-link-static.spec.js -> PASS`
- `GREEN: int-main source selector visibility E2E -> PASS, 芋道源码/admin sees 生产工单 in the source selector on http://127.0.0.1:8081`
- `BLOCKED: full readonly field-selection E2E -> current 48081 backend belongs to another worktree and does not return sourceFields`

## Risk And Scope

- Scope includes batch record cell link workbench source selection, rule save contract, source field persistence, and execution prefill.
- No fallback, mock success, or silent downgrade was added.
