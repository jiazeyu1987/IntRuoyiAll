# Request Analysis

## User Goal

Continue code delivery in the new eDHR worktree using subagent-driven implementation and main-agent reviewer gates. Every implemented feature point must have an E2E test before release.

## Current System

The worktree already contains eDHR execution, field audit, approval, archive, domain trace, and WORM guard progress. The remaining documented production gap in `05-frontend-api-e2e-contract.md` is runtime role/tenant matrix evidence: current proof is mostly admin or static SQL contract evidence, not a role-separated real UI matrix.

Two read-only explorers confirmed:

- Static SQL and Controller permission coverage exists for eDHR permissions.
- Runtime evidence is still missing for executor, approver, QA/archive, readonly, no-permission, and formal admin readonly boundaries.

## Constraints

- Mutating E2E can only write test tenant data.
- Formal `芋道源码/admin` can only be used for readonly verification.
- Missing users, passwords, menu grants, Playwright, frontend, backend, or execution IDs must fail fast.
- Do not add test-only UI controls or mock API responses.
- The main agent owns review and final release decision.

## Unknowns

- Whether the local test tenant already contains separated eDHR role accounts.
- Whether existing approved/sealed execution records are sufficient for readonly and archive checks.
- Whether no-permission users can log in without role bindings.

## Risks

- A script that only checks static menu SQL could be mistaken for runtime role-matrix proof.
- A readonly path could accidentally send a write request.
- A no-permission path could pass because a page is empty rather than explicitly unauthorized.
- Fixture setup could mutate the wrong tenant if not hard-guarded.

## Validation Surface

- Playwright real frontend at `http://localhost:8081`.
- Backend at `http://127.0.0.1:48081`.
- MySQL container `int-ruoyi-mysql`, database `ruoyi-vue-pro`.
- Package scripts and Node static tests.
- E2E evidence under `test-results/edhr-permission-tenant-matrix/`.

## Blocking Prerequisites

- Playwright runtime must resolve.
- Local frontend and backend must be running.
- Fixture script must confirm tenant `122` before any writes.
- Formal admin coverage requires explicit admin base URL/tenant/user/password and readonly write guard.
