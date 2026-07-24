# 20260529-showroom-refactor-review-gate

## Task Goal

Run a reviewer-gated, subagent-driven documentation pass for the showroom release truth refactor. The output must be reviewable written artifacts that can drive the real refactor without relying on patch-style fixes.

## Reviewer Gate

The reviewer may approve only if all of the following are true:

1. The documents describe a complete solution that can achieve both user goals without hidden side effects:
   - publish always uses the latest effective data
   - Website always loads the latest successfully published IntRuoyi data
2. The documents are written in a BDD + strict TDD + subagent-driven form.
3. The logic is internally consistent and the interfaces are explicit and clear.

## Deliverables

- `backend-release-architecture.md`
- `website-runtime-architecture.md`
- `bdd-tdd-subagent-delivery-plan.md`
- `canonical-contract.md`
- reviewer verdict in `review-report.md`

## Milestones

- [x] Create task record before starting subagents.
- [x] Dispatch subagents with disjoint document ownership.
- [x] Review all generated documents against the gate.
- [x] Record pass/fail verdict and required fixes.

## Current Status

Reviewed and approved with canonical override. The implementation baseline is the reviewer-owned canonical contract.
