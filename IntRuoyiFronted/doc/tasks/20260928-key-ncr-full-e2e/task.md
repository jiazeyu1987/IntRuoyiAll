# Critical Deviation to QA NCR Full E2E

## Goal

Exercise the critical-deviation to manually initiated QA nonconformance review path through the real frontend, using one task-owned test batch. Verify initiation signing, automatic deviation closure and review linkage, QA review queue visibility, disposition signing, and post-disposition gates. Business actions must use visible Playwright UI only; no API or database writes.

## Milestones

1. Create an isolated test order and batch through the active-order and batch-execution UI. Status: completed.
2. Initiate a critical deviation with e-signature and confirm its generated number and batch association. Status: completed.
3. Initiate a QA nonconformance review from that deviation and confirm automatic closure and QA pending-review visibility. Status: completed.
4. Complete a QA disposition with e-signature and verify post-disposition gates. Status: blocked by the required review-material input, which is empty in the UI; no disposition was submitted.
5. Preserve screenshots, trace, network log, and outcome report. Status: completed.

## Expected Verification

- The unique marker `E2E-20260928-KEY-NCR` appears on the task-owned batch and deviation.
- The critical deviation receives a `PC-YYYYMM-NNNN` number, an initiation signature, and links to a QA review after manual NCR initiation.
- The deviation closes automatically and the QA review appears as pending in the QA queue.
- QA disposition is e-signed and the batch's downstream controls reflect the disposition. This final condition is not verified because the review UI marks `评审材料` as required and no material was available; execution stopped before disposition.

## Current Status

blocked

## Task-Owned Data

- Batch: `EDHRB-1790591237468`, batch number `E2E-20260928-KEY-NCR`.
- Deviation: `PC-202609-0009`, critical, linked to the task-owned batch.
- QA review: `EDHR-NCR-20260928184110-900000001163`, review ID 68, pending review.

## Evidence

See `IntRuoyiFronted/output/playwright/key-ncr-full-20260928/verification-report.md`, screenshots, trace, network log, and console log. No service restart, source edit, API call, or database operation was performed.
