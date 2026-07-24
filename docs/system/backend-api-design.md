# Codex 测试管理后端 API 设计

## Purpose and Scope

本设计定义系统管理测试管理能力的后端模块、API、错误模型、事务边界和执行协议。后端职责是维护测试项、创建执行批次、校验权限与租户、分发任务给外部 Codex Runner、接收 Playwright 执行结果、保存检查点结果和临时截图索引。

后端不把自然语言描述直接标记为通过，不用接口调用替代用户要求的真实 Playwright 页面路径，不在 Runner 不可用时自动改成人工执行或 API-only 执行。

## Evidence Reviewed

- `IntRuoyiBackend/yudao-module-system/src/main/java/.../RoleController.java`：系统模块 Controller、`CommonResult`、`@PreAuthorize` 模式。
- `IntRuoyiBackend/yudao-module-system/src/main/java/.../PermissionController.java`：角色菜单、用户角色分配接口模式。
- `IntRuoyiBackend/yudao-module-system/src/main/java/.../MenuController.java`：菜单查询与租户菜单过滤模式。
- `IntRuoyiBackend/yudao-module-system/src/main/java/.../RoleDO.java`、`MenuDO.java`：角色、菜单和权限字段定义。
- `IntRuoyiBackend/yudao-module-ai/src/main/java/.../CodexCliChatModel.java`：现有 Codex CLI 封装会 fail fast，但只适合聊天模型，不足以表达 Playwright 执行任务生命周期。
- `docs/database-rules.md`：菜单、权限、租户绑定需核对真实 schema，不能仅凭 DO 类名推断。
- `docs/e2e-rules.md`：真实 E2E 必须通过 Playwright 操作前端页面，API 只能用于最终核验或只读辅助。

## Modules

- 后端主模块：`yudao-module-system`，新增 `codextest` 或 `testmanagement` 包，原因是页面位于系统管理且依赖系统角色、菜单、租户和用户权限。
- Runner 集成边界：新增 `CodexTestRunnerService`，通过数据库任务领取和受控 API 与外部 Runner 交互；不复用 `CodexCliChatModel` 作为通过判定来源。
- 可选 AI 依赖：实现阶段可在 Runner 进程内部使用 Codex CLI；系统后端只接收结构化执行结果，避免后端线程直接控制浏览器。
- 前端 API wrapper：`IntRuoyiFronted/src/api/system/codexTestManagement/index.ts`。
- 菜单权限种子：新增一条 release migration，使用稳定业务键 `permission` 和 `role.code` 写入，不硬编码最终绑定关系。

## API Contracts

### 测试项管理

- `GET /admin-api/system/codex-test-case/page`
  - 权限：`system:codex-test:query`
  - 参数：`name`、`status`、`executionMode`、`pageNo`、`pageSize`
  - 返回：测试项分页、检查点数量、最近执行状态、最近执行时间

- `GET /admin-api/system/codex-test-case/get?id={id}`
  - 权限：`system:codex-test:query`
  - 返回：测试项详情和检查点数组

- `POST /admin-api/system/codex-test-case/create`
  - 权限：`system:codex-test:create`
  - 请求：`name`、`methodText`、`testDataText`、`defaultExecutionMode`、`parallelSafe`、`status`、`checkpoints[]`
  - 校验：自然语言测试方法不能为空；检查点数量必须大于 0；检查点期待结果不能为空

- `PUT /admin-api/system/codex-test-case/update`
  - 权限：`system:codex-test:update`
  - 请求：包含 `id` 的完整测试项快照
  - 事务：更新测试项并替换检查点排序；运行中的执行不受历史快照影响

- `DELETE /admin-api/system/codex-test-case/delete?id={id}`
  - 权限：`system:codex-test:delete`
  - 约束：存在 `RUNNING` 或 `CLAIMED` 的执行时拒绝删除；历史执行保留快照

### 执行管理

- `POST /admin-api/system/codex-test-execution/start`
  - 权限：`system:codex-test:execute`
  - 请求：`targetTenantId`、`executionMode`、`caseIds[]`
  - 规则：目标租户必须启用；Runner 必须在线；并行模式下所有测试项必须 `parallelSafe=true`
  - 返回：`executionId`

- `POST /admin-api/system/codex-test-execution/cancel`
  - 权限：`system:codex-test:cancel`
  - 请求：`executionId`
  - 行为：仅 `PENDING`、`CLAIMED`、`RUNNING` 可取消；Runner 下次心跳收到取消信号

- `GET /admin-api/system/codex-test-execution/page`
  - 权限：`system:codex-test:query`
  - 参数：`targetTenantId`、`status`、`createTime`
  - 返回：执行批次分页

