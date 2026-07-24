# 20260529-runtime-load-latest-code

## Task Goal

Verify and restore the current IntRuoyi backend and Website runtime so the running services, not only the git repositories, load the latest showroom publish fixes.

## Milestones

- [x] Create task record before runtime/config work.
- [x] Confirm backend repository and Website repository HEAD revisions.
- [x] Resolve backend startup preconditions without fallback defaults.
- [x] Restart backend and verify `48081` health.
- [x] Verify Website public scoped release path returns JSON, not `index.html`.
- [x] Record evidence, blockers, and final status.

## Expected Verification

- IntRuoyi backend `GET http://127.0.0.1:48081/actuator/health` returns `{"status":"UP"}`.
- Running backend is started from `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` after commit `f89118240f`.
- Website dev server responds from `D:\ProjectPackage\Website`.
- Public scoped release path returns release JSON rather than HTML.

## Current Status

Completed. Local runtime is loading the latest showroom publish fixes, and the test Website scoped release path now returns JSON.

## Completed Work

- Confirmed backend repository HEAD is `f89118240f` and Website repository HEAD is `c9ed5c8`.
- Verified local backend startup had been blocked by missing DCC signature evidence runtime config, then restarted with the already-recorded explicit local config values instead of any fallback default.
- Verified local backend `48081` and local Website dev servers `4173` and `5188` all return the same scoped current-release JSON.
- Reproduced the clean-build startup failure for pure `f89118240f` on port `48082`: Spring could not instantiate `ShowroomPublicReleaseReadbackVerifier` because no explicit injectable constructor was selected.
- Added explicit constructor injection on `ShowroomPublicReleaseReadbackVerifier` and a Spring wiring regression test to lock that behavior.
- Verified the test Website server `172.30.30.58:8083` was still serving `index.html` for `/showroom/sites/.../release/current` because `/opt/intruoyi/runtime/website/nginx.conf` lacked the scoped `/showroom/sites/` proxy block.
- Replaced the remote Website nginx config with the current template-rendered scoped proxy config and recreated the `intruoyi-website` container.
- Verified test backend and Website now return matching scoped current-release JSON for release `20260528T213138Z-0bd139dadc8f`.

## Constraints

- Do not invent missing secrets or silently downgrade DCC signature evidence.
- Do not modify unrelated dirty files.
- Do not mutate IntRuoyi business data.

## Final Verification

- `GET http://127.0.0.1:48081/actuator/health` -> `{"status":"UP"}`.
- `GET http://127.0.0.1:48081/showroom/sites/yingtai-showroom/stages/TEST/release/current` -> JSON release `20260529T062609Z-2c8e98f943b3`.
- `GET http://127.0.0.1:4173/showroom/sites/yingtai-showroom/stages/TEST/release/current` -> same JSON release `20260529T062609Z-2c8e98f943b3`.
- `GET http://127.0.0.1:5188/showroom/sites/yingtai-showroom/stages/TEST/release/current` -> same JSON release `20260529T062609Z-2c8e98f943b3`.
- `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomPublicReleaseReadbackVerifierSpringWiringTest,ShowroomPublicReleaseReadbackVerifierTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 3 tests.
- `GET http://172.30.30.58:8083/showroom/sites/yingtai-showroom/stages/TEST/release/current` -> JSON release `20260528T213138Z-0bd139dadc8f`, matching backend manifest hash `9bb4622a55ccc86e2dfbb422066d5dfd9ae2e67353a6d1958d6b34441c93d992`.
