# Merge 1385 Dirty Worktree Execution Log

## Final Closeout

- Implementation blocker fix: 245c4a8fa; verification records: 465a717e0; integration merge: ad2e3a1d4.
- GREEN: cleanup preview/apply --worktree-closeout off -> PASS; retained three task records, removed only task-local temporary test files. Main's unrelated dirty files excluded from staging.
- GREEN: git merge --ff-only codex/20260914-merge-1385-to-int-main (E:/IntRuoyi) -> PASS, 380a49b2a to ad2e3a1d4.
- GREEN: git push origin codex/20260914-merge-1385-to-int-main; git push origin int_main -> PASS.
- GREEN: three unrelated main file hashes before/after merge -> PASS, unchanged.
- GREEN: main branch runtime port guard -> PASS, 8081/48081.
- GREEN: git worktree remove D:/IntRuoyiWorktree/20260914-merge-1385-to-int-main -> exit 0, Git registration removed. Source C:/Users/BJB110/.codex/worktrees/1385/IntRuoyi preserved.
- BLOCKED: physical directory readback found only IntRuoyiFronted/node_modules residue. Scoped PowerShell Remove-Item was rejected by automatic approval as "blocked by policy" before execution. No alternate deletion was attempted. Slot 24 remains active; task status stays ready_for_closeout rather than completed.

## Rule Read Evidence

- Read `AGENTS.md` instructions supplied in the user message.
- Read `docs/task-closeout-rules.md`.
- Read `docs/worktree-restrictions.md`.
- Read `docs/branch-runtime-ports.md`.
- Read `docs/backend-development.md`.
- Read `docs/worktree-memory.md` relevant merge/detached-head gates.
- Read `task-closeout-cleanup` skill and closeout rules.
- Read `project-experience-consolidation` skill.

## Authorization Evidence

- User requested: `提交并融合进int_main`.
- After blocker disclosure, user replied: `确认`.
- Scope now includes all currently dirty tracked changes and untracked files in `C:\Users\BJB110\.codex\worktrees\1385\IntRuoyi`.

## BDD

BDD: Authorized dirty batch is preserved -> Given the source worktree is detached and contains mixed dirty/untracked changes, When the user confirms full-batch fusion, Then the task creates a named integration branch and commits the exact authorized batch without discarding files.

BDD: Fusion reaches int_main only after verification -> Given the integration branch contains the authorized batch, When it is rebased or merged onto current `int_main`, Then required static/focused verification is recorded before final `int_main` completion.

## TDD / Verification Evidence

- GREEN: source-dirty-snapshot -> PASS, source HEAD b4303b4ed0c7f5344057694268b96fecdc5c2626, tracked patch SHA256 7FF07F342BFBDC8283D67B2C185A710234C0053A2B15EABE8796218B089E7A06, two non-ignored untracked files copied.
- GREEN: integration-worktree-created -> PASS, branch `codex/20260914-merge-1385-to-int-main`, path `D:\IntRuoyiWorktree\20260914-merge-1385-to-int-main`, profile `int_main`, slot 24, frontend 8158, backend 48158.
- GREEN: branch-runtime-port-guard -> PASS before batch commit.
- GREEN: dirty-patch-replay -> PASS, target tracked patch hash equals source tracked patch hash.
- GREEN: git-diff-check -> PASS before batch commit.
- GREEN: conflict-marker-scan -> PASS, no merge conflict markers under IntRuoyiBackend, IntRuoyiFronted, docs, or scripts.

## 2026-09-14 Continuation

