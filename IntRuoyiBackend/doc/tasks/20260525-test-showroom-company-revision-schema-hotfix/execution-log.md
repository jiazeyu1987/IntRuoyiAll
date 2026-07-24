# 执行日志：修复测试服展厅公司版本表缺列

BDD: 测试服展厅公司版本查询不再缺列 -> Given 测试服已部署引用 `display_name_snapshot` 的后端代码, When 查询或打开展厅公司版本相关页面, Then `showroom_company_revision` 应包含 `display_name_snapshot`、`display_name_en_snapshot`、`company_type_snapshot`，查询不再报 Unknown column。

INFO: 已采用 `bug-regression-fix-loop` 与 `database-schema-delivery` 工作流。
INFO: 已确认既有迁移脚本 `sql/showroom/20260523_showroom_version_center_schema.sql` 包含目标 3 个列。

RED: 测试服 `information_schema.columns` 只读检查 -> FAIL, `showroom_company_revision` 中 `display_name_snapshot`、`display_name_en_snapshot`、`company_type_snapshot` 命中数为 `0`，已部署后端查询字段缺失。

RED: 测试服直接查询 `SELECT id, display_name_snapshot FROM showroom_company_revision WHERE deleted = b'0' LIMIT 1;` -> FAIL, MySQL 返回 `ERROR 1054 (42S22): Unknown column 'display_name_snapshot' in 'field list'`。

GREEN: 测试服执行最小 schema DDL -> PASS, 仅新增 3 个 nullable 快照列，并补建缺失的 `showroom_version_bundle` 空表；未执行 backfill、未修改业务数据。

GREEN: 测试服 schema 复核 -> PASS, `target_columns_present = 3`；列顺序为 `honors_awards_en` 后依次 `display_name_snapshot`、`display_name_en_snapshot`、`company_type_snapshot`；`showroom_version_bundle` 表存在。

GREEN: 测试服直接查询 `display_name_snapshot` -> PASS, `SELECT id, display_name_snapshot, display_name_en_snapshot, company_type_snapshot ... LIMIT 1` 返回记录且不再报 `ERROR 1054`。

GREEN: API 验证 `POST /admin-api/system/auth/login` + `GET /admin-api/showroom/company/current`，tenant-id `122`，账号 `aoteman/admin123` -> PASS, 登录成功且公司当前版本接口 `code=0`，响应不包含 `Unknown column`。

GREEN: API 验证 `GET /admin-api/showroom/version-center/detail?targetType=COMPANY&targetId=1&revisionId=8`，tenant-id `122` -> PASS for bug regression, 响应不再包含 `Unknown column` 或 `display_name_snapshot` SQL 语法错误；当前返回 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND: COMPANY:1:8`，原因是本任务按要求未执行 readable bundle backfill。

GREEN: Playwright 真实前端 `http://172.30.30.58:8081/login?redirect=%2Fshowroom%2Fcompany`，默认租户 `芋道源码`，账号 `admin/admin123` -> PASS, 登录后进入 `http://172.30.30.58:8081/showroom/company`，`/admin-api/showroom/company/current` 返回 HTTP 200 且 `code=0`，页面和展厅接口响应均未出现 `Unknown column`。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260525-test-showroom-company-revision-schema-hotfix/database-schema-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260525-test-showroom-company-revision-schema-hotfix/bug-regression-evidence.md` -> PASS。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260525-test-showroom-company-revision-schema-hotfix --mode preview` -> PASS, keep `task.md`、`execution-log.md`、`database-schema-evidence.md`、`bug-regression-evidence.md`，delete 一次性 helper `verify-test-showroom-page.mjs`，blocked/warnings 均为 none。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260525-test-showroom-company-revision-schema-hotfix --mode apply` -> PASS, 已删除一次性 helper `verify-test-showroom-page.mjs`，blocked/warnings 均为 none。

INFO: 后续非本任务范围 blocker：版本中心 readable bundle 未回填会继续导致 `SHOWROOM_VERSION_CENTER_NOT_READY` 或 `SHOWROOM_VERSION_BUNDLE_NOT_FOUND`，需另行批准数据回填后处理。
