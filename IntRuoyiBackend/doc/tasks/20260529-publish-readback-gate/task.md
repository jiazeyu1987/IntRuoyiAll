# 20260529-publish-readback-gate

## Task Goal

Make showroom publish and deployment succeed only when the public Website entry can read the scoped current release, preventing stale Website data after manual publish.

## Milestones

- [x] Add failing backend publish read-back and current no-store tests.
- [x] Add failing deployment tooling test for public scoped Website smoke checks.
- [x] Implement backend read-back verifier and no-store current response.
- [x] Implement deployment smoke check.
- [x] Run targeted verification and record evidence.
- [x] Run task closeout cleanup preview.

## Expected Verification

- Manual publish verifies public Website current release before returning success.
- Current-release response is not cached by HTTP caches.
- Deployment script checks the scoped current release through the Website host port after restart.

## Current Status

Completed.

## Final Verification

- PASS: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomPublicReleaseReadbackVerifierTest,ShowroomReleaseAdminPublishIntegrationTest,ShowroomReleaseCurrentApiTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> 12 tests passed.
- PASS: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> 26 tests passed.
- PASS: `python -X utf8 C:/Users/BJB110/.codex/skills/task-closeout-cleanup/scripts/task_closeout.py --task-id 20260529-publish-readback-gate --mode preview` -> no deletes, no blockers.

## Constraints

- No fallback success or silent downgrade.
- Do not mutate live IntRuoyi business data.
- Preserve unrelated working tree changes.
