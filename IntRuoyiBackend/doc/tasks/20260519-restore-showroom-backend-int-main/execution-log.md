# Execution Log: Restore Showroom Backend Into int_main

## BDD Scenarios

BDD: Mainline Showroom admin API available -> Given frontend `int_main` already requests `admin-api/showroom/company/current`, When backend `int_main` is updated with the completed Showroom branch, Then `/admin-api/showroom/company/current` resolves through the Showroom controller instead of static-resource 404 handling.

BDD: Existing local backend work preserved -> Given backend `int_main` contains unrelated local modifications, When the Showroom backend branch is merged, Then those unrelated files are not reverted or silently committed by this task.

## TDD Evidence

- RED: backend `int_main` route verification -> FAIL, `ShowroomAdminController`, `ShowroomDisplayController`, and `yudao-module-showroom` were missing; the frontend symptom was `No static resource admin-api/showroom/company/current`.
- GREEN: `mvn -pl yudao-module-showroom test` -> PASS, 28 tests passed.
- GREEN: `mvn -pl yudao-server -am "-Dmaven.test.skip=true" package` -> PASS.
- GREEN: `curl.exe -i http://127.0.0.1:48081/admin-api/showroom/company/current` -> PASS for route resolution, response body is `{"code":401,"msg":"账号未登录","data":null}` instead of static-resource 404.
- GREEN: authenticated `GET /admin-api/showroom/company/current` with tenant `1` and admin access token -> PASS, returns a `DRAFT` scaffold payload with empty fields instead of `SHOWROOM_TARGET_NOT_FOUND: live company revision not found`.
- REGRESSION: `rg -n "ShowroomAdminController|ShowroomDisplayController|/showroom/company/current|/showroom/display/home|yudao-module-showroom" . -g "*.java" -g "pom.xml"` -> PASS after merge.
- CLOSEOUT PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260519-restore-showroom-backend-int-main --mode preview` -> PASS, no delete candidates and no blockers.

## Verification Evidence

- Merge was applied from `codex/showroom-t6-integration-hardening` with a review pass that preserved unrelated AI and DCC local modifications.
- Missing build-contract files were manually restored after the merge preview: root/module poms, Showroom foundation contracts, SQL baseline, and foundation contract tests.
- Backend process on port `48081` was restarted from `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-server\target\yudao-server.jar`.

## Blockers

- Showroom admin is now unblocked with an empty draft scaffold, but public display routes still require published company/narration data to show real exhibit content.
