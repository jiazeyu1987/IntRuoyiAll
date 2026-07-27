# Execution Log

## User Intent

- 用户要求“发布变更”不要显示截图中的发布包元信息、摘要、变更项和源码提交，只显示这个版本与上个版本相比 Git 里更改了哪些内容，每次最多 10 条。

## Baseline

- `git status --short --branch` 初始显示 `int_main...origin/int_main [ahead 2]` 且存在 18 个既有脏文件。
- 敏感关键词预检只命中文档说明文字和授权说明，未发现明文凭据。
- 既有脏工作区基线提交：`5b9abcd8 chore: capture pre-existing dirty worktree baseline`。
- 基线后状态：`int_main...origin/int_main [ahead 3]`，工作区干净。

## BDD Scenarios

- `BDD: release change dialog shows only git diff summary -> Given a release info payload contains package metadata, summary, changeItems, sourceCommits, and git diff changes, When the user opens the version change dialog, Then the dialog hides old metadata/source sections and only renders up to 10 git diff change entries.`
- `BDD: release change dialog empty git diff state -> Given a release info payload has no git diff changes, When the user opens the version change dialog, Then the dialog shows an explicit empty Git change message without falling back to package metadata.`

## Evidence

- `GREEN: experience-preflight -> PASS, read docs/experience-index.md; applicable gates are release-info/source commit layered verification, release-info CRLF-safe parsing, and frontend static contract isolation.`
- `RED: node tests/e2e/release-info-dock-version-only-static.spec.js -> FAIL, expected reason: ReleaseInfoDock still rendered old metadata/source sections and did not expose gitChangeItems.slice(0, 10).`
- `RED: node --test scripts/release-info-dock-contract.test.mjs -> FAIL, expected reason: ReleaseInfoDock did not expose gitChangeItems/gitChanges-only dialog contract.`
- `RED: python -m pytest IntRuoyiBackend\script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "release_change_set_is_git_diff_against_previous_release_and_capped or release_info_json_is_written_before_frontend_docker_context" -> FAIL, expected reason: publish script did not build git diff changeSet or write release-info.json before frontend Docker context.`
- `GREEN: node tests/e2e/release-info-dock-version-only-static.spec.js -> PASS.`
- `GREEN: node --test scripts/release-info-dock-contract.test.mjs -> PASS, 2 passed.`
- `GREEN: python -m pytest IntRuoyiBackend\script\tests\test_publish_int_ruoyi_to_test_tooling.py -k "release_change_set_is_git_diff_against_previous_release_and_capped or release_info_json_is_written_before_frontend_docker_context" -> PASS, 2 passed / 94 deselected.`
- `GREEN: powershell scriptblock parse publish-int-ruoyi.ps1 -> PASS.`
- `REGRESSION: pnpm ts:check -> PASS.`
- `REGRESSION: git diff --check -> PASS with CRLF normalization warnings only.`
- `REGRESSION: python -m pytest IntRuoyiBackend\script\tests\test_publish_int_ruoyi_to_test_tooling.py -> FAIL, unrelated blocker: test_build_release_backend_e2e_fails_fast_without_internal_backend_runtime_base_config now stops earlier on existing SQL metadata error "Invalid type in release migration metadata: E:\IntRuoyi\IntRuoyiBackend\sql\mysql\20260725_mes_edhr_recordbook_global_setting.sql"; 95 passed, 1 failed.`

## Implementation Notes

- `ReleaseInfoDock.vue` now renders only `changeSet.gitChanges.slice(0, 10)` in the dialog and shows `Git 变更未生成` when no Git diff items are present.
- `publish-int-ruoyi.ps1` now derives `changeSet.gitChanges/items/changes` from `git log previousCommit..currentCommit`, caps the merged list at 10, records previous release metadata, and writes `/release-info.json` into frontend dist before preparing the Docker build context.

## Blockers

- Full backend publish script regression has one pre-existing SQL metadata blocker outside this task scope; target tests and frontend type check passed.

## Experience Consolidation

- Updated existing `docs/release-build-preflight-lessons.md` with `2026-07-27 release-info 用户可见 Git 变更门禁`.
- Updated `docs/experience-index.md` with keywords for `gitChanges`, `previousCommit..currentCommit`, and version-change max 10 display.
- `rg -n "release-info 用户可见 Git 变更门禁|gitChanges|上一版本 Git 差异" docs\experience-index.md docs\release-build-preflight-lessons.md -> PASS`.

## Cleanup

- `task_closeout.py --task-id 20260727-release-change-git-diff-summary --mode preview -> PASS`; keep list contains task.md, execution-log.md, verification-report.md, frontend-feature-evidence.md; delete/blocked/warnings none.
- `task_closeout.py --task-id 20260727-release-change-git-diff-summary --mode apply -> PASS`; linked worktree false, deleted paths none.

## Git Commits

- Dirty-worktree baseline: `5b9abcd8 chore: capture pre-existing dirty worktree baseline`.
- Implementation: `6ef4f9c8 feat: show release git changes only`.
- Implementation files:
  - `IntRuoyiBackend/script/deploy/publish-int-ruoyi.ps1`
  - `IntRuoyiBackend/script/tests/test_publish_int_ruoyi_to_test_tooling.py`
  - `IntRuoyiFronted/scripts/release-info-dock-contract.test.mjs`
  - `IntRuoyiFronted/src/components/ReleaseInfoDock/ReleaseInfoDock.vue`
  - `IntRuoyiFronted/tests/e2e/release-info-dock-version-only-static.spec.js`
  - `docs/experience-index.md`
  - `docs/release-build-preflight-lessons.md`
