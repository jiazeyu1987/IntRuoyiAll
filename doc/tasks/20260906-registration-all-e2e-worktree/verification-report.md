# Verification Report

## Final Result

- Status: PASS
- Final round: `round27-post-reminder-closure`
- Summary: `doc/tasks/20260906-registration-all-e2e-worktree/artifacts/round27-post-reminder-closure/summary.json`
- Result count: 15 PASS, 0 FAIL, 0 BLOCKED
- Supplemental reminder document closure: `round25-reminder-doc` and `round26-reminder-reconfig-doc`
- Document case matrix: all registration E2E document cases are now recorded as `PASS` or document-rule `SKIPPED`.

## Commands

- `node --check IntRuoyiFronted\tests\e2e\registration-certificate-change-remaining-real.spec.js`
- `node --check IntRuoyiFronted\tests\e2e\registration-certificate-renewal-lifecycle-real.spec.js`
- `mvn.cmd --% -pl yudao-module-dcc -Dtest=DccRegistrationCertificateChangeServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn.cmd --% -pl yudao-server -am -DskipTests package`
- `.\doc\tasks\20260906-registration-all-e2e-worktree\run-registration-e2e-round.ps1 -RoundName 'round23-final-registration-all-e2e' -BaseUrl 'http://127.0.0.1:8158' -BackendUrl 'http://127.0.0.1:48158'`
- `.\doc\tasks\20260906-registration-all-e2e-worktree\run-registration-e2e-round.ps1 -RoundName 'round27-post-reminder-closure' -BaseUrl 'http://127.0.0.1:8158' -BackendUrl 'http://127.0.0.1:48158'`
- `node --check doc\tasks\20260906-registration-all-e2e-worktree\registration-reminder-config-and-delivery-real.cjs`
- `node doc\tasks\20260906-registration-all-e2e-worktree\registration-reminder-config-and-delivery-real.cjs` with `REG_REMINDER_SKIP_INITIAL_SETUP=true`
- `node doc\tasks\20260906-registration-all-e2e-worktree\registration-reminder-config-and-delivery-real.cjs` with `REG_REMINDER_RESTORE_ONLY=true`

## Final Round Cases

- `action-panel`: PASS
- `upload-admin-role-approval`: PASS
- `change-continue-approval`: PASS
- `change-submit-approval`: PASS
- `change-remaining`: PASS
- `change-ui-smoke`: PASS
- `download-search-targeted`: PASS
- `list-sort`: PASS
- `real-flow`: PASS
- `renewal-lifecycle`: PASS
- `renewal-row-dialog`: PASS
- `upload-button`: PASS
- `upload-submit-repro`: PASS
- `reminder-sort-runtime`: PASS
- `business-time-simulation`: PASS

## Supplemental Reminder Cases

- `reminder E2E-1`: PASS. Saved notification recipients by threshold through the registration certificate page and reopened the dialog to confirm persisted echo.
- `reminder E2E-2`: PASS. `chudongchuan` saw the T_30 reminder for `E2E-REMINDER-30M-R25-30M-0`; `wanglixuan` did not see that reminder.
- `reminder E2E-3`: PASS. `chudongchuan` saw the T_8 reminder for `E2E-REMINDER-8M-R25-8M-0`; the detail page showed `提醒：到期前 8 个月`. Renewal recovery is covered by the final renewal lifecycle run.
- `reminder E2E-4`: PASS. `chudongchuan` and `admin` saw the T_2 reminder for `E2E-REMINDER-2M-R25-2M-0`; `wanglixuan` did not. The detail page showed `提醒：到期前 2 个月`. Renewal recovery is covered by the final renewal lifecycle run.
- `reminder E2E-5`: PASS. `chudongchuan` and `admin` saw the T_1 reminder for `E2E-REMINDER-1M-R25-1M-0`; `wanglixuan` did not.
- `reminder E2E-6`: PASS. The 30-month recipient was changed to `wanglixuan`, a new T_30 certificate `E2E-REMINDER-30M-R26-30M-0` was triggered on `2026-09-01`, `wanglixuan` saw the new reminder, and `chudongchuan` did not. The config was restored afterward through the real frontend page.

## Notes

- Role/menu and company-scope baseline adjustments were performed only after the user explicitly authorized this task's required permission adjustments.
- No Git commit or push was performed.
