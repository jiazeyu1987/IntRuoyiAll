# ERP 表格自动同步配置

## Task Goal

在个人工作台的配置页签下新增“ERP表格自动同步”配置入口，用于统一管理每天自动同步哪些 ERP/Kingdee 表格、几点开始执行，并复用现有正式 ERP 同步任务与运行记录链路。

## Milestones

- [x] M1：确认现有 ERP 同步类型、JobHandler、运行记录和 NAS 配置页复用边界。
- [x] M2：完成 BDD + TDD 设计文档，定义后端、数据库、前端和 E2E 验收路径。
- [x] M3：先写 RED 测试，覆盖配置保存、同步类型枚举、调度 Job、前端配置页入口与 API 契约。
- [ ] M4：实现数据库迁移、后端配置 API、自动调度 Job、前端配置页签。
- [ ] M5：运行定向 GREEN/REGRESSION 验证，记录验证证据和剩余阻塞。

## Expected Verification

- 后端：运行 ERP 模块合同测试，覆盖配置查询/保存、同步类型校验、调度 Job 执行所选 handler。
- 数据库：运行新增 SQL 静态测试，验证新增表、唯一键和 dispatcher job seed。
- 前端：运行目标静态契约，验证个人工作台配置页签、API wrapper、错误展示和权限边界。
- E2E：通过 Playwright 使用真实前端路径进入个人工作台配置页签，选择同步时间与 ERP 表格并保存，再用 API/页面读取结果核对。

## Current Status

in_progress

当前任务使用隔离验证 worktree `D:\IntRuoyiWorktree\profile-erp-table-auto-sync-verify`，分支 `codex/profile-erp-table-auto-sync`。原 worktree `D:\IntRuoyiWorktree\profile-erp-table-auto-sync` 在验证期间两次被外部进程删除；本任务不在主工作区直接修改文件。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺少正式同步类型、JobHandler、权限、配置或数据库 schema 时必须 fail fast。
- `是否从根因和长期维护角度解决`：是；复用现有 `ErpKingdeeSyncTypeEnum`、正式 JobHandler、运行记录和水位链路，不复制同步逻辑。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- 工作区已有并行脏改动时，本任务必须在独立 worktree 内修改和验证，禁止覆盖或回滚其它 Codex CLI 改动。
- 涉及个人工作台配置页和数据库迁移时，必须核对正式 schema、菜单/权限、前端入口、登录后权限响应，禁止用前端隐藏或默认成功掩盖缺表/缺权限。
- 需要真实 E2E 时必须走 Playwright 真实页面路径；API 只用于最终读回验证，不替代页面操作。
