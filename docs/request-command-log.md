# Request and command log

## 2026-10-19: production database forward reconciliation

- Request: user directed that database consistency take priority, stated that data is backed up and the system has had little recent use, then asked to continue.
- Scope: prepare and validate a reviewed forward-only database reconciliation through the official migration/release mechanisms. Existing ledger records must not be rewritten. The user explicitly confirmed `PROD` on 2026-10-19 for the formal production operation.
- Commands/evidence intent: inspect fixed migration and supersession contracts, the relevant database/release rules, current local Git state, and prior read-only migration evidence. No password, token, private key, or connection secret is recorded.
- Current result: production host `172.30.30.57` was checked read-only after confirmation; backend and frontend returned HTTP 200. Running containers report release `release-20260723-intmain-codeonly-three-env-r260723s-r1`, while runtime release metadata reports `release-20260716-intmain-codeonly-three-env-r260716d-r1`. The official unbound static planner is blocked (22 checksum conflicts, 111 dependency blocks); 202 APPLY actions are not a complete consistency plan. Audit evidence remains incomplete for 518 migrations and records 38 unsatisfied rows across 13 selected DML checks. No database writes performed.
