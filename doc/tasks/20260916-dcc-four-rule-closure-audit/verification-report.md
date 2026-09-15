# Verification Report

## Scope
Current `int_qms` source verification for four user-specified DCC controlled-file rules:

1. Name and content permissions are separated, and content permission includes name visibility.
2. Upload does not create revisions; initial upload can carry an explicit initial version, and later version changes go through checkout/checkin.
3. The ordinary controlled-file flow does not expose the retired work-draft/current-version business terminology and describes file objects as controlled files with effective-state progress.
4. Linked-file major version changes notify authorized follow-up users, who decide whether to link the new version; minor version changes do not notify or auto-switch.

## Conclusion
PASS in static/local regression scope. The current code no longer shows the four-rule defects in ordinary controlled-file code paths. This report does not claim real browser E2E, live database behavior, deployment, or server-runtime verification.

## Per-rule Result
| Rule | Result | Evidence |
|---|---|---|
| Name/content permission split | PASS | Frontend/backend permission contracts and query regressions passed; scans found no fallback from content permission into name-only browsing. |
| Upload/version governance | PASS | New upload initial-version contracts, checkout/checkin browser contracts, backend version allocation, working-iteration submission, and ordinary entrypoint scans passed. |
| Controlled-file terminology | PASS | Production static scan found no `工作稿`、`工作版本`、`现行版`、`现行版本` in the controlled-file scope; lifecycle/action labels now use `受控文件` and effective-state wording. |
| Major-version relation notification | PASS | Publication/follow-up transaction regressions and backend static contracts passed; relation-resolution audit text now says the linked major version has formally become effective. |

## Verification Evidence
- Backend static contracts: `node --test yudao-module-dcc\src\test\js\dcc-static-*.cjs` -> 19 PASS, 0 failed.
- Frontend focused contracts: `node --test` over terminology, new upload version entry, name/content permission, related-file pagination, ordinary removed actions, and browser checkin main-flow -> 15 PASS, 0 failed.
- Backend targeted regression: Maven selected DCC classes -> 221 tests, 0 failures, 0 errors, 0 skipped.
- Frontend type/build: `pnpm ts:check` -> PASS; `pnpm build:local` -> PASS.
- Hygiene/static scans: `git diff --check` -> PASS; old terminology scan had no production hits; ordinary revision/upload mutation scan found no ordinary controlled-file entrypoints.

## Code Adjustments
- Restricted standalone publish/effect handling to external-review process type in backend precheck/query action projection and frontend browser action projection.
- Updated ordinary controlled-file user-facing lifecycle labels from publish/current wording to effective/current-effective wording.
- Changed saved electronic distribution recipient validation from raw `IllegalStateException` to business `ServiceException` with concrete distribution/user IDs.
- Refreshed tests whose fixtures still modeled ordinary controlled files as standalone publish candidates.

## Boundaries
- No real-page E2E was run because the current instruction did not explicitly request E2E.
- No database write, service restart, deployment, or remote server action was performed.
- Some backend/front-end class and API names still contain `Publication`/`Publish` because they are persisted/internal or external-review compatibility identifiers; user-visible ordinary controlled-file behavior is guarded by action projection and the static contracts above.
