# Codex 测试管理数据模型设计

## Purpose and Scope

本设计定义测试管理所需的数据表、关系、状态模型、迁移原则和完整性规则。数据模型必须支持用户手写自然语言测试方法、任意数量检查点、租户级执行、顺序和并行执行、Runner 领取任务、检查点结果、失败截图临时文件索引和历史执行快照。

测试项是可维护模板；执行记录是不可篡改的历史快照。用户后续修改工单号或期待结果时，只影响新执行，不改写已执行批次。

## Evidence Reviewed

- `RoleDO` 继承 `TenantBaseDO`，角色为租户内数据。
- `MenuDO` 使用 `@TenantIgnore`，菜单为全局资源，通过角色菜单绑定给租户角色。
- `system_tenant_package.menu_ids` 以 JSON 菜单 ID 数组控制租户套餐菜单。
- `20260615_system_config_package_menu.sql` 展示了系统管理菜单、按钮权限、租户套餐菜单合并和角色菜单绑定方式。
- `20260721_admin_full_scope_role_standardization.sql` 展示了通过稳定角色 code 给 tenant 1 admin 赋权的模式。
- `docs/release-build-preflight-lessons.md` 中权限菜单兼容门禁要求以 `permission` 稳定键校验菜单，避免只依赖偏好 ID。

## Entities

- `system_codex_test_case`
  - `id`
  - `name`
  - `method_text`：自然语言测试方法。
  - `test_data_text`：用户手写测试数据说明，例如工单号列表。
  - `default_execution_mode`：`SEQUENTIAL` 或 `PARALLEL`。
  - `parallel_safe`：是否允许参与并行执行。
  - `status`：启用或禁用。
  - `sort`
  - 审计字段、逻辑删除字段、`tenant_id`

- `system_codex_test_checkpoint`
  - `id`
  - `case_id`
  - `sort`
  - `name`
  - `expected_text`
  - `severity`：`INFO`、`MAJOR`、`CRITICAL`
  - `remark`
  - 审计字段、逻辑删除字段、`tenant_id`

- `system_codex_test_execution`
  - `id`
  - `target_tenant_id`
  - `execution_mode`
  - `status`
  - `requested_by`
  - `runner_session_id`
  - `started_at`
  - `finished_at`
  - `summary`
  - 审计字段、逻辑删除字段、`tenant_id`

- `system_codex_test_execution_case`
  - `id`
  - `execution_id`
  - `case_id`
  - `case_name_snapshot`
  - `method_text_snapshot`
  - `test_data_text_snapshot`
  - `checkpoint_count`
  - `status`
  - `runner_session_id`
  - `claim_time`
  - `started_at`
  - `finished_at`
  - `failure_reason`
  - 审计字段、逻辑删除字段、`tenant_id`

- `system_codex_test_checkpoint_result`
  - `id`
  - `execution_case_id`
  - `checkpoint_sort`
  - `checkpoint_name_snapshot`
  - `expected_text_snapshot`
  - `actual_text`
  - `status`
  - `mismatch_description`
  - `screenshot_artifact_id`
  - `completed_at`
  - 审计字段、逻辑删除字段、`tenant_id`

- `system_codex_test_artifact`
  - `id`
  - `execution_id`
  - `execution_case_id`
  - `checkpoint_result_id`
  - `artifact_type`：`FAILURE_SCREENSHOT`、`RUN_LOG`
  - `relative_temp_path`
  - `content_type`
  - `size_bytes`
  - `sha256`
  - `expires_at`
  - 审计字段、逻辑删除字段、`tenant_id`

- `system_codex_test_runner_session`
  - `id`
  - `runner_name`
  - `status`
  - `capabilities_json`
  - `max_parallelism`
  - `playwright_version`
  - `codex_version`
  - `last_heartbeat_time`
  - `current_running_count`
  - 审计字段、逻辑删除字段、`tenant_id`

## Relationships

