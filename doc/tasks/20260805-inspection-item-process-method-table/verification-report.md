# Verification Report

## Summary

QA 规程“检验项目”页签已改为工序检验方法表，默认展示工序、检验项目、接受标准、检验方法、检验器具及设备、抽样方案，并用新表 key 隔离旧列配置。

## Verification Commands

- RED: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> FAIL, missing process-method table key and target columns.
- GREEN: `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS.
- REGRESSION: `node tests/e2e/qa-regulation-final-applicability-static.spec.cjs` -> PASS.
- REGRESSION: `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js` -> PASS.
- TYPECHECK: `pnpm ts:check` -> PASS.
- FORMAT: `git diff --check` -> PASS.
- EVIDENCE: `validate_bug_regression.py --evidence doc/tasks/20260805-inspection-item-process-method-table/bug-regression-evidence.md` -> PASS.
- EVIDENCE: `validate_frontend_feature.py --evidence doc/tasks/20260805-inspection-item-process-method-table/frontend-feature-evidence.md` -> PASS.

## Residual Blockers

- `node tests/e2e/role-matrix-qa-regulation-static.spec.cjs` -> FAIL, unrelated existing fixture assertion: M6 QA/PQC SQL fixture lacks the expected `tmp_rrm_reset_pqc_task` freeze/reset block.
- Git closeout blocker: branch `int_main` is already ahead of `origin/int_main` and contains staged/unstaged unrelated task material; pushing or committing as part of this task would mix task ownership.

## Final Result

Implementation, required targeted verification, and task-closeout cleanup are complete. Final commit/push is blocked by unrelated local ahead commits plus staged/unstaged unrelated task material.