- User authorized resolving blockers and merging. Resume status: in_progress.
- GREEN: integrated Maven eleven-class DCC/MES selection -> PASS, DCC 350 and MES 42 tests; completed Surefire reports checked.
- GREEN: integrated Python suite with pytest-integrated-07 -> PASS, 155 tests.
- GREEN: integrated pnpm ts:check, frontend API (10), check-in (5), upload-purpose static, backend startup static and PowerShell runtime configuration -> PASS.
- Cleanup plan: preserve unrelated main documentation via no-overlap check and hashes; artifact cleanup uses --worktree-closeout off, followed by explicit ff-only merge and task-only worktree removal. No unrelated baseline commit.
- Merge resolution: retain current main DCC check-in cleanup/companion ownership, preview scope, four-stage test fixes and route regression coverage; retain this batch's MES inventory identity validation and manual rollback discovery fix.
- RED: post-merge Python regression -> FAIL, retained historical persistent-environment test contradicted main's explicit removal of that unused helper. Removed the obsolete test; main's negative contract remains.
- GREEN: pnpm install --frozen-lockfile -> PASS. Dependency build scripts were ignored by pnpm policy; required check-in and type checks completed successfully without their execution.
- GREEN: node src/views/dcc/controlled-file/browser/checkin-main-flow.spec.cjs -> PASS, 5 tests.
- GREEN: pnpm ts:check -> PASS (tsconfig.relaxed.json, repository-defined type-check command).
- GREEN: focused Maven DCC/MES selection (same ten named test classes, -pl yudao-module-dcc,yudao-module-mes -am) -> PASS, DCC 347 and MES 32 tests, BUILD SUCCESS.
- GREEN: mvn.cmd -q -f IntRuoyiBackend/pom.xml -pl yudao-server -Dtest=UploadMultipartLimitConfigTest -Dsurefire.failIfNoSpecifiedTests=false test -> PASS, 4 tests.
- GREEN: Python three-file tooling regression with --basetemp=doc/tasks/20260914-merge-1385-to-int-main/pytest-green-04 -> PASS, 156 tests.
- GREEN: release_sql_discovery_excludes focused test with pytest-green-05 -> PASS, includes conflicting metadata rejection.
- Experience consolidation: added the general forward/manual rollback discovery verification rule to existing docs/release-backup-restore.md; no new experience document.
- RED: python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_publish_int_ruoyi_to_test_tooling.py -k release_sql_discovery_excludes -q --basetemp=doc/tasks/20260914-merge-1385-to-int-main/pytest-red-02 --tb=short -> FAIL, explicitly declared manual rollback was parsed as a forward migration.
- BDD: Forward migration discovery excludes manual rollback -> Given a release SQL root with a forward migration and an explicitly declared manual rollback, When release scripts are collected, Then only the forward migration is packaged and forward files without metadata still fail.

- Main baseline commits recorded: `36548b47c`, `90adf7d6e`; fusion rebased commit `8aebc6eed`.
- BDD: Fixed approval route preview -> Given a valid four-stage route, When preview resolves users, Then all four nodes remain visible and the first node uses the correct user resolver.
- BDD: Disabled selected signoff user -> Given a valid four-stage route and a disabled selected user, When submission validates signoff users, Then it rejects before record, snapshot, and BPM writes.
- RED: focused Maven DCC/MES/server test selection -> FAIL, missing Collection import first; after import correction, 347 DCC tests ran with four workflow failures caused by obsolete route fixtures/assertions.
- Corrected the four workflow tests to use the existing four-stage route contract; no production route behavior changed.
- RED: node scripts/tests/start-branch-backend-dcc-encryption-static.spec.cjs -> FAIL, old negative local-profile assertion contradicted the runtime script.
- GREEN: node scripts/tests/start-branch-backend-dcc-encryption-static.spec.cjs -> PASS after assertion correction (repo root).
- GREEN: node scripts/dcc-frontend-api-fail-closed-contract.test.mjs -> PASS, 10 tests (frontend root; prior repo-root invocation failed on relative source paths).
- GREEN: node tests/e2e/dcc-controlled-file-protection.contract.test.js -> PASS (frontend root; static contract, not real E2E).
- GREEN: pwsh -NoProfile -File IntRuoyiBackend/script/tests/test_dcc_download_encryption_runtime_config.ps1 -> PASS.
- RED: node src/views/dcc/controlled-file/browser/checkin-main-flow.spec.cjs -> FAIL, Cannot find module typescript in integration frontend.
- RED: python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_publish_int_ruoyi_to_test_tooling.py IntRuoyiBackend/script/tests/test_restart_int_ruoyi_local_schema.py IntRuoyiBackend/script/tests/test_runtime_control_scripts.py -q -> FAIL, 152 passed, 1 failed Docker CMD assertion, 2 setup errors due default temporary directory access denial.
- Corrected the Docker CMD assertion to include the existing INTRUOYI_EXTRA_ARGS argument.
- RED: python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_publish_int_ruoyi_to_test_tooling.py IntRuoyiBackend/script/tests/test_restart_int_ruoyi_local_schema.py IntRuoyiBackend/script/tests/test_runtime_control_scripts.py -q --basetemp=doc/tasks/20260914-merge-1385-to-int-main/pytest-temp-01 --tb=short -> FAIL, 154 passed, 1 failed: manual rollback SQL is collected as a forward release migration and lacks release metadata. The expected missing BackendRuntimeBaseMode check is not reached.
- BLOCKED: pnpm install --frozen-lockfile and focused Maven rerun were interrupted after required verification and main integration blockers were confirmed; neither is recorded as PASS. No newly completed Surefire reports were observed.
- Main concurrently advanced to 7393f6731 and acquired unrelated dirty/untracked task and documentation files. No staging or modification of those files was performed in this continuation.
- Applied project-experience-consolidation review: docs/worktree-memory.md already covers worktree dependency prerequisites and main drift/dirty merge gates. Reuse those rules; no duplicate long-term document was created.
- Task remains blocked; no additional commit/push/merge, cleanup preview/apply, worktree removal, or slot release. Task-owned pytest output is listed for eventual closeout.
