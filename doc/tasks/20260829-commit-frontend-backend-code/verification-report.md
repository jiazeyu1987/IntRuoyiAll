# Verification Report

## Current Status

blocked - Code commits are local; push to `origin/int_main` failed because the available GitHub network/authentication paths are not currently usable.

## Evidence

- Branch: `int_main`.
- Remote: `origin` at GitHub HTTPS.
- Initial remote fetch required one-time direct Git config because configured local proxy `127.0.0.1:7890` was not listening and GitHub 443 direct connectivity passed.
- Branch runtime guard: PASS for `int_main/int_main`, frontend `8081`, backend `48081`.
- Staged boundary: 165 frontend/backend files, 0 out-of-scope paths.
- Secret scan: 0 high-confidence secret paths after replacing a real-E2E hardcoded password default with required env configuration.
- Size scan: no staged file over 50 MiB.
- `git diff --cached --check`: PASS.
- `node --check` for credential-reviewed real-E2E scripts: PASS.
- Code commit: `bf94b2a18 chore: commit frontend and backend updates`.
- Residual backend-code commit: `478147253 chore: commit backend SQL follow-up updates`.
- Final backend-test residual commit: `08c752160 test: update batch record report DB coverage`.
- Residual scan after commits: 0 non-log dirty files under frontend/backend roots.
- Cleanup preview/apply: PASS with no deletions, blockers, or warnings.

## Push Attempts

- HTTPS direct push with the broken local proxy disabled: FAILED, connection reset.
- SSH to GitHub port `443`: FAILED, public key denied.
- SSH to GitHub default SSH endpoint: FAILED, public key denied.
- HTTPS direct push with `schannel` and HTTP/1.1: FAILED, connection reset.

## Remaining Verification

- Restore working GitHub HTTPS/proxy connectivity or configure an authorized SSH key.
- Push `int_main` to `origin`.
- Confirm final `git status --short --branch` no longer shows local commits ahead.
