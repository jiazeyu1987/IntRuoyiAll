# 执行日志：合并并删除展厅路由集成旧测试

BDD: 展厅路由收口断言仍被覆盖 -> Given 展厅后台历史与讲解路由必须由统一后台壳承接 / When 运行展厅后台静态测试 / Then 测试仍验证 `history`、`narration-workbench` 路由使用 `showroomAdminView` 并渲染对应工作台。

BDD: 过期旧测试文件不再阻塞 -> Given `showroom-route-integration.test.mjs` 中存在过期产品加载策略断言 / When 合并有效断言后删除该文件 / Then 展厅后台定向静态测试通过且不再引用旧文件。

- PRECHECK: `doc/tasks/20260611-showroom-tab-permission-gate/task.md` -> PASS，前置任务已完成。
- PRECHECK: `rg -n "showroom-route-integration\\.test\\.mjs|showroom-route-integration" .` -> PASS，未发现 package 或脚本链路引用，只有历史任务文档和当前旧测试文件本身引用。
- RED: `node --test scripts\showroom-admin-frontend.test.mjs scripts\showroom-route-integration.test.mjs` -> FAIL，旧文件中 `showroom admin shell defers owner-company mapping lookup until product edit flows need it` 仍断言过期实现细节 `await Promise.all([loadProductRows(), loadHallRows()])`。
- CHANGE: 已将旧文件中仍有效的 `history` / `narration-workbench` 路由收口断言合并到 `scripts/showroom-admin-frontend.test.mjs`，并删除 `scripts/showroom-route-integration.test.mjs`。
- GREEN: `node --test scripts\showroom-admin-frontend.test.mjs` -> PASS，23 tests。
- GREEN: `rg -n "showroom-route-integration\\.test\\.mjs|showroom-route-integration" scripts package.json` -> PASS，未发现活动脚本引用。
- GREEN: `pnpm exec eslint scripts\showroom-admin-frontend.test.mjs` -> PASS。
- CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260611-showroom-route-integration-test-cleanup --mode preview` -> READY，delete `<none>`，blocked `<none>`，warnings `<none>`。
