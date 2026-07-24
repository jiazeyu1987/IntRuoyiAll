# Execution Log

BDD: local publish readback targets the canonical backend public release API -> Given local manual showroom publish runs inside the same machine as the backend / When publish evaluates success in local runtime / Then the readback verifier should target the canonical backend public release API origin instead of the local Website proxy shell.

BDD: local Website preview stays independently probeable -> Given local Website `4173` still serves the real public shell for browsing / When runtime-control starts the Website component / Then it should still wait for `4173` scoped current-release readiness, but publish success should not depend on a self-proxy loop through that shell.

RED: backend log inspection -> FAIL, local manual publish on `2026-05-29 20:47:31 +08:00` failed with `SHOWROOM_RELEASE_PUBLIC_READBACK_FAILED ... http://127.0.0.1:4173/showroom/sites/yingtai-showroom/stages/TEST/release/current: request timed out`; the same log window contained no matching backend `/showroom/sites/.../release/current` request, proving the timeout happened in the `4173 -> proxy -> 48081` loop before the request re-entered the backend servlet pipeline.

GREEN: `python -m pytest script/tests/test_runtime_control_scripts.py -q` -> PASS, `6` tests.

GREEN: local backend restart with no external DCC env required -> PASS, `restart-int-ruoyi-local.ps1 -Component backend` now injects explicit local DCC signature evidence runtime config and starts `48081` with `--showroom.release.public-website-origin=http://127.0.0.1:48081`.

GREEN: `Invoke-WebRequest http://127.0.0.1:48081/actuator/health` -> PASS, `{"status":"UP"}`.

GREEN: real publish API verification -> PASS, local authenticated `POST /admin-api/showroom/release/publish` returned business `code=0` with release `20260529T125938Z-2c8e98f943b3`.

GREEN: `Invoke-WebRequest http://127.0.0.1:48081/showroom/sites/yingtai-showroom/stages/TEST/release/current` -> PASS, returned release `20260529T125938Z-2c8e98f943b3`.

GREEN: `Invoke-WebRequest http://127.0.0.1:4173/showroom/sites/yingtai-showroom/stages/TEST/release/current` -> PASS, returned the same release `20260529T125938Z-2c8e98f943b3`.
