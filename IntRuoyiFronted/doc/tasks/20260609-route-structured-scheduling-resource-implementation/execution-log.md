# 执行日志

BDD: 工艺路线下显示结构化排产资源表 -> Given 排产员打开工艺路线详情 / When 查看组成工序 / Then 表格显示资源类型、标准资源、今日可用、标准班次产能、今日班次产能和状态。

BDD: 点击工序排产资源查看详情 -> Given 某工序有设备或人工资源 / When 点击排产资源 / Then 设备工序显示设备明细和维修影响，人工工序显示单人产能、人数和班次小时。

BDD: 资源大表保留但不作为主入口 -> Given 用户进入工艺路线列表页 / When 切换视图 / Then 资源大表仍可打开，但工艺路线详情提供更清晰的结构化排产资源入口。

- PRECHECK: worktree -> PASS，前端工作区为 `D:\ProjectPackage\Int\IntRuoyi\worktrees\paichan_new\yudao-ui-admin-vue3`，分支 `codex/paichan_new`。
- PRECHECK: style -> PASS，已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，本任务按密集生产作业台风格实现。
- PRECHECK: scope -> PASS，前端只在现有工艺路线详情和资源大表定位上扩展，不新增重复排产系统。
- RED: `node tests\e2e\mes-route-structured-scheduling-resource-static.spec.js` -> FAIL，组成工序表格缺少 `资源类型` 等结构化排产资源列，API 类型缺少今日产能/维修状态字段。
- GREEN: `node tests\e2e\mes-route-structured-scheduling-resource-static.spec.js` -> PASS，工艺路线组成工序表格/API 类型满足结构化排产资源契约。
- GREEN: `node tests\e2e\mes-pro-route-process-shift-capacity-display.spec.js` -> PASS，班次产能展示契约更新为标准/今日双列。
- GREEN: `node tests\e2e\mes-pro-route-process-machinery-column.spec.js` -> PASS，标准资源/今日可用入口与设备详情复用契约通过。
- GREEN: `node tests\e2e\mes-pro-route-process-machinery-capacity-summary.spec.js` -> PASS，设备明细班次产能和底部汇总契约通过。
- CHECK: `pnpm ts:check` -> FAIL，Node 默认 4GB 堆内存不足，未发现业务类型错误输出。
- GREEN: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- GREEN: `node --check tests\e2e\mes-route-structured-scheduling-resource-real-flow.e2e.js` -> PASS。
- GREEN: `MES_ROUTE_RESOURCE_E2E_BASE_URL=http://127.0.0.1:8084 node tests\e2e\mes-route-structured-scheduling-resource-real-flow.e2e.js` -> PASS，真实登录 `芋道源码/admin`，打开路线 `900026`，验证结构化排产资源列、设备详情弹窗与人工产能弹窗；本次只读验证，未修改 admin 数据。
- GREEN: `MES_ROUTE_RESOURCE_E2E_BASE_URL=http://127.0.0.1:8084 node tests\e2e\mes-route-structured-scheduling-resource-real-flow.e2e.js` -> PASS，清理仓库内 runtime 日志并重启 48082/8084 后复测通过。
- REGRESSION: 静态契约组 + `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` + `MES_ROUTE_RESOURCE_E2E_BASE_URL=http://127.0.0.1:8084 node tests\e2e\mes-route-structured-scheduling-resource-real-flow.e2e.js` -> PASS，前端无降级显示收紧后复测通过。
- REGRESSION: 融入 `int_main` 后主目录前端 `8085` + 主目录后端 `48081` -> PASS，执行 `MES_ROUTE_RESOURCE_E2E_BASE_URL=http://127.0.0.1:8085 node tests\e2e\mes-route-structured-scheduling-resource-real-flow.e2e.js`，真实登录 `芋道源码/admin`，打开路线 `900026`，验证结构化排产资源列、设备详情弹窗和人工产能弹窗；本次只读验证，未修改 admin 数据，输出 `PASS: MES structured scheduling resource real UI E2E, worker=B080`。
