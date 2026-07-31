# 执行日志：产品主数据页签重命名

## User Intent

- 用户要求：将产品主数据页签的名字改成展厅主数据。

## Preconditions

- 当前工作区：`E:\IntRuoyi`。
- 当前分支：`int_main`，预检显示本地已领先 `origin/int_main` 4 个提交，工作区无脏文件。
- 已读取规则：`docs/frontend-development.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- 已读取技能：`clear-frontend-copy`。
- 已读取经验索引：`docs/experience-index.md`。适用门禁已摘入 `task.md` 的 `## 经验门禁`。

## BDD / TDD Evidence

- BDD: 产品主数据页签改名 -> Given 用户进入产品主数据页面 / When 顶部页签或页面标题展示该入口名称 / Then 用户看到 `展厅主数据` 而不是 `产品主数据`。
- RED: `node tests/e2e/mdm-product-tab-title-static.spec.js` -> FAIL，expected reason：缺少 `IntRuoyiBackend/sql/mysql/20260728_rename_mdm_product_menu.sql`，页签重命名迁移尚未存在。
- GREEN: `node tests/e2e/mdm-product-tab-title-static.spec.js` -> PASS，页面标题、真实路径脚本和菜单迁移均识别 `展厅主数据`，并保留业务对象文案 `产品主数据`。

## Milestone Log

- M1 completed: `rg` 定位到页面标题来源 `IntRuoyiFronted/src/views/mdm/product/index.vue` 和正式动态菜单来源 `IntRuoyiBackend/sql/mysql/20260607_product_master_data.sql` 中的 `system_menu` 记录 `id=990201`、`permission='mdm:product:query'`。
- M2 completed: 新增 `IntRuoyiFronted/tests/e2e/mdm-product-tab-title-static.spec.js` 并先跑 RED。
- M3 completed: 新增 `IntRuoyiBackend/sql/mysql/20260728_rename_mdm_product_menu.sql`，更新前端页面标题和 MDM 真实路径/菜单设置脚本中的入口标签为 `展厅主数据`。
- M4 completed: 目标验证通过：
  - `node tests/e2e/mdm-product-tab-title-static.spec.js` -> PASS。
  - `node tests/e2e/mdm-tenant-package-real-setup-static.spec.js` -> PASS。
  - `node tests/e2e/mdm-real-data-prerequisite-guards-static.spec.js` -> PASS。
  - `node --check tests/e2e/mdm-product-real-setup.e2e.js; node --check tests/e2e/mdm-role-menu-real-setup.e2e.js; node --check tests/e2e/mdm-tenant-package-real-setup.e2e.js` -> PASS。
  - 聚焦文案扫描 `python C:\Users\BJB110\.codex\skills\clear-frontend-copy\scripts\scan_frontend_copy.py --root IntRuoyiFronted\src\views\mdm\product --format json` -> PASS，`garbled_text=0`；扫描器提示的 `DCC`、`ENABLE`、`DISABLE` 和插值文本为既有保留术语/代码值，不属于本次页签重命名范围。
  - 聚焦迁移门禁 `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql --sql-file sql\mysql\20260607_product_master_data.sql --sql-file sql\mysql\20260728_rename_mdm_product_menu.sql` -> PASS，`migrationCount=2`。
- Verification note: 全量前端文案扫描 120s 超时，未采用不完整结果；全量 MySQL migration policy gate 被既有 `20260725_mes_edhr_recordbook_global_setting.sql: config-seed` 阻塞，与本次新增 SQL 无关，已改用目标 SQL + 依赖迁移的正式聚焦门禁。
- Concurrency note: 任务过程中检测到并行提交使分支从 ahead 4 变为 ahead 7；`a8ad9591`、`27e64e76`、`b591e1bf` 已把本任务文档、静态契约和实现文件纳入 HEAD，同时混有其他任务记录。后续收尾仅暂存/提交本任务文档增量，避免触碰其他未提交文件。
- M5 completed: cleanup 与经验沉淀完成。
  - CLEANUP PREVIEW: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-rename-product-master-tab --mode preview` -> PASS，keep `task.md`、`execution-log.md`、`verification-report.md`，delete `<none>`，blocked `<none>`。
  - CLEANUP APPLY: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260728-rename-product-master-tab --mode apply` -> PASS，deleted_paths `<none>`。
  - EXPERIENCE: 已按 `project-experience-consolidation` 更新已有 `docs/frontend-development.md#动态菜单页签重命名门禁` 和 `docs/experience-index.md`；`rg -n "动态菜单页签重命名|system_menu\.name|20260728-rename-product-master-tab" docs\frontend-development.md docs\experience-index.md` -> PASS。
  - DOC CHECK: `git diff --check -- docs\frontend-development.md docs\experience-index.md doc\tasks\20260728-rename-product-master-tab` -> PASS，仅有 LF/CRLF 工作区提示，无 whitespace error。
