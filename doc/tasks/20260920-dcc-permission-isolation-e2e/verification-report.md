# Verification Report

## Status

PASS - COMPLETE PERMISSION ISOLATION E2E VERIFIED; CLEANUP COMPLETE

## Evidence

- Write-flow edit/approval evidence: [permission-isolation-edit-approval-write.json](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/permission-isolation-edit-approval-write.json)
- Association create evidence: [permission-isolation-association-write.json](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/permission-isolation-association-write.json)
- Association cleanup evidence: [permission-isolation-association-cleanup.json](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/permission-isolation-association-cleanup.json)
- Post-write admin matrix: [permission-isolation-admin-post-write.json](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/permission-isolation-admin-post-write.json)
- Post-write wangsiyu matrix: [permission-isolation-wangsiyu-post-write.json](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/permission-isolation-wangsiyu-post-write.json)
- Post-write zhaojie matrix: [permission-isolation-zhaojie-post-write.json](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/permission-isolation-zhaojie-post-write.json)
- Admin screenshots: [admin-browser.png](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/admin-browser.png), [admin-preview.png](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/admin-preview.png), [admin-detail.png](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/admin-detail.png), [admin-association-management.png](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/admin-association-management.png), [admin-approval-center.png](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/admin-approval-center.png)
- Wangsiyu screenshots: [wangsiyu-browser.png](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/wangsiyu-browser.png), [wangsiyu-preview.png](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/wangsiyu-preview.png), [wangsiyu-detail.png](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/wangsiyu-detail.png)
- Zhaojie screenshots: [zhaojie-browser.png](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/zhaojie-browser.png), [zhaojie-detail.png](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/zhaojie-detail.png), [zhaojie-association-management.png](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/zhaojie-association-management.png), [zhaojie-approval-center.png](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/zhaojie-approval-center.png)
- Write screenshots: [edit-restored.png](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/edit-restored.png), [approval-rejected.png](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/approval-rejected.png), [association-created.png](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/association-created.png), [association-revoked.png](C:/IntRuoyiAll-int_main/doc/tasks/20260920-dcc-permission-isolation-e2e/artifacts/association-revoked.png)
- Runtime: frontend `8061`, backend `48061`, backend health `UP`; `48081` was not touched.
- Focused backend permission tests: 164 passed.
- Static contracts: content matrix and viewer permission passed; independent view-matrix-source check failed on a literal formatting expectation.
- After backend recovery and the frontend route fix, the real browser reruns for `admin`, `wangsiyu`, and `zhaojie` completed successfully.
- The write-flow Playwright run completed the independent positive actions:
  - `wangsiyu` changed the task file name through `修改基础信息`, received HTTP 200 from the metadata PUT, then restored the original name through the same dialog with a second HTTP 200.
  - `admin` opened a real DCC approval task, selected `审核不通过`, entered a reason and electronic-signature password, submitted through the page, received HTTP 200, and the target row left the TODO list.
  - `admin` opened the real `/mdm/project-code` maintenance page, selected the target file in the assignment dialog, created a correction assignment for `wangsiyu`, received HTTP 200, and saw the assignment in `分配记录`.
  - The created assignment was revoked through the real `撤回` prompt, with revoke HTTP 200 and final row status `REVOKED`.
- Post-write permission regression for all three accounts completed successfully. The target file remained visible to `zhaojie`, preview/download remained absent for `zhaojie`, preview/download remained successful for `wangsiyu`, and `admin` retained the expected approval/edit/association controls.

## Result

Verified honestly through real frontend Playwright:

1. `zhaojie` can search and view metadata for `CODEX 文件上传流程测试 20260808`; preview and download are not exposed, and detail has no preview button.
2. `wangsiyu` can preview and download the same controlled file. The real preview opened from the row; `/preview-metadata` and `/preview` both returned 200. Download was confirmed through the UI and the controlled-file download endpoint returned 200.
3. The edit gate is independently visible: `admin` and `wangsiyu` expose the real row `更多 -> 修改基础信息` menu; `zhaojie` does not.
4. The approval gate is independently visible: `admin` had 8 real `审核` buttons before the task-owned rejection and 7 after it; `wangsiyu` and `zhaojie` have zero approval todo rows and zero review buttons.
5. The detail page's `关联文档入口` now navigates to the registered real route `/mdm/project-code?...`. `admin` and `wangsiyu` load the DCC project-code maintenance page and expose the association maintenance surface; `zhaojie` reaches the same page only through the diagnostic fallback because the detail entry is disabled, and has no project-code edit/permission actions.
6. `admin` can preview/download and see richer approval, signature, and traceability information for the same file.
7. No API or database write was used to manufacture permission fixtures. The only business writes were the real Playwright page actions described above.
