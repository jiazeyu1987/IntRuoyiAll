# IntRuoyi E2E Rules

## 触发场景

- 编写、修改、运行或评审 Playwright E2E、真实用户路径验证、截图验收、登录后联调时，必须先读取本文件。
- 涉及登录、租户、账号时，还必须读取 `docs/login-access.md`。
- 涉及本机端口或 worktree 端口时，还必须读取 `docs/local-runtime.md` 和 `docs/worktree-restrictions.md`。

## 基本规则

- E2E 必须使用 Playwright 操作真实前端页面。
- API 只能用于最终状态核验或只读辅助检查，不得代替真实用户路径。
- 默认本机入口为 `http://localhost:8081` 或 `http://127.0.0.1:8081`。
- 写入型 E2E 必须使用已确认的测试租户和账号，并创建带任务标识、可追踪、可清理的数据。
- 只读验证必须说明使用的数据来源和只读范围。

## 缺入口处理

- 发布、审计或独立验证任务发现前端无入口时，必须 fail fast，不得临时扩大范围新增入口。
- 功能或修复任务只有在入口属于用户批准范围，且已完成 BDD + TDD 时，才允许补入口。

## 禁止做法

- 禁止 mock 数据冒充真实 E2E。
- 禁止 API-only 代替前端路径。
- 禁止直接 SQL 或接口直塞绕过页面。
- 禁止修改生产租户、admin 基线数据或无关真实业务记录。
- 禁止为了测试额外添加产品上不需要的前端控件。

## 验证方式

- 记录 Playwright 命令、入口 URL、租户/用户标签、目标页面和关键断言。
- 写入型 E2E 记录测试数据标识和清理方式。
- 失败时记录实际失败位置、页面状态、网络响应或控制台错误。

## 表格行定位

- 当页面对列表进行本地排序、过滤或虚拟渲染时，Playwright 必须按页面可见的业务唯一文本定位目标行，再操作同一行的复选框或按钮。
- 不得直接用 API 返回数组下标映射前端表格行；接口排序和页面排序可能不同，会误选冻结行、错误行或无关业务数据。
- Element Plus `el-table` 存在 header/body/fixed 表格重复 DOM 时，选择行复选框必须限定在可见 `.el-table__body-wrapper tbody tr`，显式排除 `.el-table__header-wrapper` 和 `thead`；点击后必须立即断言已选业务唯一键集合，再进入“确认/应用”等写入动作。

### Element Plus 表格选择门禁

- Trigger: Playwright 需要在 Element Plus `el-table` 中勾选行复选框、批量操作、手动重排、确认应用或其他写入型流程。
- Preflight check: 在写入动作前读取可见 body 行文本，断言已选业务唯一键集合与目标集合完全一致。
- Blocker: 若选中集合缺失目标行、包含额外行，或点击坐标落在 header checkbox / indeterminate checkbox 上，必须停止并修复定位逻辑。
- Verification: 保留真实 E2E 命令、选中集合断言、写入请求参数、最终 UI/API 状态和截图/JSON 证据路径。
- Forbidden action: 禁止用表头全选、数组下标、API-only、直接 SQL 或坐标猜测绕过可见业务行定位。
- Evidence: `doc/tasks/verify-manual-reschedule-881mo-20260724/execution-log.md`，2026-07-24 手动重排修复验证。

### Codex Runner 自动测试门禁

- Trigger: 新增、修改、运行或验收 `系统管理 > 测试管理`、Codex Runner、自然语言测试方法、检查点截图或由 Codex 调用 Playwright 的自动测试流程。
- Preflight check: 真实执行前必须确认本机前端/后端入口、目标测试租户、测试管理员账号、Runner token、Codex CLI、Playwright 浏览器、Runner 本地凭据映射和测试数据清理责任。
- Blocker: 任一 Runner 或租户前置条件缺失、测试项会写入生产/非任务租户、失败检查点没有差异描述、截图路径不在受控临时目录，或并行执行包含 `parallelSafe=false` 项时必须停止。
- Verification: 记录 Runner 注册/领取/心跳/回写命令、页面执行入口、租户/用户标签、检查点结果、失败截图 artifact、最终 UI 状态和必要的只读 API 核验。
- Forbidden action: 禁止把 API-only、静态合同测试、mock 截图、默认成功、Runner 离线跳过或顺序执行降级当作真实 E2E 通过。
- Evidence: `doc/tasks/20260724-codex-test-management-delivery/verification-report.md`，2026-07-24 Codex 测试管理交付。
