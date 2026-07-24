# Execution Log: DCC Access-Rules Real-Content E2E

BDD: access-rules page shows the real selected directory name -> Given an
administrator logs in through the real frontend path and the live directory tree
returns at least one visible directory, When the user opens the DCC
access-rules page and selects a directory, Then the tree node text and current
directory title match the real directory name from
`/admin-api/dcc/directories/tree`.

BDD: access-rules page shows real access-rule rows -> Given at least one visible
directory has live access-rule rows, When the user opens that directory on the
access-rules page, Then the table shows the same number of rows as
`/admin-api/dcc/directories/{id}/access-rules` and each row reflects the real
subject, permissions, active flag, and change reason.

BDD: missing runtime prerequisites fail loudly -> Given the runtime lacks a
visible directory, access-rule row, or required lookup data, When the
verification runs, Then it fails with the exact missing prerequisite and does
not mark the page content as covered.

RED: `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\docs\qa\test-suite-evidence.md` -> FAIL, missing required `RED:`, `GREEN:`, `Verification`, and `Blockers` markers before evidence was filled in.

RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-access-rules-real-content-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-access-rules-real-content-e2e\scripts\verify-dcc-access-rules-real-content-e2e.mjs` -> FAIL, `no_visible_directory_with_access_rules:visibleDirectoryCount=2699:sample=900003:3.DMR|900004:01.图纸|900005:01成品图纸|900006:00- 作废图纸_成品|900007:01- 导丝类_成品`.

RED: runtime inspection via the same Playwright session -> FAIL, `/admin-api/dcc/directories/tree` returned `code=0` and 2699 visible directories, but `/admin-api/dcc/directories/{id}/access-rules` returned zero non-empty rule sets across all visible directories.

GREEN: not reached, because the live runtime currently has no visible directory with real access-rule rows for the content-level comparison.
