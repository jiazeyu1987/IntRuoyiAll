# 提交第三方报工修复并重新发布测试服验证报告

## Status

IN PROGRESS

## Evidence

- Feedback fix baseline source: `d6d0e7b9ea45e37001e95c5f761af9df815890f1` contained `DirectWorkstationResolution` / `resolveDirectFeedbackWorkstation` and was pushed to `origin/int_main`.
- Invalid build evidence: `r260802c-r1` missing package metadata, `r260802c-r2` missing frontend Vite dependency, and `r260802c-r3` missing manifest v1 due to sourceRepo dictionary property handling.
- Release script fix verification: targeted RED reproduced `Get-ReleaseSourceRepoIdentity` failure for `[ordered]` sourceRepo entries; GREEN passed after `Get-ReleaseObjectPropertyText` gained `System.Collections.IDictionary` key support.
- Invalid build evidence: `r260802d-r1` built backend/frontend but failed before manifest v1 when `Invoke-ReleaseCodexExec` tried to launch `codex.ps1` directly; source, manifest and change notes therefore cannot be confirmed.
- Release script fix verification: targeted GREEN passed for Codex command resolver, sourceRepo identity, and release change-set tests after the resolver began preferring native `.cmd`/`.exe` commands over PowerShell shims.
- Invalid build evidence: `r260802e-r1` has local `manifest.json` but failed because direct script invocation omitted required NAS JSON; operation not successful, so it remains invalid.
- Invalid build evidence: `r260802f-r1` failed before manifest v1 because empty Git change facts were rejected by PowerShell parameter binding.
- Release script fix verification: targeted GREEN passed for empty Git change facts after `Invoke-ReleaseCodexSummary` and prompt generation allowed empty collections.
- Current blocker before publish: the third release script fix must be committed/pushed, then a new releaseTag must be used; failed `r260802c-r1/r2/r3`, `r260802d-r1`, `r260802e-r1`, and `r260802f-r1` artifacts are invalid for publishing.
