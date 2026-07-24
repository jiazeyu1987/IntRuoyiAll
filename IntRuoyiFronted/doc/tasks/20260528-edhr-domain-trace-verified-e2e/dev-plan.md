# Development Plan

## Task Graph

### T1

task_id: T1
title: Add DomainTrace E2E expectation contract
objective: Add a focused Node contract test proving the E2E script supports expected status and blocker count, records the values in evidence, and fails on mismatches.
dependency_ids: []
affected_paths: scripts/edhr-domain-trace-e2e-contract.test.mjs
write_scope: scripts/edhr-domain-trace-e2e-contract.test.mjs
acceptance_ids: AC-01, AC-03, AC-05
validation_steps: `node --test scripts/edhr-domain-trace-e2e-contract.test.mjs`
done_definition: Contract test is RED before implementation and GREEN after script update.

### T2

task_id: T2
title: Implement expected status and blocker-count checks
objective: Update the real DomainTrace E2E script to parse optional expectations and assert final evidence.
dependency_ids: [T1]
affected_paths: tests/e2e/edhr-domain-trace-real-flow.e2e.js
write_scope: tests/e2e/edhr-domain-trace-real-flow.e2e.js
acceptance_ids: AC-02, AC-04, AC-05
validation_steps: `pnpm e2e:edhr:domain-trace:check`; real `pnpm e2e:edhr:domain-trace`
done_definition: Syntax check passes and real VERIFIED E2E produces PASS evidence.

### T3

task_id: T3
title: Record execution and verification evidence
objective: Update task evidence after RED/GREEN and real E2E.
dependency_ids: [T2]
affected_paths: doc/tasks/20260528-edhr-domain-trace-verified-e2e/execution-log.md, doc/tasks/20260528-edhr-domain-trace-verified-e2e/test-report.md
write_scope: doc/tasks/20260528-edhr-domain-trace-verified-e2e
acceptance_ids: AC-01, AC-02, AC-03, AC-04, AC-05
validation_steps: Review task docs and independent tester report.
done_definition: Execution log and test report name commands, outputs, ids, and residual risks.

## Conflict Analysis

T1 and T2 both inspect the same E2E script but have sequential dependency to avoid conflicting edits. T3 is documentation-only after verification.
