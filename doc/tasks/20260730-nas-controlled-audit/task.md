# NAS 受控状态统计与 Excel 报告

## Task Goal

在“NAS 管理”页面新增“统计未受控文件”能力。系统固定只读扫描 NAS 共享下的 `1. QMS documents`、`2.DHF`、`3.DMR` 三个目录，按系统范围内最新有效 `ACTIVE` 受控文件和标准化相对路径精确匹配，异步生成可下载的 `.xlsx` 报告。

子目录遇到 `ACCESS_DENIED` 时跳过该目录及其子树并记录；NAS 连接、共享、三个根目录或根目录访问失败时任务直接失败且不生成伪造报告。第一版不删除、移动、导入或修改 NAS 文件。

## Milestones

1. `completed` - 建立任务文档，核对代码边界、数据库 schema、权限、运行规则和现有 NAS 扫描实现。
2. `completed` - 设计并落地 NAS 来源映射、统计任务、统计结果和报告文件持久化结构。
3. `completed` - 在 `infra` 增加通用 SMB 单连接异步递归扫描能力，支持子目录权限跳过和根目录 fail-fast。
4. `completed` - 在 `dcc` 增加受控状态统计服务、任务状态 API、流式 Excel 报告生成和下载接口。
5. `completed` - 在 NAS 转移创建受控文件的同一事务中写入来源映射；历史来源只迁移精确 `NAS transfer source: <path>` 记录，无法唯一确认的记录进入待确认。
6. `completed` - 在 NAS 管理页面增加按钮、确认提示、任务轮询、真实失败原因展示、自动下载和重新下载入口。
7. `blocked` - 完成后端/Mapper/SQL/Excel、前端静态合同和定向回归验证；真实后端运行态当前被同仓重启构建卡住，无法执行真实 NAS 页面 E2E。
8. `pending` - 生成 verification-report，完成 evidence validator、经验沉淀、cleanup preview/apply、提交和推送。

## Expected Verification

- BDD/TDD 覆盖任务创建、异步状态、子目录无权限跳过、根目录/NAS 失败、精确路径匹配、冲突、来源缺失、报告数量一致性和 NAS 转移来源映射事务一致性。
- 后端运行受影响模块的目标 JUnit、Mapper/SQL/schema 静态或集成验证，以及 Excel 生成验证。
- 前端任务专用静态合同覆盖按钮、确认文案、轮询、下载、失败原因和权限条件；再运行前端类型检查或项目既有定向检查。
- 使用 `backend-api-delivery`、`database-schema-delivery`、`frontend-feature-delivery`、`quality-assurance-test-suite` evidence validator。
- 若本机服务、测试服务器、NAS 账号和测试租户前置条件均可核实，使用真实前端路径执行只读 NAS 验证；缺少任一前置条件时记录精确 blocker，不用 mock/API-only 冒充。
- 提交前执行 `git diff --check`、目标文件 staged 清单检查、分支推送并确认不再 ahead。

## Applicable Experience Gates

### PowerShell 分号串联测试退出码门禁

- Trigger: PowerShell 中串联运行 Maven、Node 静态合同、schema 验证或证据脚本。
- Preflight check: 每条验证命令单独执行，或显式检查 `$LASTEXITCODE`，不得用最后一个 PASS 掩盖前置失败。
- Blocker: 任一 RED/GREEN/REGRESSION 命令退出码非 0 且未记录首个失败原因。
- Verification: `execution-log.md` 记录实际命令、退出码、首个失败原因或 PASS 证据。
- Forbidden action: 用 `cmd1; cmd2` 的最后退出码代表整条验证链成功。
- Evidence: `docs/powershell-memory.md#powershell-分号串联测试退出码门禁`。

### 前端静态合同隔离门禁

- Trigger: 为 NAS 管理页面新增按钮、轮询、下载、失败提示等前端行为。
- Preflight check: 新增任务专用 `*-static.spec.js`，只读取本任务相关 Vue/API 文件。
- Blocker: 依赖无关历史静态合同失败证明本任务 RED/GREEN，或把类型检查历史 blocker 写成需求通过。
- Verification: 运行任务专用静态合同，并在回归阶段再运行必要的前端检查。
- Forbidden action: 修改大范围旧测试或用无关测试结果代替本任务验收。
- Evidence: `docs/frontend-development.md#前端静态契约隔离门禁`。

### E2E 脚本入口存在性门禁

- Trigger: 用户要求真实 E2E 或运行态验证。
- Preflight check: 确认真实前端入口、登录前置、测试账号、NAS 只读授权和 Playwright 浏览器可执行文件存在。
- Blocker: 缺少真实页面入口、测试服务器授权、登录态或 NAS 授权时，不得用 API-only 或静态合同冒充 E2E。
- Verification: 记录真实页面路径、任务编号、下载报告结果；缺前置时记录 blocker。
- Forbidden action: 将静态合同、API wrapper 或 mock 数据称为真实 E2E。
- Evidence: `docs/e2e-rules.md#e2e-脚本入口存在性门禁`。

### DCC 当前有效版本门禁

- Trigger: 统计“当前受控浏览”或最新 `ACTIVE` 受控文件。
- Preflight check: 同时核对 `dcc_controlled_file.status = ACTIVE` 与 `dcc_controlled_file_master.current_active_controlled_file_id`。
- Blocker: 仅按文件名、页面权限、浏览器黑名单或旧版本记录判断受控状态。
- Verification: 单元测试覆盖精确路径唯一命中、同路径多记录待确认、来源缺失。
- Forbidden action: 按文件名、扩展名、目录名猜测受控关系。
- Evidence: `docs/release-build-preflight-lessons.md#2026-07-19-controlled-content-生命周期迁移-obsolete_chain-门禁`。

## Design Constraints

- 是否引入 fallback/降级/吞异常：否。子目录 `ACCESS_DENIED` 是明确业务规则，不属于错误吞掉；根目录、共享、认证和网络失败必须显式失败。
- 是否从根因和长期维护角度解决：是。来源映射、通用扫描器、任务持久化和流式报告均作为正式链路实现。
- 是否存在临时补丁或绕过：否。不按文件名猜测，不使用浏览器筛选、黑名单、当前用户权限或默认成功值替代正式数据。

## Current Status

`blocked_on_runtime`

当前可验证部分：前端 NAS 统计按钮/确认/轮询/下载/失败提示静态合同通过，任务相关文件 `git diff --check` 通过。

当前阻塞：本机 `48081` 后端未监听；另一个同仓本地重启任务正在执行 `restart-int-ruoyi-local.ps1 -Component full`，其 Maven 主线程卡在 `WinNTFileSystem.delete0 / IncrementalBuildHelper.beforeRebuildExecution`。在不终止该并发任务、不清理共享 `target` 目录的前提下，无法完成后端 JUnit、后端加载和真实 Playwright NAS 管理 E2E。
