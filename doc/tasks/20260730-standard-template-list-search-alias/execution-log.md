# Execution Log

## User Intent

- 用户反馈：把“MES工序”改成“标准模板列表”后，前端全局搜索 `mes工序` 找不到页签，并要求修复之前阻塞本地后端启动的 block。

## BDD

- `BDD: MES工序旧关键词仍可发现标准模板列表 -> Given 动态菜单入口已按用户要求重命名为标准模板列表 / When 用户在前端搜索 mes工序 / Then 应能找到同一个标准模板列表入口且页面标题仍显示标准模板列表`
- `BDD: 后端本地启动不被过时独立目录测试阻塞 -> Given 当前方案复用已有工艺路线资源模型 / When 执行后端构建或标准本地后端重启 / Then 不应因不存在的独立 MES 工序目录包导致 testCompile 失败`
- `BDD: 登录后动态菜单也能参与全局搜索 -> Given admin 登录后动态路由才加入 Vue Router / When 用户在顶部搜索框输入 mes工序 / Then 搜索组件必须读取最新路由表并展示 标准模板列表/mes/pro/mes-process`

## Evidence

- Skill loaded: `bug-regression-fix-loop`
- Trigger docs read: `docs/frontend-development.md`, `docs/backend-development.md`, `docs/database-rules.md`, `docs/local-runtime.md`, `docs/task-closeout-rules.md`, `docs/powershell-encoding.md`, `docs/worktree-restrictions.md`
- Bug contract loaded: `C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\references\bug-contract.md`
- Experience index read: `docs\experience-index.md`; applied dynamic menu rename, Chinese menu HEX, static contract isolation, local Maven blocker gates.

## RED

- `node tests\e2e\mes-pro-mes-process-readonly-static.spec.js` -> FAIL，预期原因：搜索组件缺少 `ROUTER_SEARCH_ALIASES`，旧关键词 `mes工序` 不能匹配改名后的“标准模板列表”。
- `mvn.cmd -pl yudao-module-mes -am "-DskipTests" test-compile` -> FAIL，预期原因：过时测试 `MesProMesProcessCatalogSchemaTest` 引用不存在的 `cn.iocoder.yudao.module.mes.dal.dataobject.pro.mesprocess` 包。

## GREEN

- `node tests\e2e\mes-pro-mes-process-readonly-static.spec.js` -> PASS。
- `mvn.cmd -pl yudao-module-mes -am "-DskipTests" test-compile` -> PASS。
- `node tests\e2e\mes-pro-route-resource-orphan-static.spec.js` -> PASS。
- `node tests\e2e\mes-route-mes-process-tab-static.spec.js` -> PASS。
- `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file IntRuoyiBackend\sql\mysql\20260512_mes_base_schema.sql --sql-file IntRuoyiBackend\sql\mysql\20260709_mes_route_process_flow_graph.sql --sql-file IntRuoyiBackend\sql\mysql\20260709_mes_route_flow_config_unification.sql --sql-file IntRuoyiBackend\sql\mysql\20260730_mes_process_readonly_catalog_menu.sql --output doc\tasks\20260730-standard-template-list-search-alias\migration-policy-gate.json` -> PASS。
- `pnpm ts:check` -> PASS。
- 真实只读页面验证：本机 Chrome + Playwright，`芋道源码/admin` 登录后在顶部菜单搜索输入 `mes工序`，结果包含 `标准模板列表/mes/pro/mes-process`，未出现 `系统异常`。

## Blockers

- Closeout blocker: 当前分支被并行任务推进到 `ahead 8`，且剩余脏改动为非本任务文件；本任务不执行提交/推送，避免混入并行任务内容。

## Reopen 2026-07-30

- 用户反馈：`芋道源码/admin` 运行态仍然搜索不到 `mes工序`。
- 初步定位：`RouterSearch` 在组件 setup 阶段执行 `const routers = router.getRoutes()`，后续动态菜单路由加入后不会刷新该常量，导致真实登录态可能继续搜索旧路由快照。

## Reopened RED / GREEN

- RED: `node tests\e2e\mes-pro-mes-process-readonly-static.spec.js` -> FAIL，命中 `菜单搜索不得在 setup 初始化阶段缓存静态路由表`。
- Fix: `IntRuoyiFronted\src\components\RouterSearch\index.vue` 删除静态 `routers` 常量，新增 `getSearchRoutes()`，在过滤选项和历史路径解析时实时读取 `router.getRoutes()`。
- GREEN: `node tests\e2e\mes-pro-mes-process-readonly-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-route-mes-process-tab-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: 真实只读 Playwright 路径，`芋道源码/admin` 登录后搜索 `mes工序`，结果包含 `标准模板列表/mes/pro/mes-process`；MES 写请求数 `0`。
- Runtime note: 真实页面有一条既有头像资源 502，URL 为 `http://test.yudao.iocoder.cn/user/avatar/20251220/blob_1766215463801.jpg`，与本次搜索和 MES 接口无关。
- Closeout blocker: 当前 `int_main` 为 `ahead 10, behind 8`，并有非本任务脏改动；本任务不执行提交/推送。
