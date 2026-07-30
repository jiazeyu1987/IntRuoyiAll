# Execution Log

## User Intent

- 用户反馈：把“MES工序”改成“标准模板列表”后，前端全局搜索 `mes工序` 找不到页签，并要求修复之前阻塞本地后端启动的 block。

## BDD

- `BDD: MES工序旧关键词仍可发现标准模板列表 -> Given 动态菜单入口已按用户要求重命名为标准模板列表 / When 用户在前端搜索 mes工序 / Then 应能找到同一个标准模板列表入口且页面标题仍显示标准模板列表`
- `BDD: 后端本地启动不被过时独立目录测试阻塞 -> Given 当前方案复用已有工艺路线资源模型 / When 执行后端构建或标准本地后端重启 / Then 不应因不存在的独立 MES 工序目录包导致 testCompile 失败`

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
