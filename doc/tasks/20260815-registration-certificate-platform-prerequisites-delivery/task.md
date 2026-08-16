# 国内注册证平台前置能力并行交付

## Task Goal

在独立 worktree 中并行交付国内注册证后续开发所需、且不依赖未决业务口径的三个平台前置能力：System 受控内容生命周期扩展、Infra/DCC 业务文件全出口统一授权门禁、System 站内信业务键幂等与明确失败。主 Agent负责规划门禁、代码审查、独立测试、组合验证和融合到 `int_main`。

## Scope

- SP-01：扩展现有 System controlled-content core，使其可承载注册证类型/profile 和严格漂移阻断合同。
- SP-02：统一 Infra/DCC 文件访问授权，覆盖公共直链、通用预览、OnlyOffice/转换/打印/下载等出口。
- SP-03：扩展 System notify，使稳定业务键贯穿 API、消息落库和唯一约束；模板禁用或返回空 ID 必须失败。
- 本任务不实现注册证主档、版本、审批、提醒规则、迁移、页面或任何依赖 D-001..D-010 的业务行为。
- 经用户明确授权，允许为解除 TC-18 独立验证阻塞，最小修复 Infra runtime-control 超时进程未完整退出导致的 Windows 临时目录句柄竞态；不得顺带修复其余四个既有 Infra 基线失败。

## Milestones

- M1：完成真实代码基线、监督任务包、规划与拆分门禁。
- M2：创建 1 个集成 worktree 和 3 个执行 worktree，固定分支、路径和写入范围。
- M3：三个 executor 按 BDD + strict TDD 并行交付 SP-01/SP-02/SP-03。
- M4：主 Agent逐分支 review；独立 tester 逐任务验证，失败退回原 executor。
- M5：融合三个已通过分支，在集成 worktree 执行组合回归和安全门禁。
- M6：只有组合验证通过后才快进融合到 `int_main`，随后完成任务清理与剩余 blocker 记录。

## Expected Verification

- 每个子项目先有可发现且实际执行的 RED 测试，再有同命令 GREEN 和相邻回归。
- SP-01 证明现有 controlled-content core 被扩展而非另建平行生命周期。
- SP-02 证明所有列明服务端文件出口统一调用对象级 Guard，裸 `fileId`、跨租户/公司和无 grant 均被拒绝。
- SP-03 证明并发/崩溃重放只生成一条平台消息，禁用模板、缺参数和空消息 ID 明确失败。
- 独立 tester 不能修产品代码；主 Agent核对实际测试数、失败原因、文件范围和无 fallback。
- 集成分支完成组合回归、迁移合同和 branch runtime guard；`int_main` 融合前复核主工作区脏文件不被覆盖。

## Experience Gates

- `docs/backend-development.md#需求追踪必须校验语义而不是只校验编号门禁`：结构覆盖不等于语义正确。
- `docs/backend-development.md#受监管业务文件全出口授权门禁`：必须覆盖所有服务端出口，不能只补下载控制器。
- `docs/backend-development.md#站内信领域幂等必须延伸到平台消息门禁`：平台消息表必须持久化业务键并以唯一约束去重。
- `docs/worktree-memory.md#并行子-agent-控制权隔离门禁`：executor 只能修改各自 worktree 与写入范围；主 Agent独占集成和状态。

## Design Constraint Check

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；扩展现有平台核心与统一授权边界，不复制第二套平台。
- 是否存在临时补丁或绕过：否。

## Current Status

completed：本机 Docker TC-17 已由主 Agent和独立 tester 两次通过。`int_main` 的前次推进已无冲突保留到 integration `68578ad1c7b60a0228f12eccae55e345ff64b4ca`；主 Agent和独立非编写 Agent均在该精确提交通过验证。TC-20 原子预检确认 67 个任务路径、零冲突标记、两个运行门禁、祖先关系和主工作区脏文件零重叠后，以 `--ff-only` 成功融合。任务清理 preview/apply 仅删除临时端口释放脚本；5 个任务 worktree 已删除，端口登记 14/18/22/23 已释放，保留规划、执行和验证记录。

## Cleanup Keep

- doc/tasks/20260815-registration-certificate-platform-prerequisites-delivery/task.md
- doc/tasks/20260815-registration-certificate-platform-prerequisites-delivery/request-analysis.md
- doc/tasks/20260815-registration-certificate-platform-prerequisites-delivery/prd.md
- doc/tasks/20260815-registration-certificate-platform-prerequisites-delivery/dev-plan.md
- doc/tasks/20260815-registration-certificate-platform-prerequisites-delivery/test-plan.md
- doc/tasks/20260815-registration-certificate-platform-prerequisites-delivery/task-state.json
- doc/tasks/20260815-registration-certificate-platform-prerequisites-delivery/execution-log.md
- doc/tasks/20260815-registration-certificate-platform-prerequisites-delivery/test-report.md
- doc/tasks/20260815-registration-certificate-platform-prerequisites-delivery/verification-report.md
