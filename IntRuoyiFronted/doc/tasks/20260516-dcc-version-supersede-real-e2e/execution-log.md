# Execution Log: DCC 同名新版本替代真实 E2E

BDD: 相同版本禁止重复上传 -> Given 用户已在真实 DCC 上传页选择同一文件类别、同一文件名称、同一文件编号并成功发布 `1.0` / When 用户再次提交同链路的 `1.0` / Then 前端必须暴露明确版本错误，且不能生成新的有效修订或新的待审批链路。

BDD: 同类别同名新版本发布后旧版本自动失效 -> Given 用户已在真实 DCC 链路中成功发布同一逻辑文件的 `1.0` / When 用户再次提交并审批通过同一文件类别、同一文件名称、同一文件编号的 `2.0` / Then `2.0` 必须成为新的 `ACTIVE`，旧版 `1.0` 必须自动变为 `SUPERSEDED`，并记录 `supersededByFileId` 指向新版本。

RED: repository coverage for controlled-file version supersede real E2E -> FAIL, before this task the repository had no single real Playwright flow proving duplicate-version rejection and higher-version supersession on the same logical controlled-file chain.

RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-version-supersede-real-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-version-supersede-real-e2e\scripts\verify-dcc-version-supersede-real-e2e.mjs` -> FAIL, `playwright-cli` required an explicit named browser session open before `run-code` could execute.

RED: same real command after opening the named session -> FAIL, the upload-page `文件名称` control in the current workspace had already changed to the history-enabled combobox shape, so the previous plain-text selector no longer resolved.

RED: same real command after the selector fix -> FAIL, the live DCC route no longer assigned `admin`; the first submission created file `19`, but the current runtime resolved the upload route to real users `145 / 148 / 146 / 147`, so a single-account approval script could not find the new approval row.

RED: same real command after switching to deterministic real approvers -> FAIL, the runtime schema was behind the code contract and both `dcc_file_category_distribution_rule` and `dcc_controlled_file_distribution` were missing `distribution_medium`, causing `DCC审批任务` to fail with backend SQL errors during real approval-task loading.

RED: same real command after the schema repair -> FAIL, the duplicate-version path still timed out because the script registered the submit-response waiter after clicking `提交审批` and could miss the fast failure response.

RED: same real command after moving the duplicate-response waiter before the click -> FAIL, the first-stage task did not always go to the hard-coded `docReview` account; the live BPM instance for file `24` assigned `DOC_CONTROL_REVIEW` to user `117`, so the approval actor had to be resolved from the actual active BPM task rather than assumed from the stage template.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-version-supersede-real-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-version-supersede-real-e2e\scripts\verify-dcc-version-supersede-real-e2e.mjs` -> PASS, logical file `DCC-VERSION-SUPERSEDE-1778946949161-文件` first published as `1.0 / ACTIVE`, rejected the duplicate `1.0` with `Controlled file version must be greater than the current chain version`, then published `2.0 / ACTIVE` and converted `1.0` to `SUPERSEDED` with `supersededByFileId=26`.
