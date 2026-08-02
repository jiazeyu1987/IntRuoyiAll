# 提交第三方报工修复并重新发布测试服验证报告

## Status

IN PROGRESS

## Evidence

- Feedback fix baseline source: `d6d0e7b9ea45e37001e95c5f761af9df815890f1` contained `DirectWorkstationResolution` / `resolveDirectFeedbackWorkstation` and was pushed to `origin/int_main`.
- Invalid build evidence: `r260802c-r1` missing package metadata, `r260802c-r2` missing frontend Vite dependency, and `r260802c-r3` missing manifest v1 due to sourceRepo dictionary property handling.
- Release script fix verification: targeted RED reproduced `Get-ReleaseSourceRepoIdentity` failure for `[ordered]` sourceRepo entries; GREEN passed after `Get-ReleaseObjectPropertyText` gained `System.Collections.IDictionary` key support.
- Invalid build evidence: `r260802d-r1` built backend/frontend but failed before manifest v1 when `Invoke-ReleaseCodexExec` tried to launch `codex.ps1` directly; source, manifest and change notes therefore cannot be confirmed.
- Release script fix verification: targeted GREEN passed for Codex command resolver, sourceRepo identity, and release change-set tests after the resolver began preferring native `.cmd`/`.exe` commands over PowerShell shims.
- Current blocker before publish: the second release script fix must be committed/pushed, then a fresh clean release worktree and new releaseTag must be used; failed `r260802c-r1/r2/r3` and `r260802d-r1` artifacts are invalid for publishing.
