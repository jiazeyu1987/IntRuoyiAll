# IntRuoyi Database And SQL Rules

## 触发场景

- 写 SQL、迁移、菜单、权限、租户绑定、schema 相关代码或数据修复脚本前，必须先读取本文件。
- 涉及真实数据库连接、远端数据库或发布数据变更时，还必须读取 `docs/server-access.md` 和 `docs/release-backup-restore.md`。

## Schema 核对

- 写 SQL 前必须用当前真实库或当前迁移文件核对表结构。
- 优先使用 `SHOW TABLES`、`DESCRIBE <table>`、已有 migration、mapper XML、现有 SQL 模板或测试夹具作为证据。
- 不得仅凭 DO 类名、字段猜测、历史记忆或旧项目文档编写运行 SQL。

### 数据修复临时表排序规则门禁

- Trigger: 数据修复、测试项种子、菜单/权限补齐等 SQL 使用临时表、字面量或用户变量与真实表字符列做 `JOIN`、`=`、`NOT EXISTS` 比较，尤其包含中文名称、权限字符串、表单名称、测试项名称。
- Preflight check: 写入前用 `information_schema.COLUMNS` 核对目标字符列 `COLLATION_NAME`；临时表字符串列必须声明与目标列一致的 `CHARACTER SET` 和 `COLLATE`，或在比较表达式上显式 `COLLATE` 到目标列排序规则。
- Blocker: MySQL 报 `ERROR 1267 Illegal mix of collations`，或发现临时字符串列与目标字符列排序规则不一致时必须停止并回滚当前事务。
- Verification: 重试前先确认失败事务未提交；修复后记录命中行数、目标行数、字段排序规则和关键文本扫描结果。
- Forbidden action: 禁止修改数据库默认排序规则、手改真实表排序规则、扩大 `WHERE` 范围、拆掉精确租户/删除标记条件，或把失败事务当作成功继续执行。
- Evidence: `doc/tasks/20260727-test-management-deterministic-closed-loop/execution-log.md`。

### 中文菜单名称 ASCII 安全迁移门禁

- Trigger: 菜单、权限、租户套餐或动态路由 SQL 需要写入中文入口名称，尤其通过 MySQL 客户端、Docker `mysql < file.sql`、PowerShell/stdin 或发布迁移执行 `system_menu.name` 更新。
- Preflight check: 中文目标值必须有 ASCII 安全写入方案，例如 `CONVERT(UNHEX('<utf8-hex>') USING utf8mb4)`，或先明确证明客户端连接已使用 `utf8mb4`；目标行必须用稳定主键加权限/路径等字段精确锁定。
- Blocker: 执行后 `HEX(name)` 不是预期 UTF-8、出现 mojibake/问号、目标行定位不唯一、只验证页面文案但未核对运行库 HEX，或 SQL 缺少 release migration 元数据时必须停止。
- Verification: 记录修复前旧值或乱码 HEX、修复后 `HEX(name)`、目标行 `permission/path/component/component_name/deleted` 不变、聚焦 migration policy gate，以及真实页面动态菜单不再显示旧名称。
- Forbidden action: 禁止用前端硬编码标题遮盖动态菜单旧值；禁止直接执行含中文字符串字面量的 SQL 后不复核 HEX；禁止扩大 `WHERE` 范围或改角色/租户绑定来掩盖菜单名未更新。
- Evidence: `doc/tasks/20260728-fix-product-menu-title-runtime/execution-log.md`。

### 测试管理 schema 迁移门禁

- Trigger: 访问 `系统管理 > 测试管理` 提示 `系统异常`，或修改/运行 `system_codex_test_case`、Codex Runner、测试项分页、测试管理页面相关接口。
- Preflight check: 先用当前真实库或迁移脚本核对 `system_codex_test_case.project`、`node_chain_name`、`node_chain_sort`、`node_chain_execution` 等当前 DO/Mapper 必需字段是否存在，并确认本地 Docker MySQL 已应用对应测试管理迁移。
- Blocker: 当前代码引用的字段在真实库缺失、迁移未应用、迁移测试失败，或只看到前端 toast 而缺少分页 API/DB schema 证据时必须停止。
- Verification: 记录 schema 核对结果、迁移执行目标、对应迁移契约测试结果，例如 `script/tests/test_codex_test_case_project_migration.py` 或 `script/tests/test_codex_test_node_chain_migration.py`，以及真实测试管理页面 E2E 不再出现 `系统异常`。
- Forbidden action: 禁止用前端隐藏错误、后端默认 project、吞掉数据库异常、切换数据源、mock 成功或 API-only 代替真实页面恢复来绕过缺字段。
- Evidence: `doc/tasks/fix-test-management-system-exception-20260726/verification-report.md`；`doc/tasks/20260727-codex-test-node-chain/database-schema-evidence.md`。

### 个人工作台隐藏任务状态迁移门禁

- Trigger: 个人中心、个人工作台、统一待办、`profile-workbench-task-visibility`、`hidden-keys`、页面提示 `待办任务加载失败` 或 `隐藏任务状态：系统异常`。
- Preflight check: 先只读核对当前后端连接库是否存在 `system_profile_workbench_task_visibility`，并确认 `sql/mysql/20260727_system_profile_workbench_task_visibility.sql` 与依赖 `20260708_system_user_table_column_config` 均通过 release migration policy gate。
- Blocker: 目标表缺失、迁移依赖未纳入门禁、迁移契约测试缺失、或只看到前端 alert 而没有接口/DB schema 证据时必须停止。
- Verification: 记录 `information_schema.tables` 表数量、目标列清单、迁移应用目标、`script/tests/test_system_profile_workbench_task_visibility_sql.py` 结果、migration policy gate 结果，以及个人工作台真实页面不再显示加载失败。
- Forbidden action: 禁止让前端忽略隐藏状态接口失败、后端返回空隐藏列表、吞掉 SQL 异常、mock 成功或跳过迁移来让待办列表看似恢复。
- Evidence: `doc/tasks/20260727-todo-task-hidden-status/verification-report.md`。

### 数据修复与写入型 E2E 恢复并发门禁

- Trigger: 数据修复目标同时可能被 Playwright、Codex Runner、`finally` 恢复逻辑或定时测试写入。
- Preflight check: 执行前检查目标测试进程和命令行，确认恢复逻辑已自然结束；删除前重新导出精确范围快照，并把行数、主键边界和全字段校验值绑定到事务断言。
- Blocker: 同范围写入型 E2E 正在运行、目标行数或校验值在快照后变化、或测试恢复逻辑可能重新插入目标数据时必须停止；不得强停不属于当前任务的并发进程。
- Verification: 事务提交后再次检查并发进程和目标范围稳定值；若 E2E 在修复后启动，必须等其恢复完成后复验最终行数和业务字段，不能只记录事务瞬时成功。
- Forbidden action: 禁止在外部恢复任务仍活跃时把一次删除成功宣称为最终完成；禁止扩大删除范围、循环强删或终止无归属依据的并发任务。
- Evidence: `doc/tasks/20260727-delete-duplicate-fill-rules/execution-log.md`。

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
