# 测试服第三方报工导入不生效原因验证报告

## Conclusion

- 结论更新：测试服不是旧版本未发布导致；补齐授权范围内的用户与工作站基础数据后，`李萍.xlsx` 已能通过真实页面导入并生成 5 张正式报工。
- 当前成功结果：报工列表有 5 条本次导入明细，排产工单 `881MO093613` 和 `881MO093615` 的进度已更新。
- 剩余跳过原因仍是多种正式业务前置条件叠加：工单不存在、Excel 汇总/杂务行无排产工序、同排产工序拆成多个未完成任务且 Excel 任务号不等于系统 `PT-*` 任务号。

## Evidence

- Test server current release: `release-20260802-feedback-fix-test-r260802h-r1`.
- Test server release-info source commit: `f0c34dfed910f52f9c03b401e976cbd2d0424e00`, `dirty=false`.
- Test server backend image: `intruoyi-backend:release-20260802-feedback-fix-test-r260802h-r1`.
- Test server frontend image: `intruoyi-frontend:release-20260802-feedback-fix-test-r260802h-r1`.
- Current test server backend health: `UP`.
- Current test server frontend HTTP: `200`.
- `ThirdPartyFeedbackImportServiceImpl#importDirectWorkReportWorkbook` only creates import records and recalculates progress after `createFeedbackWithScheduleSnapshot` and `submitFeedback` succeed.
- `李萍.xlsx` parses to 69 processable rows under current parser behavior.
- Before data repair, `mes_pro_feedback_import_record` had no `2026-08-02` records; latest `李萍.xlsx` records were `2026-08-01 00:22:30` and had `feedback_id=0`.
- Before data repair, `mes_pro_feedback` had no `2026-08-02` formal feedback rows for the screenshot-related schedule orders.
- Before data repair, failure classification by backend execution order:
  - `ACTIVE_TASK_NOT_FOUND`: 28 rows.
  - `WORK_ORDER_NOT_FOUND`: 19 rows.
  - `PROCESS_NOT_FOUND`: 17 rows.
  - `WORKSTATION_NOT_FOUND`: 3 rows.
  - `FEEDBACK_USER_NOT_FOUND`: 2 rows.
- Data repair inserted 21 `system_users` rows and 18 `mes_md_workstation` rows on test server, all marked with `CODX_TPFB_20260802`.
- Real UI import after data repair returned `importedCount=5`, `pendingCount=0`, `submittedCount=5`, `skippedRows=65`.
- Generated formal feedback: `FB-000157`, `FB-000158`, `FB-000159`, `FB-000160`, `FB-000161`.
- DB verification: import records `220`-`224` bind to feedback ids `157`-`161`.
- DB verification: schedule order `881MO093613` progress is `1.965385`; schedule order `881MO093615` progress is `0.965385`.
- UI verification: 报工列表页面显示 5 条导入明细；排产工单页面显示 `881MO093613` 为 `1.97%`、`881MO093615` 为 `0.97%`。
- Retest on 2026-08-02 17:24: real UI import passed again with `importedCount=5`, `submittedCount=5`, `pendingCount=0`, `skippedRows=65`; generated `FB-000162` to `FB-000166` and import records `225` to `229`.
- Retest DB verification: schedule order `881MO093613` progress increased to `3.930769`; schedule order `881MO093615` progress increased to `1.930769`.
- Retest UI verification: 报工列表仍显示导入明细，排产工单页面显示 `881MO093613` 为 `3.93%`、`881MO093615` 为 `1.93%`。

## Impact

- Before data repair, no formal feedback was created, so the formal report list stayed empty and schedule progress did not change.
- After data repair, the five rows with complete prerequisites now create formal feedback and update schedule progress.
- If the intended business behavior is to import `881MO...-1-序号` task numbers or miscellaneous timing rows, the importer needs an explicit mapping/ignore rule; silently mapping them to arbitrary `PT-*` tasks would be a fallback and is not allowed.

## Required Fix Path

- Authorized data repair path is complete for users and workstations.
- Further expansion requires a business rule decision for Excel task code to system `PT-*` task mapping, and whether `组装` / `组件` / miscellaneous timing rows should be skipped or mapped.
- If more rows must import, test server also needs the missing production orders/schedule orders/processes created from formal source data, not inferred during import.
- Current retest found no new import-chain issue. Remaining skipped rows are data/mapping scope, not a regression in direct feedback creation or schedule progress recalculation.
