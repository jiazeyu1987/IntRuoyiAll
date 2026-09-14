# CI/CD Environment Evidence

## Environment

- Task: `20260727-release-change-codex-summary`.
- Environment targets: local release-script generation path only.
- Server deploy targets: none; no test, production, backup, SSH, database, or shared-storage publish action was executed.

## Pipeline

- `IntRuoyiBackend/script/deploy/publish-int-ruoyi.ps1`: release-info change summary generation now invokes Codex CLI, requires structured JSON, validates 1 to 10 plain-Chinese summary items, rejects raw commit identifiers, and fails fast on missing/failed/timed-out Codex execution.
- `IntRuoyiBackend/script/tests/test_publish_int_ruoyi_to_test_tooling.py`: release-script contract tests cover Codex invocation, fail-fast behavior, and summary validation.

## Secrets

- The release environment must provide an authenticated `codex` executable or `INTRUOYI_RELEASE_CHANGE_SUMMARY_CODEX_CLI_COMMAND` pointing to one.
- No secret values are stored in the repository.

## Commands

- Build/test local verification: focused Python release-script tests, PowerShell parse, frontend static contract, and `pnpm ts:check`.
- Package/deploy: not executed in this task.
## Rollback

- Revert the implementation commit if release-info Codex summary generation needs to be removed.

## Artifact And Release Output

- Expected release artifact: `dist-intruoyi-test/release-info.json` generated before Docker context preparation.
- Expected JSON field: `changeSet.gitChanges` contains user-readable Codex summaries and `changeSet.summaryGenerator = "codex"` when there are Git differences.

## RED Evidence

- Backend release-script tests first failed because Codex summary functions and fail-fast validation were absent.
- Frontend static contracts first failed because the visible label still said raw Git changes.

## Verification

- Focused Codex summary tests passed.
- PowerShell script parsing passed.
- Frontend static contract and type check passed.
- Full publish tooling regression still has one unrelated SQL metadata blocker recorded in the task verification report.

## Blockers

- Manual server publish approval: not requested and not granted.
- Codex runtime smoke in the local desktop environment was blocked by Codex authentication/plugin-sync failure first, then by timeout when user config was isolated; release code keeps this as fail-fast behavior rather than fallback.
