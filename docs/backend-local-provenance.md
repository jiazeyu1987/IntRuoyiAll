# Local backend package provenance

The branch backend launcher requires a matching source/package receipt. Existing packages without a receipt fail startup; receipts must not be manually stamped onto an old package.

Build without stopping the running service:

```powershell
python scripts/runtime/backend-package.py build --root IntRuoyiBackend
```

After a successful build and verification, stop only the authorized backend process and run `scripts/runtime/start-branch-backend.ps1`. `-Build` performs the same certified clean build, but requires the port to be free first.

The receipt binds the executable JAR SHA-256 to module POMs, module `src/main` files and `.mvn` inputs, including uncommitted and untracked source files. Source changes during the build invalidate certification. A missing receipt, changed source, or replaced package blocks startup. The launcher checks again after copying the JAR and saves the receipt next to its runtime copy.

Scope: local branch launcher. Remote release images and externally supplied deployment packages have their own release provenance and must not be represented as certified by this local receipt. Dependencies supplied outside the repository and the Java/Maven installation are not covered by the source fingerprint.

Approval/workbench identifiers are compared byte-for-byte across schema boundaries; display text retains its existing search/sort behavior. Never normalize identifiers by lowercasing or change the whole database collation to work around a join error.

Protected error diagnostics retain exception class, service code, numeric SQL vendor code and validated SQLSTATE through a bounded cause chain. Exception messages, SQL text, parameters and tokens remain excluded. Workbench source adapters log source ID and count/read operation separately. A failing source still fails the whole request; it is never silently omitted.

## Verified regression (2026-10-29)

Independent worktree `codex/workbench-root-fix`, runtime slot 60 (8315/48315): certified clean backend build, 17 targeted Java tests, Python provenance rejection cases, launcher contracts, and 128 complete MySQL EXPLAIN plans passed. Identifier comparisons also passed against mixed unicode_ci/0900_ai_ci/bin fixtures.

Playwright used the real login and UI: personal workbench loaded 4982 visible tasks and 1 hidden task; approval todo loaded 125 rows and done loaded 20476 rows. Refresh, next/previous page, and hidden/visible switches passed. Counts are observations of shared test data, not fixed assertions. Pagination now ignores automatic page normalization while the authoritative total is unknown; loading a new page no longer resets it to page 1. Three existing frontend profile contracts passed.

External icon-host timeouts and a shared release-workflow scheduler file-lock error were observed separately; no task-loading failure occurred after backend readiness. No API calls substituted for UI actions.
