# Codex 测试管理配置安全部署设计

## Purpose and Scope

本设计定义测试管理功能的配置项、密钥处理、权限控制、部署形态、运行安全和观测要求。核心目标是让系统按钮能触发外部 Codex Runner 使用 Playwright 真实执行，同时不泄露账号密码、不把 Runner 缺失伪装成成功、不用 API-only 替代用户路径。

## Evidence Reviewed

- `docs/login-access.md`：本机默认入口为 `http://localhost:8081`，凭据来源只记录来源，不复制密码。
- `docs/e2e-rules.md`：写入型 E2E 使用确认的测试租户和账号，失败记录页面状态、网络响应或控制台错误。
- `docs/powershell-preflight-lessons.md`：Playwright 前需确认依赖和浏览器 executable path，命令输出需要脱敏。
- `CodexCliChatModel.java`：现有 Codex CLI 调用会创建临时文件、设置超时并在失败时抛异常。
- `YudaoAiProperties.java`：已有 `yudao.ai.codex-cli` 配置命名空间，可作为 Runner 侧配置参考。

## Configuration

- `yudao.codex-test.runner.enabled`：是否允许后端接受 Runner 注册和领取任务。
- `yudao.codex-test.runner.heartbeat-timeout-seconds`：Runner 心跳超时阈值。
- `yudao.codex-test.runner.max-claim-size`：单次领取任务上限。
- `yudao.codex-test.artifact-temp-dir`：后端保存临时截图的根目录。
- `yudao.codex-test.artifact-retention-hours`：临时截图保留时长。
- `yudao.codex-test.allowed-base-url`：Runner 只能访问的前端入口，例如本机 `http://localhost:8081` 或授权测试环境。
- `yudao.codex-test.execution.default-timeout-minutes`：单个测试项执行超时。
- Runner 本地配置：Codex CLI 命令、Playwright 模块路径、浏览器 executable path、目标租户账号凭据映射、最大并行数。

## Secrets

- Runner token 必须通过环境变量、密钥文件或部署密钥注入，不能写入 Git、任务文档或前端配置。
- 测试租户账号密码由 Runner 本地凭据映射解析，后端只传 `targetTenantId` 和租户名称，不传明文密码给前端。
- 执行日志、失败原因、Runner stdout 和截图元信息必须脱敏 password、token、cookie、Authorization header。
- 前端不展示服务器绝对截图路径，只通过受权限保护的 artifact 接口读取。
- Codex prompt 中允许包含测试步骤、检查点和测试数据，但不得包含明文密码、token 或私钥。

## Permissions

- 新角色：`测试管理员`，稳定角色编码 `codex_test_admin`。
- 页面权限：`system:codex-test:query`。
- 操作权限：`create`、`update`、`delete`、`execute`、`cancel`、`artifact`。
- `admin` 赋权：通过 tenant 1 的启用 admin 用户和 `codex_test_admin` 角色 code 解析后写入 `system_user_role`。
- 角色菜单绑定：`codex_test_admin` 绑定测试管理菜单和所有测试管理按钮权限。
- 非测试管理员：菜单不可见，API 返回权限失败。
- Runner API：不使用普通用户会话，使用 Runner token 和最小可调用接口。

## Security Controls

- 执行发起前校验目标租户启用状态、当前用户权限、Runner 在线状态和并行安全。
- Runner 只能领取后端下发任务，不能自行选择任意测试项或任意租户。
- Runner 回写必须校验 `executionCaseId` 当前属于该 session 或处于允许回写状态。
- 截图上传限制 content type、大小、扩展名和任务归属。
- artifact 下载接口校验当前用户有 `system:codex-test:artifact` 权限且 artifact 未过期。
- Playwright 失败时必须保存失败位置、页面 URL、关键断言说明和截图；不能只返回“失败”。
- 结果判定必须来自真实页面观测和检查点比对；API 只能作为最终核验或只读辅助证据。

## Deployment

- 后端部署：随现有 `yudao-server` 提供测试项管理、执行管理和 Runner API。
- Runner 部署：独立本机或服务器进程，靠 Runner token 注册；建议与可访问目标前端的网络环境部署在同一受控机器。
- 本机开发：使用 `IntRuoyiFronted` 的 `http://localhost:8081` 和 `IntRuoyiBackend` 的 `http://127.0.0.1:48081`，执行前确认服务运行。
- 测试环境：只有用户明确授权测试服务器时才允许 Runner 指向远端；正式环境默认禁止执行写入型测试。
- 临时截图目录：由后端服务账号可写、可读，Runner 通过上传接口写入，不要求共享文件系统。
- 并行执行：后端按 `parallelSafe` 和 Runner `maxParallelism` 限制任务领取；同租户内不安全测试拒绝并行。

## Observability

- 后端记录执行批次、case、checkpoint 的状态变更事件。
- Runner 注册、领取、心跳、完成、失败均记录结构化日志。
- 每个失败 checkpoint 记录 mismatch 描述、截图 artifact、页面 URL、关键选择器或业务对象。
- 指标建议：执行批次数、通过率、失败率、阻塞率、平均耗时、Runner 在线数、截图清理数。
- 前端执行详情展示 Runner 名称、开始结束时间、耗时、失败摘要和每条检查点状态。
- 任务日志不得输出明文凭据；脱敏规则本身需要测试覆盖。

## Open Questions

- 是否需要在第一版提供 Runner 管理页面，还是只通过配置和只读在线状态展示。
- 是否需要支持多个前端 base URL；第一版建议一个执行批次只能选择一个受控 base URL。
- 是否需要把失败截图保留周期暴露给测试管理员配置。

## Design Blockers

- 没有可用 Runner token、Codex CLI、Playwright 模块或浏览器 executable path 时，执行入口必须阻塞。
- 没有目标租户登录凭据映射时，Runner 必须返回阻塞结果并说明缺失凭据标签。
- 未经当前任务授权，Runner 配置不得指向测试服务器、正式服务器或备份服务器。

