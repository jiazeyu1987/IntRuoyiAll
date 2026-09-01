# Verification Report

## Result

PASS - the approved frontend and backend code was committed and pushed to `origin/int_main`.

## Commit Scope

- Commit: `11b1b97ca feat: update frontend and backend workflows`
- Scope: 99 files only under `IntRuoyiBackend/` and `IntRuoyiFronted/`.
- Excluded: root documentation, `AGENTS.md`, pytest temporary output, `LOG_FILE_IS_UNDEFINED` files, and resource images.

## Verification Evidence

- `git diff --cached --check` -> PASS.
- Frontend changed static contracts -> PASS.
- Frontend changed JavaScript syntax checks -> PASS.
- `pnpm ts:check` -> PASS.
- `python -m pytest -q script/tests/test_dcc_registration_certificate_business_event_notify_template_sql.py script/tests/test_dcc_registration_certificate_change_approval_mvp_sql.py` -> PASS, 4 passed.
- `node script/tests/invoice-voucher-print-kingdee-config-bridge-static.test.mjs` -> PASS.
- `mvn -pl yudao-module-bpm,yudao-module-dcc,yudao-module-erp,yudao-module-mes,yudao-module-system -am -DskipTests compile` -> PASS.
- `scripts\\preflight\\branch-runtime-port-guard.ps1` -> PASS for `int_main`, frontend 8081 and backend 48081.
- Staged and outgoing-history 100 MB object scans -> PASS.
- `git push origin int_main` -> PASS, `190d50a42..11b1b97ca`.

## Known Non-Blocking Items

- The Maven compiler emitted existing source/varargs warnings but completed with `BUILD SUCCESS`.
- The remaining dirty files are outside the authorized frontend/backend scope and were not staged or modified by this task.