- 一个测试项包含多个检查点：`system_codex_test_checkpoint.case_id -> system_codex_test_case.id`。
- 一个执行批次包含多个测试项执行：`system_codex_test_execution_case.execution_id -> system_codex_test_execution.id`。
- 一个测试项执行包含多个检查点结果：`system_codex_test_checkpoint_result.execution_case_id -> system_codex_test_execution_case.id`。
- 一个失败检查点结果可关联一个截图 artifact：`screenshot_artifact_id -> system_codex_test_artifact.id`。
- 一个执行批次绑定一个目标测试租户：`target_tenant_id -> system_tenant.id`。
- Runner session 可领取多个 execution case，但同一 execution case 同时只能被一个有效 session 持有。

## State Models

- 测试项状态：`ENABLE`、`DISABLE`。
- 执行批次状态：`PENDING`、`RUNNING`、`PASS`、`FAIL`、`BLOCKED`、`CANCELED`、`TIMEOUT`。
- 测试项执行状态：`PENDING`、`CLAIMED`、`RUNNING`、`PASS`、`FAIL`、`BLOCKED`、`CANCELED`、`TIMEOUT`。
- 检查点结果状态：`NOT_RUN`、`PASS`、`FAIL`、`BLOCKED`。
- Runner session 状态：`ONLINE`、`OFFLINE`、`DISABLED`。
- 合法流转：`PENDING -> CLAIMED -> RUNNING -> PASS|FAIL|BLOCKED|TIMEOUT`，取消可从 `PENDING|CLAIMED|RUNNING` 进入 `CANCELED`。

## Migration Notes

- 新增 SQL 必须带 `release-migration` 元数据头，类型按 schema、menu、permission 拆分或明确组合策略。
- 菜单创建以 `permission='system:codex-test:query'` 和组件路径为稳定业务键；如需使用偏好 ID，必须先校验无冲突。
- 角色创建以 `code='codex_test_admin'` 为稳定业务键，名称为 `测试管理员`。
- 给 `admin` 赋权时通过 `system_users.username='admin'`、`tenant_id=1`、`system_role.code='codex_test_admin'` 解析 ID，不写死用户角色关系 ID。
- `system_role_menu` 绑定通过查询实际菜单 ID 完成，不能只绑定偏好菜单 ID。
- `system_tenant_package.menu_ids` 更新前必须校验 JSON 有效；菜单 ID 合并后需要核对目标租户套餐包含测试管理菜单。
- 临时截图文件只保存相对路径，绝对根路径来自后端配置，不落库。

## Data Integrity Rules

- 测试项名称在同一 `tenant_id` 下唯一，逻辑删除数据不参与唯一约束。
- 检查点在同一测试项内按 `case_id + sort` 唯一。
- 执行快照字段不能为空；执行开始后不读取可变测试项作为断言来源。
- 并行执行时，所有 selected case 必须 `parallel_safe=true`，否则拒绝创建批次。
- `FAIL` 检查点必须有 `mismatch_description`；如果页面可截图，必须有关联截图 artifact。
- artifact 文件读取必须校验权限、执行归属和过期时间。
- Runner 心跳过期后，已领取未运行完成的任务进入 `TIMEOUT` 或释放策略必须有明确规则；第一版建议进入 `TIMEOUT` 并要求用户重新执行，避免重复真实页面操作。

## Open Questions

- 是否需要将测试项模板设为全局共享；第一版按登录租户隔离管理，执行时显式选择目标租户。
- 是否需要在测试项中保存变量结构化字段；第一版保留用户手写 `test_data_text`，由 Codex 从自然语言中理解。
- 是否需要执行记录归档表；第一版可通过保留周期和索引满足查询。

## Design Blockers

- 实现前必须核对目标数据库是否已有合适的菜单 ID 范围和角色分类 `menu`。
- 实现前必须确认 `system_tenant_package.menu_ids` 字段长度是否足以合并新增菜单；不足时需先设计 schema 扩容。
- 实现前必须确认临时目录清理任务不会删除正在查看的截图，且过期后 UI 能展示明确原因。

