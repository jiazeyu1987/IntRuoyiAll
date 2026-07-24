# Execution Log

BDD: 讲解任务未进入运行态时不显示卡片 -> Given 产品列表页拿到一键讲解任务状态但 `active=false` 且 `running=false` When 页面计算顶部任务区域 Then “一键讲解任务”卡片必须不显示，即使存在历史命中数、完成时间或失败信息

BDD: 讲解任务进入执行或续跑态时显示卡片 -> Given 一键讲解任务状态为 `running=true` 或 `active=true` When 页面计算顶部任务区域 Then “一键讲解任务”卡片必须显示，并允许用户手动关闭后在下次触发任务时重新打开

BDD: 封面任务未进入运行态时不显示卡片 -> Given 产品列表页拿到一键封面任务摘要且状态为已停止、已完成或仅允许执行 When 页面计算顶部任务区域 Then “一键封面任务”卡片必须不显示，即使存在历史统计、失败信息或 `startAllowed=true`

BDD: 封面任务进入执行或续跑态时显示卡片 -> Given 一键封面任务摘要表示后台任务仍在运行或等待下一轮检查 When 页面计算顶部任务区域 Then “一键封面任务”卡片必须显示，并在用户再次触发任务时重新打开

RED: `node --test scripts/showroom-admin-product-list.test.mjs` -> FAIL, 新增 `task banners stay hidden when backend tasks are not running` 后首轮断言命中 `true !== false`，证明旧 `narrationScriptTaskVisible` / `coverTaskVisible` 会因为历史统计与 `startAllowed` 等非运行态字段把卡片错误显示出来。

GREEN: `node --test scripts/showroom-admin-product-list.test.mjs` -> PASS, 17/17 通过；讲解任务仅在 `active || running` 时显示，封面任务仅在 `active || running || taskStatus in [WAITING,RUNNING]` 时显示。

GREEN: `node --test scripts/showroom-admin-frontend.test.mjs` -> PASS, 21/21 通过，后台产品页固定任务区与现有静态契约未回退。

GREEN: `node tests/e2e/showroom-product-toolbar-layout.spec.js` -> PASS, 产品页紧凑工具栏与固定任务区布局保持稳定。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-task-banners-running-only run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260523-showroom-task-banners-visible-only-while-running\scripts\verify-showroom-task-banners-hidden-when-idle.mjs` -> PASS，真实登录 `测试租户(122) / aoteman / admin123` 后进入 `http://127.0.0.1:8081/showroom/product`，当前真实状态接口返回讲解任务 `active=false,running=false`、封面任务 `taskStatus=COMPLETED,startAllowed=true`，页面上两张任务卡片均未显示。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260523-showroom-task-banners-visible-only-while-running --mode preview` -> PASS，仅识别本任务 evidence 文档、一次性 Playwright 校验脚本与 green 截图为可删产物，无 blocked 项。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260523-showroom-task-banners-visible-only-while-running --mode apply` -> PASS，已删除本任务 evidence 文档、一次性 Playwright 校验脚本与 green 截图，仅保留 `task.md`、`execution-log.md`、正式代码和回归测试。
