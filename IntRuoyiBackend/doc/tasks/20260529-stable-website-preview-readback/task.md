# 20260529-stable-website-preview-readback

## Task Goal

Make local showroom publish verification rely on a stable Website preview runtime instead of the Vite dev server, and ensure local runtime-control does not return before the Website readback endpoint is actually ready.

## Milestones

- [x] Create task record before code changes.
- [x] Capture RED evidence for current unstable Website startup assumptions.
- [x] Update runtime-control/startup tooling to align with the stable Website preview runtime.
- [x] Re-run targeted script and backend verification.
- [x] Record final verification and completion status.

## Expected Verification

- `python -m pytest script/tests/test_runtime_control_scripts.py -q`
- `powershell -ExecutionPolicy Bypass -File .\script\deploy\restart-int-ruoyi-local.ps1 -Component website`
- `Invoke-WebRequest http://127.0.0.1:4173/showroom/sites/yingtai-showroom/stages/TEST/release/current`

## Current Status

Completed.
