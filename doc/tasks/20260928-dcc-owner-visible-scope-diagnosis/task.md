# DCC Owner Visible Scope Diagnosis

## Goal
Use the real Playwright UI as admin to inspect the DCC document-control, category-permission, and directory-permission configuration for controlled file `2054545668044084024`. Determine which VIEW grants are absent for Owner A (production, user ID `910326`) and Owner B (quality, user ID `910327`), and record the actual configuration path without saving changes.

## BDD
- BDD: inspect owner visible-scope configuration -> Given tenant 芋道源码, frontend `http://127.0.0.1:8062`, backend `http://127.0.0.1:48062`, admin login `admin/admin123`, and the identified controlled file/category/directory; When admin navigates through visible UI to document-control, category, and directory permission pages and inspects Owner A/B effective VIEW settings; Then the report identifies each UI path, the file's category/directory as visibly shown, missing VIEW grants by owner/department, and supporting screenshots/naturally generated response status, with zero save/write actions.

## Milestones
1. `completed`: read repository E2E, login, runtime, worktree, and closeout rules; confirm endpoints and existing evidence.
2. `completed`: use real Playwright UI as admin to inspect document-control, category, VIEW matrix, and directory authorization pages read-only.
3. `blocked`: cross-check the target file's category/directory and Owner A/B grants; admin UI did not expose the target mapping, so no absent grant can be responsibly identified.
4. `pending`: structurally validate task records and set status according to evidence and closeout constraints.

## Expected Verification
- Record browser URL and visible navigation path for each permission configuration area.
- Record the target file/category/directory values visible in the UI, if available.
- Record whether Owner A and Owner B each have effective VIEW permission at document-control, category, and directory scope; distinguish explicit grant, inherited grant, absent grant, and indeterminate UI state.
- Preserve Playwright screenshots and a sanitized evidence JSON containing only naturally observed request path/method/status and visible UI facts; never record passwords, tokens, cookies, or response bodies.
- Assert zero permission-save/update requests and make no changes.
- Validate `task.md` and `execution-log.md` structure and confirm all evidence files exist.

## Design Constraints
- Read-only admin inspection; no permission save, role/department/owner changes, or DCC business actions.
- Use Playwright against the real frontend at port 8062 only. Do not call `fetch`, API clients, APIRequest, curl, or database tools. Network evidence may only come from requests naturally triggered by real UI navigation.
- Do not infer missing configuration from a denied Owner detail page alone; distinguish effective scope from UI-observed values.
- Do not touch Git, stop/restart services, or alter other task assets.
- Store evidence only under this task directory; redact all credentials and authentication material.

## Current Status
 blocked

## Cleanup Keep
- doc/tasks/20260928-dcc-owner-visible-scope-diagnosis/task.md
- doc/tasks/20260928-dcc-owner-visible-scope-diagnosis/execution-log.md
