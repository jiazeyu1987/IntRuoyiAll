# Execution Log

BDD: Public read-back gates publish success -> Given a scoped showroom publish switches current, When the public Website current endpoint does not return the same release, Then the admin publish call fails instead of reporting success.

BDD: Current release is never cached by intermediaries -> Given Website requests the scoped current release, When backend responds, Then the response has no-store cache semantics.

BDD: Deployment validates Website proxy -> Given Website is restarted, When deployment smoke checks the public Website scoped current endpoint, Then returning HTML or mismatched release blocks deployment.

START: Created IntRuoyi task record before production code changes.

RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomReleaseAdminPublishIntegrationTest,ShowroomReleaseCurrentApiTest" test` -> FAIL, expected reason: public read-back verifier class missing.

RED: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -k public_website_scoped_current_release -q` -> FAIL, expected reason: publish script did not verify public scoped current release through Website origin.

GREEN: `mvn -pl yudao-module-showroom -am -DskipTests compile` -> PASS.

GREEN: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomPublicReleaseReadbackVerifierTest,ShowroomReleaseAdminPublishIntegrationTest,ShowroomReleaseCurrentApiTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 12 tests.

GREEN: `python -m pytest script/tests/test_publish_int_ruoyi_to_test_tooling.py -q` -> PASS, 26 tests.

GREEN: `python -X utf8 C:/Users/BJB110/.codex/skills/task-closeout-cleanup/scripts/task_closeout.py --task-id 20260529-publish-readback-gate --mode preview` -> PASS, no deletes and no blockers.
