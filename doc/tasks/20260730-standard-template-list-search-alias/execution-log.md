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
- Closeout blocker: 当前 `int_main` 为 `ahead 14, behind 8`，并有非本任务脏改动；本任务不执行提交/推送。

## Cleanup Applied 2026-07-30

- Cleanup preview: kept task.md, execution-log.md, verification-report.md, bug-regression-evidence.md, migration-policy-gate.json; delete target was output/playwright/20260730-standard-template-list-search-alias.
- Cleanup apply: PASS，已删除本任务 Playwright 调试截图目录；正式证据文件全部保留。
- Evidence validation: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260730-standard-template-list-search-alias\bug-regression-evidence.md` -> PASS。
- Final Git blocker: `int_main...origin/int_main [ahead 14, behind 8]`，且仍有非本任务脏改动；不提交、不推送。

## Experience Consolidation 2026-07-30

- 已按 project-experience-consolidation 检索现有经验归宿。
- 合并到 `docs/frontend-development.md#动态菜单页签重命名门禁`：入口改名兼容旧搜索词时，`RouterSearch` 需要别名并实时读取登录后的动态路由表，不能缓存 setup 阶段的 `router.getRoutes()`。
- 更新 `docs/experience-index.md` 关键词：`RouterSearch`、菜单搜索别名、`router.getRoutes`、动态路由快照、MES工序 搜索 标准模板列表。
- 验证：`rg -n "RouterSearch|动态路由快照|MES工序 搜索 标准模板列表" docs\experience-index.md docs\frontend-development.md` -> PASS。

## Final Real E2E 2026-07-30

- 运行态问题复现：旧 `48081` Jar `E:\IntRuoyi\output\runtime\int_main\backend-route-import-graph-fix-20260730-1815-v2.jar` 调用 `/admin-api/mes/pro/route-resource/page?pageNo=1&pageSize=20` 时返回业务码 `500` / `系统异常`，后端日志根因是 `IllegalStateException: Missing route: 922138`。
- 定向回归：主工作区 Maven 进程卡在 Windows 文件权限复制；按当前任务 PID `33116/44748` 精确停止，未停止并行 Maven 进程。
- 隔离构建：在 `D:\IntRuoyiWorktree\standard-template-e2e-runtime` 从 `fa55ac61` 建立干净 worktree，确认源码含 `filterResolvedRouteProducts(...)` 和 `getResourcePage_doesNotFailWholePageWhenRouteProductReferencesMissingRoute`。
- GREEN: `mvn.cmd -ntp -pl yudao-module-mes -am "-Dtest=MesProRouteResourceServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 3, Failures: 0, Errors: 0`。
- GREEN: `mvn.cmd -ntp -pl yudao-server -am "-DskipTests" package` -> PASS，生成 `D:\IntRuoyiWorktree\standard-template-e2e-runtime\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`。
- 运行态替换：复制隔离 Jar 为 `E:\IntRuoyi\output\runtime\int_main\backend-standard-template-e2e-20260730-2115.jar`，SHA256 `C79371D6B1DC445B94D7160BAB53827679DCC54E787ABF85C49FC60F8BE2C089`；确认旧 PID `50528` 属于 `int_main` 旧 Jar 后停止并启动新 PID `53040`。
- GREEN: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> `{"status":"UP"}`；前端 `http://127.0.0.1:8081/` -> HTTP `200`。
- E2E adjustment: Playwright 默认浏览器缓存缺少 headless shell，改用本机已安装 Chrome `C:\Program Files\Google\Chrome\Application\chrome.exe`；该调整只指定真实浏览器二进制，不切换前后端 URL、账号或数据源。
- E2E adjustment: 顶部搜索框真实 DOM 是 `input.el-select__input[role="combobox"]`，placeholder 文案由 Element Plus 外层展示；脚本改用真实可见 combobox 作为一线用户实际入口。
- GREEN: `node doc\tasks\20260730-standard-template-list-search-alias\standard-template-list-real.e2e.mjs` -> PASS；官方登录前置 `芋道源码/admin` PASS，搜索 `mes工序` 命中 `标准模板列表/mes/pro/mes-process`，进入 `/mes/pro/mes-process`。
- GREEN: E2E 断言 `/admin-api/mes/pro/route-resource/page?pageNo=1&pageSize=20` HTTP `200`、业务码 `0`、total `580`；列头包含 `产品名称/路线/序号/MES工序名称/MES工序编码/执行工序/设备/设备数量/10.5小时日产能/日产人力/工序单价/报工/批记录/批记录工序名称`；页面无“系统异常”，MES 写请求数 `0`，页面错误数 `0`。
- Evidence artifact: `E:\IntRuoyi\output\playwright\20260730-standard-template-list-search-alias\standard-template-list-evidence.json`。
- Evidence screenshot: `E:\IntRuoyi\output\playwright\20260730-standard-template-list-search-alias\standard-template-list-success.png`。
- Closeout blocker remains: 当前 `int_main` 有非本任务本地 ahead 提交；本次只完成 E2E 验证与证据记录，不提交/推送并行范围。
