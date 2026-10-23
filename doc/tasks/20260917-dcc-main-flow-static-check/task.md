# DCC Main Flow Static Check

## Goal

Use gpt-5.5 high subthreads to statically inspect the DCC main-flow code for logic defects across controlled browsing, revision/check-in, file lifecycle, and upload-to-controlled-save flows. The main thread must review each reported finding, fix confirmed defects, skip non-issues, and iterate until the subthreads report no remaining static logic issues in scope.

## Milestones

- Read repository rules and DCC design constraints.
- Dispatch readonly static review subthreads for the four target flows.
- Review each child finding against current source code and business rules.
- Write RED BDD/static contracts for confirmed defects before production changes.
- Implement confirmed fixes only in the affected DCC main-flow logic.
- Run focused GREEN/REGRESSION verification or record blockers.
- Mark `ready_for_closeout`, run cleanup preview/apply where allowed, then mark `completed` or `blocked`.

## Expected Verification

- Focused static or unit tests covering each confirmed defect.
- Targeted backend Maven and/or frontend pnpm checks for touched files where practical.
- `git diff --check`.
- Child subthread re-check confirms no remaining static logic issue in the four requested flows.

## Current Status

blocked - 重复准备记录，已收口至 doc/tasks/20260917-dcc-main-flow-static-logic-audit/；以主任务 verification-report.md 为准，不独立声明完成。Git 提交推送及正式 cleanup 尚未执行。

## BDD

- BDD: Controlled browsing permission split -> Given a user has only file-name view permission, When they search, browse, switch directories, versions, or select relation candidates, Then they can discover authorized names only and cannot receive body, detail, preview, or mixed-context data unless content permission is separately granted.
- BDD: Revision/check-in controlled change -> Given an existing controlled file is changed through checkout/check-in, When the user submits the change with a summary, Then the system preserves old versions, locks approval content, applies major/minor notification rules, and does not auto-switch existing relations.
- BDD: Lifecycle approval to controlled save -> Given an uploaded or revised version is under approval, When it is rejected or finally approved with stamped PDF and valid default directory, Then rejection ends the approval with reason and final approval saves to the default controlled directory or fails with the real reason.
- BDD: Upload-to-controlled-save closed loop -> Given an uploader selects a project and template folder, When they create a first-time file and submit it through approval, Then upload requires a valid template/default directory, rejects duplicate logical identity in upload entry, locks content during approval, and becomes controlled only after stamped PDF/default-directory save succeeds.

## Design Constraints Check

- No fallback, downgrade, swallowed errors, mock success, or compatibility patch.
- No E2E unless explicitly requested in the current turn.
- No Git commit or push without current-turn authorization.
- DCC controlled browsing must keep name and content permissions separate.
- Upload entry is for first creation only; existing files change only through checkout/check-in.
- Stamped PDF and official default directory are mandatory for final controlled save.
- Existing relations stay on the selected version until receiver confirms update.
