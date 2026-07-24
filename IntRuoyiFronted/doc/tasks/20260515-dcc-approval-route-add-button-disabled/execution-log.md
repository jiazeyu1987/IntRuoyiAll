# Execution Log: DCC 审批路线新增按钮不可点击排查

BDD: 审批路线页面可新增路线 -> Given 用户已通过真实前端入口登录且具有 DCC 审批路线管理权限，When 用户进入审批路线页面并选择一个可配置的文件类别，Then “新增路线”按钮应可点击并能打开新增路线表单；若前置条件缺失，页面必须暴露准确阻塞信息。

- M1: Completed. 上一条前端任务 `20260515-route-last-process-key-flag-toggle-e2e` 已记录为 blocked，阻塞原因是 MES BOM 主数据缺失，不阻塞当前 DCC 页面排查。
- M2: Completed. 当前任务文档与执行日志已在排查前创建。
- RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-approval-route-add-button run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-dcc-approval-route-add-button-disabled\scripts\inspect-add-route-button.mjs` -> PASS as reproduction evidence, the real page first showed `initialDisabled=true` for “新增路线” before any file category was selected.
- GREEN: same Playwright verification command -> PASS, after selecting file category `产品技术要求`, the same real page showed `afterSelectDisabled=false`, loaded 48 category options successfully, and clicking the button produced a visible dialog with no page errors.
- M3: Completed. 用户所说“不可点击”已通过真实页面复现，表现为未选类别时前端禁用态，而不是点击报错。
- M4: Completed. 根因已定位到 `src/views/dcc/controlled-file/routes/index.vue` 的 `:disabled="!queryParams.categoryId"`。
- M5: Completed. 当前未发现需要修复的生产代码缺陷；问题属于页面前置条件缺少显式提示。
- M6: Completed. Playwright 已完成回归式验证并确认：选择类别后按钮可正常点击。
