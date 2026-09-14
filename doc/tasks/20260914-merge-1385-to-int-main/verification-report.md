# Merge 1385 Verification Report

## Result

ready_for_closeout

Required checks passed on the resolved integration tree including main 380a49b2a. Commit, push, cleanup and final fast-forward remain.

## Verified Changes

- Authorized source batch preserved in 8aebc6eed; blockers fixed in 245c4a8fa, progress recorded in 465a717e0.
- Explicit manual rollback SQL excluded from forward discovery; missing forward metadata and conflicting declarations remain errors. No database state changed.
- Main DCC check-in cleanup, companion PDF ownership, preview permissions and four-stage route regressions retained.
- MES inventory validation uses formal source identity rather than source type alone.

## Verification

- DCC focused Maven: 350 tests PASS; MES: 42 tests PASS. Eleven selected classes, -pl yudao-module-dcc,yudao-module-mes -am; completed Surefire reports checked.
- Server UploadMultipartLimitConfigTest: 4 tests PASS with standalone server module Maven invocation.
- Python publish/local restart/runtime tooling: 155 tests PASS, pytest-integrated-07. Obsolete persistent-environment test removed because it contradicted main's negative contract.
- Integrated frontend API: 10 PASS; check-in: 5 PASS; upload-purpose static contract PASS; pnpm ts:check PASS.
- Direct-download PowerShell configuration and backend startup static contracts PASS.
- Branch port guard PASS: slot 24, frontend 8158, backend 48158. git diff --check PASS.
- pnpm install --frozen-lockfile PASS. Ignored dependency lifecycle scripts did not prevent required checks. No dependency or lockfile changes committed.

## Boundaries

- No real E2E, deployment, live database writes or service restart.
- Main's three unrelated DCC/closeout documentation files remain outside task staging and merge scope; verify hashes around final fast-forward.
- Cleanup keeps three task records and removes task-local temporary products. Source detached 1385 worktree remains preserved.
- Reusable SQL-discovery lesson consolidated into docs/release-backup-restore.md.