- `GET /admin-api/system/codex-test-execution/get?id={id}`
  - 权限：`system:codex-test:query`
  - 返回：批次、测试项执行、检查点结果、截图 artifact 元信息

- `GET /admin-api/system/codex-test-execution/artifact?id={artifactId}`
  - 权限：`system:codex-test:artifact`
  - 返回：临时截图文件流；过期或缺失返回明确错误码

### Runner 协议

- `POST /admin-api/system/codex-test-runner/register`
  - 认证：Runner token，不使用普通用户 token
  - 请求：`runnerName`、`capabilities`、`maxParallelism`、`playwrightVersion`、`codexVersion`
  - 返回：`runnerSessionId`

- `POST /admin-api/system/codex-test-runner/claim`
  - 认证：Runner token
  - 请求：`runnerSessionId`、`capacity`
  - 返回：可执行的 case execution 任务，包含目标租户、自然语言方法、检查点快照、允许的 base URL、结果 schema

- `POST /admin-api/system/codex-test-runner/heartbeat`
  - 认证：Runner token
  - 请求：`runnerSessionId`、`runningExecutionCaseIds[]`
  - 返回：取消指令和服务器时间

- `POST /admin-api/system/codex-test-runner/checkpoint-result`
  - 认证：Runner token
  - 请求：`executionCaseId`、`checkpointSort`、`status`、`expectedText`、`actualText`、`mismatchDescription`、`screenshotArtifactId`
  - 约束：`FAIL` 必须带失败描述；需要截图时先上传 artifact

- `POST /admin-api/system/codex-test-runner/artifact`
  - 认证：Runner token
  - 请求：multipart 文件和 `executionCaseId`、`checkpointSort`、`artifactType`
  - 行为：后端保存至配置的临时目录并返回 `artifactId`

- `POST /admin-api/system/codex-test-runner/complete-case`
  - 认证：Runner token
  - 请求：`executionCaseId`、`status`、`summary`、`startedAt`、`finishedAt`
  - 汇总：所有检查点通过则 case 通过；任一失败则 case 失败；执行前置条件缺失则阻塞

## Error Model

- `CODEX_TEST_CASE_NOT_EXISTS`：测试项不存在或已删除。
- `CODEX_TEST_CASE_EMPTY_METHOD`：自然语言测试方法为空。
- `CODEX_TEST_CASE_EMPTY_CHECKPOINT`：检查点为空。
- `CODEX_TEST_TARGET_TENANT_INVALID`：目标租户不存在、禁用或过期。
- `CODEX_TEST_RUNNER_OFFLINE`：没有在线 Runner 或 Runner 心跳过期。
- `CODEX_TEST_RUNNER_CAPABILITY_MISSING`：Runner 缺少 Playwright、Codex 或目标浏览器能力。
- `CODEX_TEST_PARALLEL_UNSAFE_CASE`：并行执行包含未声明并行安全的测试项。
- `CODEX_TEST_EXECUTION_RUNNING`：测试项存在运行中执行，禁止删除或重复执行冲突范围。
- `CODEX_TEST_ARTIFACT_NOT_FOUND`：截图不存在、过期或已清理。
- `CODEX_TEST_RESULT_SCHEMA_INVALID`：Runner 回写结果不符合结构化契约。

所有错误通过业务错误码和明确 message 返回；后端不得把异常吞掉后返回空列表、默认通过或默认成功。

## Transactions and Idempotency

- 创建执行批次时，一个事务内写入 execution、execution_case 和 checkpoint_result 初始记录。
- 测试项更新不改写历史执行快照；执行时复制 `methodText`、`testDataText` 和检查点期待结果。
- Runner claim 使用原子状态迁移：`PENDING -> CLAIMED -> RUNNING`，避免两个 Runner 领取同一任务。
- Runner 回写检查点结果按 `executionCaseId + checkpointSort` 幂等更新；重复回写必须保持同一结果或返回冲突。
- 完成 case 时后端根据检查点状态汇总 case 状态；完成 batch 时根据全部 case 状态汇总批次状态。
- 取消执行时写入取消状态和取消人；Runner 收到取消后停止后续 Playwright 操作并回写已完成证据。

## Open Questions

- Runner token 由配置文件提供还是通过系统管理页面生成一次性密钥。
- 执行记录保留周期和截图临时文件保留周期是否相同。
- 是否需要为测试项提供导入/导出配置包能力，以便跨环境迁移。

## Design Blockers

- 实现前必须核对真实 `system_menu`、`system_role`、`system_user_role`、`system_role_menu`、`system_tenant_package` schema。
- 实现前必须确认 Runner 部署位置与后端临时目录写入权限；如果 Runner 与后端不在同一机器，必须使用 artifact 上传接口。
- 实现前必须确认测试账号凭据来源；后端和任务日志不得保存或输出明文密码。

