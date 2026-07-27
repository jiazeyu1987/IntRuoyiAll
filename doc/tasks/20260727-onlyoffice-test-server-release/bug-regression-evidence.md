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
- The next release must use a new releaseTag and confirm the generated preflight plan lists cleanup before binding.

## Risk And Scope

The fix is limited to deterministic migration ordering. It must not weaken dependency blocking, environment filtering, checksum checks, or release fail-fast behavior.

## Blockers

The failed releaseTag is invalid and will not be reused. A new releaseTag is required for the next build and test deployment.
