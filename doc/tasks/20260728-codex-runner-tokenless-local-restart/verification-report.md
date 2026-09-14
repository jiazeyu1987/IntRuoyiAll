# Verification Report

## Current Result

PASS。

## Evidence

- Static RED confirmed the previous local restart script still managed and injected `CODEX_TEST_RUNNER_TOKEN`.
- `restart-int-ruoyi-local.ps1` now defaults local backend restart to tokenless Runner mode: no token file, no random token generation, no parent-process Runner token injection, and Java startup clears inherited `Env:\CODEX_TEST_RUNNER_TOKEN`.
- `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_runtime_control_scripts.py -q` passed with 15 tests.
- `node IntRuoyiFronted\tests\e2e\codex-runner-on-demand-startup-script-static.spec.js` passed.
- `node IntRuoyiFronted\tests\e2e\codex-test-runner-http-client-static.spec.js` passed.
- Target Maven tokenless Runner tests passed with 2 tests.
- Active int_main backend was restarted on `48081` from `backend-tokenless-local-restart-20260728-234110.jar`; health is `UP`.
- Tokenless Runner register probe without `X-Codex-Runner-Token` returned business `code=0`.
- Real Runner process is running from the workspace script, and stderr contains no token-invalid error after restart.

## Runtime Artifact

- Jar: `E:\IntRuoyi\output\runtime\int_main\backend-tokenless-local-restart-20260728-234110.jar`
- SHA256: `cc90619251f9275331a8994661fabe10c6aef396ad4b8ac36ed0ddb547074983`
- Backend PID: `49968`
- Runner PID: `34272`

## Boundary

This is not API-only execution and not mock success. The system still uses the backend-controlled Runner protocol, registration, claim, heartbeat, and structured result write-back; the removed requirement is only the local Runner token precondition requested by the user.

## Cleanup

- Preview: PASS，only the task-owned temporary restart helper was selected for deletion.
- Apply: PASS，temporary helper deleted; task records retained.
