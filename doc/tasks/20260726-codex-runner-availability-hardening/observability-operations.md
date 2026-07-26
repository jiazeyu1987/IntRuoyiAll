# Observability Operations Evidence

## Critical Signals

- Runner online count and stale runner count.
- Latest heartbeat time and heartbeat age seconds.
- Required capability presence for `playwright` and `codex`.
- Current running count.
- Startup prerequisite failures for Node, Codex CLI, token, backend health, and frontend entry.

## Runbook

1. Check the page Runner status strip.
2. If stale, run `IntRuoyiFronted/scripts/start-codex-test-runner.ps1` with a valid token source. Use `-RestartExisting` only after confirming the existing process is the project Runner and stale.
3. If registration fails with invalid token, restart the local backend with the same Runner token and verify backend health.
4. Verify DB heartbeat age remains below `yudao.codex-test.runner.heartbeat-timeout-seconds` after at least one idle heartbeat interval.

## Verification

- Runtime Runner session `id=8` heartbeat age was `3` seconds after a 25 second idle wait.