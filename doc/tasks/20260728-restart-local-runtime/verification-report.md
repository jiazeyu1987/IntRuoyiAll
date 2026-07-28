# Verification Report

## Result

BLOCKED

## Evidence

- Port precheck: `8081` and `48081` were not listening before restart.
- Restart command: `restart-int-ruoyi-local.ps1 -Component full -WorktreeName int_main`.
- Restart result: failed fast with `Required Docker container is not running: int-ruoyi-mysql`.
- Docker read-only status: `int-ruoyi-mysql` exists but is stopped: `Exited (255) 35 minutes ago`.

## Impact

- Backend was not restarted because the required local MySQL Docker container is stopped.
- Frontend was not restarted by the full script because the script fails before frontend dispatch when backend preflight fails.

## Follow-Up Required

- User authorization is required before starting or repairing local Docker dependencies, then rerun the same standard full restart flow.
