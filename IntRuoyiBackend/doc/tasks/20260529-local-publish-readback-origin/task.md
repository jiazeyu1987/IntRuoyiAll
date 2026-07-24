# 20260529-local-publish-readback-origin

## Task Goal

Remove the local manual-showroom-publish dependency on the `4173` Website proxy loop by making local publish readback verify against the canonical backend public release API on `48081`, while keeping Website `4173` as a separately-probed consumer runtime.

## Milestones

- [x] Create task record before code changes.
- [x] Capture RED evidence for the current local `4173 -> proxy -> 48081` timeout failure mode.
- [x] Change local runtime startup so `showroom.release.public-website-origin` points at the canonical backend public release API.
- [x] Keep independent Website startup/readiness probing for `4173`.
- [x] Re-run targeted verification and record final status.

## Expected Verification

- `python -m pytest script/tests/test_runtime_control_scripts.py -q`
- `Invoke-WebRequest http://127.0.0.1:48081/showroom/sites/yingtai-showroom/stages/TEST/release/current`
- `Invoke-WebRequest http://127.0.0.1:4173/showroom/sites/yingtai-showroom/stages/TEST/release/current`

## Current Status

Completed.
