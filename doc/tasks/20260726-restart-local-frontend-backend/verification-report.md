# Verification Report

## Result

PASS

## Runtime Ownership

- Frontend: `8081`, PID `55676`, `node.exe` Vite process under `E:\IntRuoyi\IntRuoyiFronted`.
- Backend: `48081`, PID `53292`, `java.exe` running `yudao-server-exec.jar` under `E:\IntRuoyi\IntRuoyiBackend`.

## Endpoint Verification

- `http://127.0.0.1:8081/`: three consecutive HTTP `200` responses.
- `http://127.0.0.1:48081/actuator/health`: three consecutive `status=UP` responses.

## Constraints

- No fallback, port substitution, mock service, data-source switch, shared configuration edit, or unknown-process termination was used.
- Runtime logs are under `.runtime/local-int-main/` and remain attached to the running services.
- Closeout preview/apply completed with no deleted paths, blockers, or warnings.
- After this verification, control of backend port `48081` was handed to the eDHR fix task; this task will not restart that port again.

## Closeout Guard

- The initial mandatory guard run failed while the concurrent port-governance task was between script and contract updates.
- After that task synchronized `2026-07-26-branch-runtime-v3`, the guard passed for `int_main` with frontend `8081` and backend `48081`.
- No concurrent task file was modified or staged as a workaround.

## Git Closeout

- Runtime verification commit: `c3a2bcfb`.
- Closeout commit: `7afe6429`.
- `origin/int_main` pushes passed and the branch was no longer ahead after the closeout push.
