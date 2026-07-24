# Execution Log: DCC 上传路线第三层岗位名称显示

BDD: upload route preview shows third-stage position names -> Given the DCC
upload page previews a category whose third-stage approval positions are
`900335` and `900336`, When the operator clicks `预览路线`, Then the preview must
show the configured岗位名称 instead of fallback text `岗位#900335 / 岗位#900336`.

BDD: resolved approver display stays intact -> Given the upload preview already
shows approval mode and resolved users, When the position-name lookup is fixed,
Then the existing approval-mode and resolved-user columns must remain unchanged.

- M1: Completed. Created the task package before production-code edits.
- RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-route-position-name-display run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-upload-route-position-name-display\scripts\verify-dcc-upload-route-position-name-display.mjs`
  -> FAIL, stage-three preview rendered
  `岗位#900335 / 岗位#900336`.
- M2: Completed. Captured the real upload-page regression showing the numeric
  fallback in the third-stage route preview.
- M3: Completed. Updated the upload preview to use approval-position lookup and
  extended the shared fixed local-position name map with `900335 ->
  编制部门负责人` and `900336 -> 授权代表`.
- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-route-position-name-display run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-upload-route-position-name-display\scripts\verify-dcc-upload-route-position-name-display.mjs`
  -> PASS, real upload preview for `生产用设备清单` displayed stage 3 as
  `编制部门负责人 / 授权代表`.
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm exec vue-tsc --noEmit -p tsconfig.relaxed.json`
  -> PASS.
- M4: Completed. Targeted regression verification and type checking are green.
- M5: Completed. Bug evidence validation is green and the task is ready for a
  task-only commit.
