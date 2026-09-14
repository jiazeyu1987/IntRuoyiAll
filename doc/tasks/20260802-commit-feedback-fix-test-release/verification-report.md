# 提交第三方报工修复并重新发布测试服验证报告

## Status

TEST RELEASE PASS; BUSINESS IMPORT DATA BLOCKER

## Evidence

- Feedback fix baseline source: `d6d0e7b9ea45e37001e95c5f761af9df815890f1` contained `DirectWorkstationResolution` / `resolveDirectFeedbackWorkstation` and was pushed to `origin/int_main`.
- Invalid build evidence: `r260802c-r1` missing package metadata, `r260802c-r2` missing frontend Vite dependency, and `r260802c-r3` missing manifest v1 due to sourceRepo dictionary property handling.
- Release script fix verification: targeted RED reproduced `Get-ReleaseSourceRepoIdentity` failure for `[ordered]` sourceRepo entries; GREEN passed after `Get-ReleaseObjectPropertyText` gained `System.Collections.IDictionary` key support.
- Invalid build evidence: `r260802d-r1` built backend/frontend but failed before manifest v1 when `Invoke-ReleaseCodexExec` tried to launch `codex.ps1` directly; source, manifest and change notes therefore cannot be confirmed.
- Release script fix verification: targeted GREEN passed for Codex command resolver, sourceRepo identity, and release change-set tests after the resolver began preferring native `.cmd`/`.exe` commands over PowerShell shims.
- Invalid build evidence: `r260802e-r1` has local `manifest.json` but failed because direct script invocation omitted required NAS JSON; operation not successful, so it remains invalid.
- Invalid build evidence: `r260802f-r1` failed before manifest v1 because empty Git change facts were rejected by PowerShell parameter binding.
- Release script fix verification: targeted GREEN passed for empty Git change facts after `Invoke-ReleaseCodexSummary` and prompt generation allowed empty collections.
- Release script final source: `f0c34dfed910f52f9c03b401e976cbd2d0424e00` was pushed to `origin/int_main`; it includes the release-info change note fix and is the HEAD used by the final release worktree.
- Final release worktree: `D:\ProjectPackage\Int\IntRuoyiWorktrees\r260802-feedback-fix-test-v5\app`, detached at `f0c34dfed910f52f9c03b401e976cbd2d0424e00`, clean before build.
- Final releaseTag: `release-20260802-feedback-fix-test-r260802h-r1`.
- Final package: `E:\Int\CacheData\IntRuoyi\publish-int-ruoyi\release-20260802-feedback-fix-test-r260802h-r1`, with `manifest.json`, `release-manifest.json`, and `intruoyi-images_release-20260802-feedback-fix-test-r260802h-r1.tar`.
- Manifest source verification: backend and frontend sourceRepos both point to `f0c34dfed910f52f9c03b401e976cbd2d0424e00`, `dirty=false`, publishScope=`code-only`.
- Frontend release-info verification: package and remote `/release-info.json` both show releaseTag `release-20260802-feedback-fix-test-r260802h-r1`, source commit `f0c34dfed910f52f9c03b401e976cbd2d0424e00`, and `changeSet.gitChanges=["修正了前端版本更新说明的生成问题，让发布时的变更说明能够正常整理出来。"]`.
- Test server runtime verification: `.env IMAGE_TAG` equals `release-20260802-feedback-fix-test-r260802h-r1`; backend/frontend actual image tags equal the releaseTag; both containers are running; backend health is `UP`; frontend HTTP is `200`.
- Release database verification: `infra_release_operation_lock` shows target_environment=`test`, operation_id=`test-release-20260802-feedback-fix-test-r260802h-r1`, status=`APPLIED`; migration status counts are `APPLIED=5`, `SKIPPED_ALREADY_APPLIED=374`, `FAILED/RUNNING=0`.
- Runtime version UI verification: real browser check passed with test server frontend, releaseTag and change note visible, `consoleErrors=[]`; raw non-secret evidence is `runtime-version-ui-evidence.json`.
- Business import verification blocker: `李萍.xlsx` import reaches the new runtime but does not pass business acceptance on current test-server data. `测试租户/aoteman` skipped rows with `WORK_ORDER_NOT_FOUND`; `芋道源码/admin` still hit data prerequisites including `ACTIVE_TASK_NOT_FOUND`, `PROCESS_NOT_FOUND`, `WORKSTATION_NOT_FOUND`, and `FEEDBACK_USER_NOT_FOUND`.
- Closeout verification: task-closeout preview/apply passed, `build-release-r260802c-r3.log` was deleted, task core records and UI evidence were kept, all `r260802-feedback-fix-test*` release worktree registrations and physical directories were removed, temporary branch `codex/release-info-change-notes-20260802` was deleted after merge verification, and no process references the removed paths.

## Conclusion

- The requested test-only publish is successful for `release-20260802-feedback-fix-test-r260802h-r1`.
- The original stale-code/root-cause risk is resolved: the test server now runs a release sourced from `f0c34dfed910f52f9c03b401e976cbd2d0424e00`, not the old `b99246f58` package.
- The remaining `李萍.xlsx` report-import failure is a test-server business data precondition blocker, not evidence that the new code failed to deploy.
- Task closeout is complete.
