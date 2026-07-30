# NAS 受控状态统计与 Excel 报告

## Task Goal

在“NAS 管理”页面新增“统计未受控文件”能力。系统固定只读扫描 NAS 共享下的 `1. QMS documents`、`2.DHF`、`3.DMR` 三个目录，按系统范围内最新有效 `ACTIVE` 受控文件和标准化相对路径精确匹配，异步生成可下载的 `.xlsx` 报告。

子目录遇到 `ACCESS_DENIED` 时跳过该目录及其子树并记录；NAS 连接、共享、三个根目录或根目录访问失败时任务直接失败且不生成伪造报告。第一版不删除、移动、导入或修改 NAS 文件。

## Milestones

1. `in_progress` - 建立任务文档，核对代码边界、数据库 schema、权限、运行规则和现有 NAS 扫描实现。
2. `pending` - 设计并落地 NAS 来源映射、统计任务、统计结果和报告文件持久化结构。
3. `pending` - 在 `infra` 增加通用 SMB 单连接异步递归扫描能力，支持子目录权限跳过和根目录 fail-fast。
4. `pending` - 在 `dcc` 增加受控状态统计服务、任务状态 API、流式 Excel 报告生成和下载接口。
5. `pending` - 在 NAS 转移创建受控文件的同一事务中写入来源映射；历史来源只迁移精确 `NAS transfer source: <path>` 记录，无法唯一确认的记录进入待确认。
6. `pending` - 在 NAS 管理页面增加按钮、确认提示、任务轮询、真实失败原因展示、自动下载和重新下载入口。
7. `pending` - 完成后端/Mapper/SQL/Excel、前端静态合同和定向回归验证；具备运行态与授权后执行一次真实 NAS 只读验证。
8. `pending` - 生成 verification-report，完成 evidence validator、经验沉淀、cleanup preview/apply、提交和推送。

## Expected Verification

- BDD/TDD 覆盖任务创建、异步状态、子目录无权限跳过、根目录/NAS 失败、精确路径匹配、冲突、来源缺失、报告数量一致性和 NAS 转移来源映射事务一致性。
- 后端运行受影响模块的目标 JUnit、Mapper/SQL/schema 静态或集成验证，以及 Excel 生成验证。
- 前端任务专用静态合同覆盖按钮、确认文案、轮询、下载、失败原因和权限条件；再运行前端类型检查或项目既有定向检查。
- 使用 `backend-api-delivery`、`database-schema-delivery`、`frontend-feature-delivery`、`quality-assurance-test-suite` evidence validator。
- 若本机服务、测试服务器、NAS 账号和测试租户前置条件均可核实，使用真实前端路径执行只读 NAS 验证；缺少任一前置条件时记录精确 blocker，不用 mock/API-only 冒充。
- 提交前执行 `git diff --check`、目标文件 staged 清单检查、分支推送并确认不再 ahead。

## Design Constraints

- 是否引入 fallback/降级/吞异常：否。子目录 `ACCESS_DENIED` 是明确业务规则，不属于错误吞掉；根目录、共享、认证和网络失败必须显式失败。
- 是否从根因和长期维护角度解决：是。来源映射、通用扫描器、任务持久化和流式报告均作为正式链路实现。
- 是否存在临时补丁或绕过：否。不按文件名猜测，不使用浏览器筛选、黑名单、当前用户权限或默认成功值替代正式数据。

## Current Status

`in_progress`

