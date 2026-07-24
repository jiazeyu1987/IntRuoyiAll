# Execution Log: 生产工单临时冻结真实数据 E2E 补强

BDD: 生产工单页展示临时冻结总开关并请求真实状态 -> Given 用户通过真实登录进入生产工单页 / When 页面完成加载 / Then 页面应显示 `临时冻结` 开关，并请求真实的 `temporary-freeze-status` 接口。

BDD: 生产排产页应排除冻结工单 -> Given 用户通过真实登录进入生产排产页 / When 页面请求待排产工单列表 / Then 工单分页请求必须携带 `temporaryFrozen=false`。

BDD: 受控脚本执行临时冻结开关往返 -> Given 操作者显式允许破坏性验证并接受可能清理未结束排产任务的风险 / When 脚本在真实生产工单页执行临时冻结开关往返 / Then 页面应成功完成开关切换并在结束时恢复原始状态。

RED: frontend repo lacked dedicated temporary-freeze real-data E2E assets -> FAIL, before this task there were no task-local Playwright scripts under `yudao-ui-admin-vue3/doc/tasks/...` for the temporary-freeze feature; verification depended on a single cross-repo ad-hoc script.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session temp-freeze-e2e-smoke run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-pro-workorder-temporary-freeze-real-e2e\scripts\verify-temp-freeze-smoke.mjs` -> PASS, returned `{"url":"http://127.0.0.1:8081/mes/pro/work-order","hasSwitchText":true,"switchCount":1,"statusHttpCode":200,"statusApiCode":0,"enabled":false}`.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session temp-freeze-e2e-task-scope run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260515-pro-workorder-temporary-freeze-real-e2e\scripts\verify-temp-freeze-task-scope.mjs` -> PASS, returned `{"url":"http://127.0.0.1:8081/mes/pro/task","requestUrl":"http://localhost:48081/admin-api/mes/pro/work-order/page?pageNo=1&pageSize=10&status=1&type=1&temporaryFrozen=false","httpCode":200,"apiCode":0}`.

GREEN: guarded destructive script safety gate -> PASS, `exercise-temp-freeze-roundtrip.mjs` refused to run without `ALLOW_DESTRUCTIVE_TEMP_FREEZE_E2E=1` and emitted the expected fail-fast risk message instead of mutating real schedule data.
