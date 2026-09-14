# Release Review Report

## Task Summary

- Review scope: current trusted-time collection and evidence export, official-signature time boundaries, the round-2 archive renderer changes, related tests, and static UI contracts.
- Review mode: independent static release review; no product code, task state, runtime service, remote server, database, or real E2E operation was changed or executed.
- Verification rerun: MES archive/signature regression 36/36 PASS; Infra trusted-time/inspection contracts PASS; trusted-time frontend static contract PASS; `pnpm ts:check` PASS.

## Structured Decision

- logic_status: fail
- usability_status: fail
- ui_status: pass

## Logic Review

- Status: fail
- Blocking Issues:
  1. `RuntimeTimeEvidenceExporter.export` generates a trusted-time ZIP for any saved inspection without first proving that the run contains the required `trusted-time-prod` and `trusted-time-audit` evidence. At `RuntimeTimeEvidenceExporter.java:34-45`, generation starts immediately, and `safeChecks` at lines 81-82 explicitly turns a missing checks list into an empty list. A legacy PASS inspection created before trusted-time checks existed can therefore be exported with the title and conclusion of a trusted-time report while containing no trusted-time evidence. This violates the task constraint that missing time evidence must fail explicitly and must not produce a default-success report.
  2. `RuntimeTrustedTimeParser.validate` only rejects a null Stratum (`RuntimeTrustedTimeParser.java:132-134`). Numeric but invalid NTP strata such as `0` or `16` pass this gate when the other strings are valid, despite the PRD requiring Stratum abnormalities to prevent PASS. The tests cover missing and non-numeric Stratum, but not numeric boundary values.
  3. The active execution page does not apply the stable signature ID tie-breaker that the archive renderer now uses. `ExecutionPage.vue:4331-4335` sorts only by second-precision `signedAt` and takes the last row. The backing page query orders equal timestamps by descending ID (`MesProBatchRecordExecutionSignatureMapper.java:67-68`), so stable JavaScript sorting preserves the newest ID first and the page then selects the lowest/older ID. Two same-action signatures created in the same second can therefore display the stale signer in the signature cell.
- Notes:
  - Round-2 PDF/XLSX/printable archive changes correctly stop using `signatureDisplayAt` or `selectedSignedAt` as the official displayed time, use `signedAt`, fail on missing official time, and use a stable ID tie-breaker in the printable archive path.
  - The focused regression suite passing does not cover the three cases above.

## Usability Review

- Status: fail
- Blocking Issues:
  - The same-second signature selection defect can show an older signer as the current signature in the form cell. The user has no way to detect or correct that misleading display from the page, so this blocks release even though the timestamp text itself now comes from `signedAt`.
- Notes:
  - Trusted-time failure reasons, empty state, download loading state, and abnormal-run export availability are otherwise clear in the current static UI.

## UI Review

- Status: pass
- Blocking Issues:
  - None in the static scope requested by the reviewer packet.
- Notes:
  - The trusted-time panel uses the existing table, tag, alert, button, loading, and responsive patterns. No static overlap, unreadable text, or broken layout contract was found. Runtime visual review was not required by this packet.

## Blocking Issues

- blocking_issues:
  1. Evidence export accepts saved runs with no complete trusted-time evidence and can label a legacy PASS run as a trusted-time PASS package.
  2. Numeric invalid Stratum values are accepted.
  3. Same-second frontend signature selection chooses the older ID from the actual descending API order.

## Non-Blocking Suggestions

- non_blocking_suggestions: []

## Required Changes

- required_changes:
  1. Before creating any ZIP, validate that the saved run contains both canonical trusted-time checks, each with a non-null `trustedTime` object and matching canonical environment/host identity. Missing, duplicate, or mismatched time evidence must fail explicitly. Add regression tests for a legacy PASS run with no trusted-time checks and a partial/mismatched run; blocked checks with genuine captured failure evidence must remain exportable.
  2. Define and enforce the accepted synchronized Stratum range (for the current NTP contract, `1..15`) and add boundary tests proving `0` and `16` cannot PASS while valid boundaries remain accepted.
  3. In every latest-signature selector used by the active execution form, compare `signedAt` first and numeric signature `id` second. Add a frontend contract/unit test using the real API order (`signedAt DESC, id DESC`) with two same-action, same-second rows and assert that the higher ID is displayed.
  4. Rerun the MES archive/signature regression, Infra trusted-time/inspection suite, frontend signature selection test, trusted-time static contract, and TypeScript check.

## Final Decision

- final_decision: fail
