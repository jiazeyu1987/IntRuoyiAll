# Development Plan

## Task Graph

### task_id: T1

title: Static contract and package script wiring
objective: Add a Node contract test and package scripts for the permission matrix gate.
dependency_ids: []
affected_paths: `package.json`, `scripts/edhr-permission-tenant-matrix-contract.test.mjs`
write_scope: Frontend package scripts and contract test only.
acceptance_ids: AC-01, AC-02, AC-03, AC-07
validation_steps: `node --test scripts\edhr-permission-tenant-matrix-contract.test.mjs`
done_definition: Contract test fails before implementation and passes after scripts/E2E/fixture guards exist.

### task_id: T2

title: Test-tenant role fixture script
objective: Add a dry-run-first fixture script that creates or verifies test-tenant role matrix users and menu bindings.
dependency_ids: []
affected_paths: `doc/tasks/20260528-edhr-role-tenant-e2e-gate/scripts/prepare-edhr-role-matrix-fixtures.cjs`
write_scope: Task-local fixture script only.
acceptance_ids: AC-05, AC-06
validation_steps: Dry run then explicit `--apply` in tenant `122`.
done_definition: Dry run writes nothing; apply creates or verifies separated users/roles only in tenant `122`.

### task_id: T3

title: Runtime Playwright permission matrix E2E
objective: Add the real UI E2E gate with write guards, no-permission negative checks, and role-separated readonly/positive smoke paths.
dependency_ids: [T1, T2]
affected_paths: `tests/e2e/edhr-permission-tenant-matrix.e2e.js`
write_scope: E2E script only.
acceptance_ids: AC-01, AC-02, AC-03, AC-04, AC-05
validation_steps: `pnpm e2e:edhr:permission-matrix:check`, then `pnpm e2e:edhr:permission-matrix`
done_definition: E2E passes with real accounts or records exact `BLOCKED` prerequisite.

### task_id: T4

title: Independent reviewer evidence
objective: Main reviewer verifies worker changes, E2E evidence, and no side effects before release.
dependency_ids: [T1, T2, T3]
affected_paths: `doc/tasks/20260528-edhr-role-tenant-e2e-gate/*`
write_scope: Evidence docs only.
acceptance_ids: AC-01, AC-02, AC-03, AC-04, AC-05, AC-06, AC-07
validation_steps: Review git diff, rerun tests, inspect result JSON.
done_definition: Reviewer gate records PASS or exact BLOCKED status.
