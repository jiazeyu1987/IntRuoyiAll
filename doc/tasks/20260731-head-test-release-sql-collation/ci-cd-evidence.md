# CI/CD Evidence

## Environment

- Target release scope: test server only.
- Current implementation workspace: `E:\IntRuoyi`, branch `int_main`, pre-fix HEAD `d1ffcef87e9a6af884cfe47bb0ad69b78febecfd`.
- Previous failed test release: `release-20260731-intmain-head-test-r260730a-r1`.

## Commands

- RED: `python -X utf8 -m pytest script/tests/test_dcc_codex_test_items_seed.py`.
- GREEN: `python -X utf8 -m pytest script/tests/test_dcc_codex_test_items_seed.py`.
- SQL policy: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql`.
- Pending release commands: new clean release worktree, `build-release`, then `publish-test` only.

## Secrets

No secrets are stored in this evidence. Raw publish operation logs containing MySQL `-p...` command shape must not be committed or copied.

## Pipeline

No CI pipeline files changed. The release workflow remains existing runtime-control `build-release -> publish-test`; this task only repairs a required SQL file and its static regression test.

## Verification

- SQL regression test: PASS, 5 tests.
- Full release migration policy gate: PASS, 400 migrations.
- Runtime release rebuild and test-server verification remain pending.

## Rollback

If the SQL repair causes an unexpected migration issue, revert the implementation commit before creating the release worktree and rebuild with a new releaseTag. Do not reuse the failed releaseTag.

## Blockers

Testing-server release is not yet complete; final success still requires a new committed HEAD, clean release worktree, manifest/sourceRepo dirty=false checks, `publish-test` success, and runtime verification.
