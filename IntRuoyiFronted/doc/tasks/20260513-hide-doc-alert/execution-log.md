# Execution Log: Hide global DocAlert banners

BDD: hide shared documentation banner globally -> Given pages across the admin frontend render documentation links through the shared `DocAlert` component, When a user opens a page such as MES production scheduling, Then the green documentation banner should not be visible anywhere in the page.

BDD: preserve page content under the banner -> Given a page previously rendered both a `DocAlert` banner and its normal business content, When the banner is hidden, Then the page content and route should still render normally without changing unrelated success UI patterns.

GREEN: current task documentation -> PASS, created `doc/tasks/20260513-hide-doc-alert/` before editing the shared component.
- RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session hide-doc-alert-red run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260513-hide-doc-alert\scripts\verify-doc-alert-hidden.mjs` -> FAIL, the real route `http://127.0.0.1:8081/mes/pro/task` still displayed the `生产排产、工序流转卡` DocAlert banner.
- M3: Completed. RED evidence was captured on the real MES production scheduling page after logging in through the local frontend.
- M4: Completed. Updated the shared `src/components/DocAlert/index.vue` component to render `null` globally while preserving its public props contract.
- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session hide-doc-alert-green-2 tab-new http://127.0.0.1:8081/mes/pro/task` plus follow-up `snapshot`/`eval` checks -> PASS, the real route `http://127.0.0.1:8081/mes/pro/task` rendered normally, showed the production scheduling page content, and `document.body.innerText.includes('生产排产、工序流转卡')` returned `false` on both desktop and `390x844` viewport sizes.
- INFO: `pnpm ts:check` -> BLOCKED BY ENVIRONMENT, `vue-tsc --noEmit` aborted with Node heap out-of-memory before reporting frontend type issues.
- M5: Completed. Real-page GREEN verification and evidence updates are done, and the task is ready for a scoped frontend commit.
