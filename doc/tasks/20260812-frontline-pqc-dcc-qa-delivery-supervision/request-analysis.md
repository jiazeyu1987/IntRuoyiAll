# User Goal

用户授权本次由主管 Agent 调度并完成 14 个任务：C00、DF01 至 DF11、INT12、VAL13。主管负责依赖波次调度、代码评审、验证门禁、fast-forward 合并到 int_main、清理已完成任务 worktree，并输出最终任务状态报告。

并发上限为最多 3 个工作子 Agent，不包含主管 Agent。当前平台容量为 4 个活跃 Agent，满足主管加 3 个工作子 Agent 的上限；若后续容量变化，主管必须先报告实际容量，不得静默降低并发。

授权范围包括创建任务分支和 D:\IntRuoyiWorktree\ 下的独立 worktree、修改本任务代码和测试、运行本地验证、创建本地提交、合并进 int_main，以及清理已完成任务 worktree。未授权 push、部署、远程服务器操作或修改共享业务数据。

# Current System

- 工作区是 E:\IntRuoyi，后端 IntRuoyiBackend 和前端 IntRuoyiFronted 与根目录同属一个 Git 仓库。
- 当前分支为 int_main；本地 int_main 与 origin/int_main 均指向 53f8ddc154a083193dbdfd3cc5a79fdd6fbf7b80。
- 设计放行证据存在且通过：.review-fix-loop/runs/20260812T001009Z-e59e06/run.json 记录 status=passed、final_decision=pass；复审报告 report-round-2.md 的 Final Decision 为 pass。
- 设计包 doc/tasks/20260811-frontline-pqc-dcc-qa-agent-design/ 已包含 common-background、architecture、interface-contracts、data-migration-contract、acceptance/system 文档，以及 14 个 agent-tasks 文档。
- D:\IntRuoyiWorktree\ 已存在；端口预留脚本 scripts\runtime\reserve-worktree-slot.ps1 和端口保护脚本 scripts\preflight\branch-runtime-port-guard.ps1 已存在。
- 当前主工作区存在大量既有未提交和未跟踪变更，包含 AGENTS.md、历史任务文档、review-fix-loop 运行目录、设计包目录和 docs/changes 文件。后续每个子任务创建分支、评审和合并前必须核对任务真实增量与这些既有变更的文件交集。
- 当前已有多个 worktree 位于 D:\IntRuoyiWorktree\；后续任务必须使用新的唯一目录，并在启动服务前通过端口预留脚本分配独立端口。
- 当前生产功能尚未按本设计包完成；设计任务只是通过放行评审，不能替代后续代码实现、测试和真实页面验证。

# Constraints

- 必须遵守工作区 AGENTS.md、触发规则、严格 no-fallback、BDD + 严格 TDD、PowerShell UTF-8、worktree、端口登记和 Git 操作边界。
- 所有任务必须按 Wave 0 至 Wave 9 调度；前置任务未通过主管评审、独立验证并合入 int_main 前，后继任务不得启动。
- 某个波次任务少于 3 个时，不得用后续波次任务补满并发。
- 每个实现任务必须从包含全部前置任务的最新 int_main 创建独立分支和 D:\IntRuoyiWorktree\ 下的独立 worktree。
- 启动任何本地服务前必须用 scripts\runtime\reserve-worktree-slot.ps1 原子预留端口，不得随机换端口。
- 工作子 Agent 只能修改对应任务文档明确归属的文件，不得自行合并 int_main，不得删除 worktree，不得修改主管状态文件。
- 主管必须亲自复核完整 diff、提交范围、任务文档、接口统一性、当前系统复用、过度设计风险和 RED/GREEN 真实性，并独立重跑验证。
- 合并必须 fast-forward；冲突、无法 fast-forward、半合并风险或不明并发修改必须停止报告。
- 禁止 push、部署、远程服务器操作、共享业务数据修改、fallback、吞异常、兼容分支、模拟成功、临时绕过、重复模型或设计文档未批准的抽象。

# Unknowns

