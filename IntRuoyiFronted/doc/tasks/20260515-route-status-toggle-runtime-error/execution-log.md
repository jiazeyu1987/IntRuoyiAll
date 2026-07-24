# Execution Log: MES 工艺路线状态开关报错排查修复

BDD: route status toggle exposes the real blocker -> Given an operator logs into the real local frontend and opens `/mes/pro/route`, When they toggle the status of the failing route row, Then the system either updates the route status successfully or surfaces the exact backend/frontend blocker without hiding the failure.

- M1: Completed. The previous frontend task `doc/tasks/20260514-remove-auto-schedule-worktree/task.md` was explicitly blocked because the user switched priority to this runtime bug.
- M2: Completed. This task document and execution log were created before any production code changes for the bug fix.
- M3 GREEN: Real-path reproduction on `http://127.0.0.1:8081/mes/pro/route` confirmed that `ROUTE-XLSX-00001` failed during enable, while backend log `C:\Users\BJB110\logs\yudao-server.log` recorded the exact blocker `工艺路线必须要有关键工序` for route id `900025`.
- RED: `npx.cmd --yes --package @playwright/cli playwright-cli --session route-status-toggle-repro run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-route-status-toggle-runtime-error\scripts\verify-route-status-business-message.mjs` -> FAIL, the real page showed the generic message `服务器错误,请联系管理员!` instead of the backend business blocker.
- M5 GREEN: Updated [service.ts](/D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/src/config/axios/service.ts) so the frontend error-message extraction also reads `data.message`, and the `code === 500` branch displays the resolved backend message instead of forcing the generic 500 text.
- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session route-status-toggle-repro run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-route-status-toggle-runtime-error\scripts\verify-route-status-business-message.mjs` -> PASS, the real page now shows `工艺路线必须要有关键工序`.
- GREEN: `pnpm exec eslint src/config/axios/service.ts` -> PASS.
