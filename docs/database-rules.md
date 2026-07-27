# IntRuoyi Database And SQL Rules

## 触发场景

- 写 SQL、迁移、菜单、权限、租户绑定、schema 相关代码或数据修复脚本前，必须先读取本文件。
- 涉及真实数据库连接、远端数据库或发布数据变更时，还必须读取 `docs/server-access.md` 和 `docs/release-backup-restore.md`。

## Schema 核对

- 写 SQL 前必须用当前真实库或当前迁移文件核对表结构。
- 优先使用 `SHOW TABLES`、`DESCRIBE <table>`、已有 migration、mapper XML、现有 SQL 模板或测试夹具作为证据。
- 不得仅凭 DO 类名、字段猜测、历史记忆或旧项目文档编写运行 SQL。

### 测试管理 schema 迁移门禁

- Trigger: 访问 `系统管理 > 测试管理` 提示 `系统异常`，或修改/运行 `system_codex_test_case`、Codex Runner、测试项分页、测试管理页面相关接口。
- Preflight check: 先用当前真实库或迁移脚本核对 `system_codex_test_case.project` 等当前 DO/Mapper 必需字段是否存在，并确认本地 Docker MySQL 已应用对应 `sql/mysql/20260726_system_codex_test_case_project.sql` 迁移。
- Blocker: 当前代码引用的字段在真实库缺失、迁移未应用、迁移测试失败，或只看到前端 toast 而缺少分页 API/DB schema 证据时必须停止。
- Verification: 记录 schema 核对结果、迁移执行目标、`script/tests/test_codex_test_case_project_migration.py` 结果，以及真实测试管理页面 E2E 不再出现 `系统异常`。
- Forbidden action: 禁止用前端隐藏错误、后端默认 project、吞掉数据库异常、切换数据源、mock 成功或 API-only 代替真实页面恢复来绕过缺字段。
- Evidence: `doc/tasks/fix-test-management-system-exception-20260726/verification-report.md`。

### 个人工作台隐藏任务状态迁移门禁

- Trigger: 个人中心、个人工作台、统一待办、`profile-workbench-task-visibility`、`hidden-keys`、页面提示 `待办任务加载失败` 或 `隐藏任务状态：系统异常`。
- Preflight check: 先只读核对当前后端连接库是否存在 `system_profile_workbench_task_visibility`，并确认 `sql/mysql/20260727_system_profile_workbench_task_visibility.sql` 与依赖 `20260708_system_user_table_column_config` 均通过 release migration policy gate。
- Blocker: 目标表缺失、迁移依赖未纳入门禁、迁移契约测试缺失、或只看到前端 alert 而没有接口/DB schema 证据时必须停止。
- Verification: 记录 `information_schema.tables` 表数量、目标列清单、迁移应用目标、`script/tests/test_system_profile_workbench_task_visibility_sql.py` 结果、migration policy gate 结果，以及个人工作台真实页面不再显示加载失败。
- Forbidden action: 禁止让前端忽略隐藏状态接口失败、后端返回空隐藏列表、吞掉 SQL 异常、mock 成功或跳过迁移来让待办列表看似恢复。
- Evidence: `doc/tasks/20260727-todo-task-hidden-status/verification-report.md`。

## 租户和菜单权限

- 动态菜单页面交付必须同时核对：
  - 前端组件文件
  - `system_menu.path`
  - `system_menu.component`
  - `system_menu.component_name`
  - `system_menu.permission`
  - 目标租户角色菜单绑定
  - 登录后权限响应
- 写入型数据操作必须确认目标租户，不得污染生产租户、admin 基线数据或无关业务数据。

## 禁止做法

- 禁止 schema 缺证据时继续执行 SQL。
- 禁止用默认成功、空结果或 mock 数据掩盖缺表、缺字段、缺权限。
- 禁止未授权改远端数据库。
- 禁止把权限缺失误判为前端组件缺失。

## 验证方式

- 记录 schema 核对命令和关键字段证据。
- 记录 SQL 执行目标、租户范围、影响范围和回滚或清理方式。
- 执行后核对受影响行数、菜单权限响应或业务页面结果。
