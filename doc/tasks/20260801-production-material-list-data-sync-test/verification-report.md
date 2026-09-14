# Verification Report

## Summary

- Result: data sync PASS, closeout Git status BLOCKED.
- Scope: data-only sync of `mes_kingdee_production_material_list` from local `ruoyi-vue-pro` to test server `172.30.30.58`; no package build, release, MinIO sync, production server, or backup-server operation.
- Target backup: pre-upsert backup and post-upsert/pre-linkage-fix backup both exist, are non-empty, and pass `gzip -t`.

## Database Verification

- Schema: local and test server are both MySQL `8.0.39`; target table column hash `a0c1daa82199770dfe2cd6b2b2a4fb76e9910e7390e1195a096ee44c49b54008` and index hash `90f6a7f2999db47e90697754ea936e34a0b0d9df18fbcc76bbc5e0e5f4a216df` match.
- Upsert: target final total `7,983`, distinct business keys `7,983`; tenant counts `1=2593`, `121=1466`, `122=2458`, `162=1466`.
- Business hash: source and target hashes match for all four tenants: `c38e445268fd70e9ec82fa84f2266c4aa55b5fe112325a5872bb82062079da04`, `a9942cf1fe163866ff24285aedb94116f3351b04e6130a6b93aa1e5fc740e243`, `316f54d06c5cef952ad46680c5169b10491dd580c9942c971b3437546fdf18f5`, `a9942cf1fe163866ff24285aedb94116f3351b04e6130a6b93aa1e5fc740e243`.
- Linkage: invalid cross-tenant or missing item links were cleared (`child_material_id=4610`, `product_id=365`); final `orphan_work_order`, `orphan_child_item`, `orphan_product_item`, `orphan_work_order_bom`, and `bom_mismatch` are all `0`.
- Cleanup: remote staging table `codex_pml_stage_20260801` was dropped and verified absent.

## API And Page Verification

- Health: `http://172.30.30.58:48081/actuator/health` returned `{"status":"UP"}`.
- API sample: tenant `1`, user label `admin`, `group-page` returned `total=236`; sample `sourceBillNo=PPBOM00309005`, `lineCount=29`; `detail-list` returned `29` rows with first child code `A001.02.070.105`.
- Page sample: `/erp/production/material-list` rendered in an authenticated read-only browser context and opened `PPBOM00309005` details with `29` rows.
- E2E caveat: official login-page E2E is BLOCKED because the test-server login page enables captcha; token-bootstrap page rendering was recorded only as read-only page evidence, not as proof of login-page E2E.

## Validators

- PASS: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc\tasks\20260801-production-material-list-data-sync-test\database-schema-evidence.md`.
- PASS: `python C:\Users\BJB110\.codex\skills\backup-disaster-recovery-readiness\scripts\validate_backup_disaster_recovery.py --evidence doc\tasks\20260801-production-material-list-data-sync-test\recovery-evidence.md`.
- PASS: `rg -n "生产用料清单跨环境白名单|生产用料清单同步|mes_kingdee_production_material_list" docs\database-rules.md docs\experience-index.md`.
- PASS: `git diff --check -- doc/tasks/20260801-production-material-list-data-sync-test docs/database-rules.md docs/experience-index.md` returned only CRLF conversion warnings and no whitespace errors.

## Closeout

- Task status: `ready_for_closeout`.
- Cleanup: `task-closeout-cleanup preview/apply` passed; staging SQL plus temporary `database-schema-evidence.md` and `recovery-evidence.md` were deleted after validator PASS was copied into this report.
- Git closeout blocker: repository is already `int_main...origin/int_main [ahead 10]` with unrelated dirty/staged changes in frontend files and other task docs; this task has not committed or pushed changes.
