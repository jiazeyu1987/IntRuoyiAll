# Release Preflight Stable Ordering Regression

## Bug Summary

The test release preflight plan can execute a later workstation-binding migration before an earlier test-only cleanup migration, even though both dependencies are satisfied and Manifest v1 lists cleanup first.

## Expected Behavior

Dependency ordering must keep every prerequisite before its child while otherwise preserving Manifest v1 order. The balloon cleanup migration must therefore execute before the workstation-binding migration.

## Reproduction

- ReleaseTag: `release-20260727-onlyoffice-test-r260727-1823`
- Target: test server `172.30.30.58`
- Result: `20260717_mes_balloon_excel_device_workstation_binding.sql` ran before `20260716_mes_balloon_xlsx_route_00002_invalid_process_cleanup.sql` and failed with `balloon Excel target route process count mismatch`.

## Root Cause

`release_preflight_plan.py` uses a FIFO queue for zero-indegree migrations. A child that becomes ready earlier remains ahead of a lower-index child that becomes ready later, so the result is topologically valid but not stable relative to Manifest v1 order.

## Regression Test

`script/tests/test_release_preflight_plan.py::test_preflight_preserves_manifest_order_when_dependencies_become_ready`

## TDD Evidence

- RED: `python -X utf8 -m pytest script\tests\test_release_preflight_plan.py::test_preflight_preserves_manifest_order_when_dependencies_become_ready -q` failed because `binding` was emitted before the earlier `cleanup` migration.
- GREEN: `python -X utf8 -m pytest script\tests\test_release_preflight_plan.py script\tests\test_publish_int_ruoyi_to_test_tooling.py script\tests\test_mes_balloon_xlsx_route_00002_invalid_process_cleanup_sql.py script\tests\test_mes_balloon_excel_device_workstation_binding_sql.py -q` passed with `117 passed`.

## Verification

- The full migration policy gate passed with `migrationCount=383`.
- The failed test release was restored to the previous runtime tag, and backend/frontend/OnlyOffice health remained green.
- Regenerating the failed package plan with the fixed planner produced cleanup before binding, with both actions set to `APPLY`.
- The final r6 code-only release completed successfully after data migrations and their data-dependent descendants were skipped for the test-server code-only deployment.

## Risk And Scope

The fix is limited to deterministic migration ordering. It must not weaken dependency blocking, environment filtering, checksum checks, or release fail-fast behavior.

## Blockers

The failed releaseTag is invalid and will not be reused. No open blocker remains for the final r6 test deployment.

# OnlyOffice Public File URL Health Check Regression

## Bug Summary

The r4 deploy switched the test-server containers and passed external HTTP readiness, but the final OnlyOffice public-file-base URL check failed because the publish script wrapped `curl` in an intermediate `sh -lc` command. The URL argument was lost during PowerShell/SSH/remote shell quoting, so curl printed help instead of requesting `http://backend:48081/actuator/health`.

## Expected Behavior

Deploy validation must pass the health URL as one quoted argument to `curl` inside `intruoyi-onlyoffice`. If the backend health endpoint is reachable from the container, the check must pass; if not, the script must fail fast with the real curl error.

## Reproduction

- ReleaseTag: `release-20260727-onlyoffice-test-r260727-codeonly-r4`.
- Target: test server `172.30.30.58`.
- Result: deploy failed with `ONLYOFFICE_PUBLIC_FILE_BASE_URL_UNREACHABLE`.
- Control probe: `wget http://backend:48081/actuator/health` from `intruoyi-onlyoffice` returned HTTP 200, proving the runtime network path was healthy.

## Root Cause

The script built `docker exec intruoyi-onlyoffice sh -lc "curl ... '<url>' >/dev/null && echo OK"`. The nested shell boundary made URL quoting dependent on multiple command parsers, and the remote command reached curl without the URL argument.

## Regression Test

`script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_deploy_checks_onlyoffice_container_can_reach_public_file_base_url`

## TDD Evidence

- RED: r4 deploy final validation failed with `ONLYOFFICE_PUBLIC_FILE_BASE_URL_UNREACHABLE`, while a direct container probe reached backend health HTTP 200.
- GREEN: targeted pytest passed with `1 passed` after changing the command contract to direct `docker exec intruoyi-onlyoffice curl -fsS --connect-timeout 5 <healthUrl>`.
- GREEN: expanded publish regression suite passed with `125 passed`.

## Verification

- PowerShell parser validation passed.
- `git diff --check` passed.
- Branch runtime port guard passed.
- r4 operation lock was closed as `FAILED`; final r6 deployment validated OnlyOffice health with direct container curl and completed successfully.

## Risk And Scope

The change only affects the release validation command. It does not change the runtime URL, OnlyOffice configuration, database migration execution, or code-only data policy.

## Blockers

r4 is invalid and must not be reused. No open blocker remains for the final r6 test deployment.

# Empty Code-only APPLY Queue Regression

## Bug Summary

The r5 deploy reached the required SQL phase after code-only filtering skipped all remaining pending data and data-dependent migrations. Because the script passed `Get-ReleasePreflightApplyItems` directly inside a parameter expression, PowerShell bound the empty output as `$null` to `Sort-RequiredDatabaseSqlApplyItems -Items`, causing deployment to fail before container restart.

## Expected Behavior

When code-only filtering produces no APPLY items, deploy-release must treat that as an empty array and continue to runtime deployment. Empty APPLY is valid for a code-only release after all eligible non-data migrations are already applied or skipped.

## Reproduction

- ReleaseTag: `release-20260727-onlyoffice-test-r260727-codeonly-r5`.
- Target: test server `172.30.30.58`.
- Result: deploy failed with `Cannot bind argument to parameter 'Items' because it is null`.
- Remote state: operation lock was `RUNNING`, migration rows were `SKIPPED_ALREADY_APPLIED`, containers had not restarted, and `.env IMAGE_TAG` was restored to the actual running r4 tag during failure closeout.

## Root Cause

PowerShell command substitution returned no objects after code-only filtering. Passing that expression directly to a mandatory array parameter produced `$null` instead of `@()`, despite the target parameter allowing empty collections.

## Regression Test

`script/tests/test_publish_int_ruoyi_to_test_tooling.py::test_deploy_release_handles_empty_code_only_apply_queue_before_sorting`

## TDD Evidence

- RED: r5 deploy required SQL phase failed with a null `Items` parameter after all data/data-dependent APPLY rows were filtered out.
- GREEN: targeted pytest passed with `3 passed` after assigning `$preflightApplyItems = @(Get-ReleasePreflightApplyItems ...)` before sorting.
- GREEN: expanded publish regression suite passed with `126 passed`.

## Verification

- PowerShell parser validation passed.
- `git diff --check` passed.
- Branch runtime port guard passed.
- r5 operation lock was closed as `FAILED`; final r6 deployment treated the empty APPLY queue as `@()` and continued to runtime deployment successfully.

## Risk And Scope

The change only normalizes an empty apply queue to an empty array. It does not change migration eligibility, dependency closure, runtime configuration, or data sync behavior.

## Blockers

r5 is invalid and must not be reused. No open blocker remains for the final r6 test deployment.