- 当前大量未提交变更的归属需要主管在每个子任务前按文件交集判定，不能直接纳入任务提交。
- 端口登记表当前具体占用状态尚未在规划阶段读取；真正启动服务前必须以预留脚本结果为准。
- C00 的真实 schema preflight、历史数据阻塞清单、批准清单和测试数据是否齐备，必须由 C00 RED 阶段证明。
- 本地 Maven、pnpm、Playwright、数据库、登录测试账号和真实测试数据是否满足各任务验证，需要在对应任务启动时按触发规则确认。
- 每个实现任务最终精确文件清单要以对应 agent-task 文档和当前代码定位结果为准，主管不得用聊天记忆扩大写范围。

# Risks

- 主工作区已有大量并发变更，直接从脏工作区切分或合并可能带入无关文件。
- C00 涉及 schema、迁移和历史回填；若批准清单或测试库不足，后续 DF 任务必须阻塞，不能用 current QA 或路线推算补齐。
- DF01 至 DF11 跨后端、前端、数据库和真实页面，若并行任务误改共享 service、VO、mapper 或页面组件，会破坏后继 fast-forward 合并。
- 旧逻辑容易把 QA 与产品、路线版本或 MES 工序重新耦合；这会违背用户已确认的“QA 只对应 DCC 项目代码”规则。
- PATROL_AM 与 PATROL_PM 共用 inspectionType=PATROL 项目但必须是两条规则任务，按 inspectionType 去重会漏任务。
- 历史锁定版本允许 PUBLISHED/RETIRED 读取，若复用管理端 current 查询或 DCC 当前启用校验，会导致旧订单不可执行。
- 提交幂等需要任务行锁、CAS、hash 和唯一 event 共同保证；任何只靠前端防重复或吞并发失败的实现都不可接受。

# Validation Surface

- 启动门禁：AGENTS.md、触发规则、设计包、评审放行证据、Git 分支状态、worktree 列表、端口脚本和任务目录状态。
- 文档门禁：主管任务持续维护 task.md、execution-log.md、request-analysis.md、prd.md、dev-plan.md、test-plan.md、task-state.json、test-report.md。
- 单任务门禁：每个任务必须有 BDD、RED、GREEN、回归验证、任务记录、实现提交和主管复核记录。
- 后端验证：按任务运行 Java/JUnit、Maven 模块验证、schema/static tests、service/controller/mapper 合同测试和必要的真实 Bean 组合回归。
- 前端验证：按任务运行 TypeScript、direct node 静态合同测试、页面相关 Playwright 真实路径验证。
- 集成验证：DF01 至 DF11 全部合并后执行 INT12 全链路集成。
- 独立验收：INT12 合并后，由未参与实现的独立 Agent 执行 VAL13，只验收不修生产代码。
- 合并验证：每个任务合并前吸收最新 int_main 并完整重跑验证，最终只 fast-forward 合并。
- 最终验收：14 个任务状态明确，证据完整，INT12 和 VAL13 通过，代码合入 int_main，任务 worktree 清理完成，未 push、未部署。

# Blocking Prerequisites

当前规划阶段未发现阻止写入 request-analysis.md 和 prd.md 的缺失输入。进入实际开发前，主管必须通过以下门禁；任一失败即阻塞对应任务或整个波次：

- C00 启动前必须确认分支和 worktree 从最新 int_main 创建，并且与当前主工作区既有脏改动无未归属交集。
- C00 必须证明 schema preflight、迁移脚本、backfill/postflight/rollback 脚本、批准清单和验证命令可用。
- 启动任何本地服务前必须成功运行端口预留脚本；端口登记冲突或目录不在 D:\IntRuoyiWorktree\ 下必须阻塞。
- 真实页面或写类型 E2E 必须确认本地前后端、测试租户、测试账号和任务自有测试数据；缺任一项不得用 API-only、mock 或共享业务数据替代。
- 任何任务若需要修改未在其任务文档中归属的文件，必须退回主管重新划分所有权，不得自行扩权。
