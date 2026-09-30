# 20260917 DCC Main Flow Static Audit Fix

## Task Objective

Use four gpt-5.5 high subthreads to statically audit the DCC controlled-file main flows, then review each reported logic issue in the main thread. Confirmed issues must be fixed; false positives must be skipped. Iterate until the audited main flows have no confirmed static logic issues in scope.

## Scope

- Controlled browsing flow: name view permission, content view permission, browse/search, related-file candidate discovery, link creation permissions and business scope, revalidation on directory/file/version switches, and no cross-directory or cross-version data reuse.
- Version upgrade flow: checkout/checkin, edit locking, version rule classification, major/minor notification boundaries, approval and stamped PDF activation, and no automatic related-file switching.
- File lifecycle flow: configuration, first upload, approval/rejection, stamped PDF before activation, controlled-file state, modification, replacement, and related-file major-version notification.
- Upload-to-controlled-save flow: source upload, approval, stamped PDF, default-directory controlled save, activation, failure exposure, and downstream permission governance.

## BDD

- BDD: Controlled browsing permission split -> Given a user has only name-view permission, When they browse/search/select related-file candidates, Then they can discover names but cannot receive正文、详情或预览; Given they also have content-view permission, When they open a file/version, Then content is returned only after the current directory/file/version context is revalidated.
- BDD: Version upgrade approval gate -> Given an existing controlled file is checked out and checked in, When a new version is submitted, Then old versions are retained, the new version waits for approval and stamped PDF activation, and only major-version changes notify eligible related processors without switching existing links automatically.
- BDD: File lifecycle state integrity -> Given a controlled-file version moves through upload, approval, rejection, stamped PDF, activation, modification and replacement, When each state transition happens, Then formal route, version lock, default directory save and history retention are enforced without fallback state success.
- BDD: Upload controlled-save closure -> Given a user uploads a new controlled file through a selected template folder, When they submit and the approval completes, Then the selected template, initial version, attachments, approval route, stamped PDF, default directory and activation result remain bound to the same target version.

## Milestones

- [ ] Read required docs and establish task record.
- [ ] Spawn four read-only static audit subthreads using gpt-5.5 high.
- [ ] Review subthread findings against current code and discard false positives.
- [ ] Apply minimal fixes for confirmed main-flow logic defects.
- [ ] Run focused static/unit verification allowed by project rules.
- [ ] Re-run/iterate subthread audits until no static logic issues remain.
- [ ] Prepare closeout evidence and record blockers if Git closeout cannot be completed without explicit authorization.

## Expected Verification

- Focused backend Maven tests or static contracts for touched DCC backend classes.
- Focused frontend type/static checks for touched DCC frontend files.
- `git diff --check`.
- No E2E unless explicitly requested by the user in the same turn.

## Design Constraints Check

- No fallback, downgrade, swallowed exception, mock success, or compatibility patch.
- No database writes, service start/restart, remote server operation, Git commit/push, or E2E without explicit same-turn authorization.
- Keep fixes scoped to confirmed DCC main-flow logic issues.
- Preserve independent chains: controlled browsing permissions, version upgrade, lifecycle, and upload-to-controlled-save.
- Use UTF-8 for Chinese text and PowerShell commands without `&&`.

## Current Status

blocked - 重复准备记录，已收口至 doc/tasks/20260917-dcc-main-flow-static-logic-audit/；以主任务 verification-report.md 为准，不独立声明完成。Git 提交推送及正式 cleanup 尚未执行。
