# 执行日志：修复产品主数据菜单运行态旧标题

## User Intent

- 用户反馈：截图中左侧菜单仍显示“产品主数据”，要求确认并修复为“展厅主数据”。

## Preconditions

- 当前工作区：`E:\IntRuoyi`。
- 本次任务只拥有 `doc/tasks/20260728-fix-product-menu-title-runtime/` 及本次明确新增的修复证据；当前工作区存在大量并行任务未提交改动，不得回滚、覆盖或混入。
- 已读取技能：`bug-regression-fix-loop`、`clear-frontend-copy`、`database-schema-delivery`。
- 已读取规则：`docs/database-rules.md`、`docs/frontend-development.md`、`docs/local-runtime.md`、`docs/login-access.md`、`docs/e2e-rules.md`、`docs/task-closeout-rules.md`。
- 本机运行态：`http://127.0.0.1:8081/` 返回 HTTP 200；`http://127.0.0.1:48081/actuator/health` -> `{"status":"UP"}`；后端本地配置连接 `int-ruoyi-mysql` 的 `127.0.0.1:23306/ruoyi-vue-pro`。

## BDD / TDD Evidence

- BDD: 动态菜单显示新名称 -> Given 用户登录本地系统并打开基础数据菜单 / When 查看产品主数据对应的左侧菜单入口 / Then 菜单显示 `展厅主数据`，不显示 `产品主数据`。
- RED: 数据库只读查询 -> FAIL，`system_menu.id=990201` 当前 `HEX(name)=E4BAA7E59381E4B8BBE695B0E68DAE`，即旧值 `产品主数据`。
- RED: 先应用原始迁移后复核 -> FAIL，`HEX(name)=C3A5C2B1E280A2C3A5C5BDE280A6C3A4C2B8C2BBC3A6E280A2C2B0C3A6C28DC2AE`，说明 MySQL 客户端字符集使中文字面量写成 mojibake。
- RED: `node tests/e2e/mdm-product-menu-runtime-encoding-static.spec.js` -> FAIL，expected reason：缺少 `20260728_fix_mdm_product_menu_utf8_name.sql`。
- GREEN: `node tests/e2e/mdm-product-menu-runtime-encoding-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mdm-product-tab-title-static.spec.js` -> PASS。
- GREEN: 聚焦 migration policy gate -> PASS，`migrationCount=3`，包含 `20260728_fix_mdm_product_menu_utf8_name`。
- GREEN: 本地数据库复核 -> PASS，`HEX(name)=E5B195E58E85E4B8BBE695B0E68DAE`，目标记录的 `permission/path/component/component_name/deleted` 不变。
- GREEN: Playwright 真实页面验证 -> PASS，展开“基础数据”后 `visibleNew=["展厅主数据","展厅主数据","展厅主数据"]`，`visibleOld=[]`。

## Milestone Log

- M1 completed: 截图旧文本位于动态左侧菜单，不是页面组件标题；本地运行库只读查询确认目标菜单仍为旧值。
- M2 completed: 新增 `IntRuoyiFronted/tests/e2e/mdm-product-menu-runtime-encoding-static.spec.js`，先 RED 锁定必须存在 UTF-8 hex 修复迁移。
- M3 completed: 新增 `IntRuoyiBackend/sql/mysql/20260728_fix_mdm_product_menu_utf8_name.sql`，使用 `CONVERT(UNHEX('E5B195E58E85E4B8BBE695B0E68DAE') USING utf8mb4)` 写入菜单名，避免客户端字符集导致中文乱码；已将该迁移应用到本地 `int-ruoyi-mysql`。
- M4 completed: 静态契约、目标迁移门禁、数据库 HEX 复核和真实 Playwright 左侧菜单验证均通过。
- M5 completed: `task-closeout-cleanup` preview/apply 已通过，仅删除本任务额外 evidence 文件；`project-experience-consolidation` 已新增中文菜单名称 ASCII 安全迁移门禁和索引路由；实现提交为 `1374f802`。当前工作区仍存在大量并行任务未提交改动，本任务提交必须使用明确 pathspec。

## Closeout Evidence

- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-fix-product-menu-title-runtime --mode preview` -> PASS，blocked/warnings 均为 `<none>`。
- Cleanup apply: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-fix-product-menu-title-runtime --mode apply` -> PASS，删除 `bug-regression-evidence.md` 和 `database-schema-evidence.md`。
- Experience consolidation: 已将 MySQL 客户端中文菜单名迁移乱码经验沉淀到 `docs/database-rules.md#中文菜单名称 ASCII 安全迁移门禁`，并在 `docs/experience-index.md` 增加 `system_menu.name 中文菜单名` 等关键词路由。
- Final verification: `node tests\e2e\mdm-product-menu-runtime-encoding-static.spec.js` -> PASS；`node tests\e2e\mdm-product-tab-title-static.spec.js` -> PASS；聚焦 migration policy gate -> PASS，`migrationCount=3`；本地数据库复核 `HEX(name)=E5B195E58E85E4B8BBE695B0E68DAE`；`git diff --check -- ...` -> PASS。
- Commit evidence: `git commit -m "fix: apply utf8-safe mdm product menu rename"` -> `1374f802`，仅包含 `20260728_fix_mdm_product_menu_utf8_name.sql` 和 `mdm-product-menu-runtime-encoding-static.spec.js`。
