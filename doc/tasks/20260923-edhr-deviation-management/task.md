# eDHR 偏差管理开发文档任务

## Goal

在独立 worktree piancha_0923 按已确认 PRD、系统设计、前端交互和测试计划实施偏差管理，并完成逐阶段开发验证。

## Scope

- P1：偏差数据、编号、角色权限、未推送PQC时正式批记录身份。
- P2：唯一处理记录、全签名矩阵、修订版本和常规关闭。
- P3：标准列表、详情、正式批记录页签和追溯交互。
- P4：关键偏差转统一NCR、原子关闭和四项服务端拦截。
- P5：在线追溯和历史签名证据。
- P6：定向回归、迁移策略和交接。
- 只在 worktree piancha_0923 操作；不修改主工作区未提交改动。
- 不改原Office文件；E2E、数据库写入、服务启动/重启、Git提交/推送遵守当轮授权规则。

## Milestones

- [x] P1 正式身份、编号、数据与权限。
- [x] P2 唯一处理、签名及常规关闭。
- [x] P3 标准列表、详情和前端交互。
- [x] P4 关键偏差转NCR及四项控制。
- [x] P5 在线追溯与历史证据。
- [x] P6 定向验证与交接。

## Expected Verification

- 每阶段按development-plan.md先添加行为测试，实际RED后最小实现，再GREEN和相关回归。
- Maven、前端定向测试/ESLint/TS、迁移策略依具体阶段执行并记录真实执行数量与结果。
- 测试报告逐阶段由独立测试者形成；E2E仅在用户当轮明确授权后执行。
- 完成阶段验证后cleanup preview/apply并如实记录Git交付门禁。

## Current Status

blocked

## Worktree Setup

- Worktree: D:\IntRuoyiWorktree\piancha_0923
- Branch: codex/piancha-0923
- Baseline: f46b0bdc20d80ee7fa6e5d6b86898bcae0d0b357
- Runtime reservation: int_main slot 13, frontend 8094/backend 48094; backend health `UP`, frontend Vite active.
- 文档源自主工作区本地忽略目录并已复制到本worktree；后续执行以此处为事实源。

## Blockers

- 用户已授权按需启用执行、数据库SQL迁移和服务重启，并澄清偏差表中的批准方式统一为电子签名。偏差发起电子签名已接入创建事务并完成定向回归。
- GxP策略已按用户确认登记：版本 `20260924-edhr-deviation-01`，批准依据标识 `EDHR-DEVIATION-GXP-20260924`，owner角色 `ROLE_QA_QUALITY_OWNER`，`signaturePolicy=REQUIRED`，`retentionClass=GXP_BATCH_RECORD`；后续通过新策略版本配置修改，历史事件不重写。P1独立复核已通过。
- P1/P2/P4独立复核已通过；P3/P5前端静态合同、后端定向回归、前端 `vue-tsc` 和 ESLint 已通过。真实 Playwright 已完成任务专属批记录创建、偏差电子签名发起、唯一处理保存、五个普通偏差签名节点和常规关闭。
- 已完成关键偏差转不合格审批、待审冻结、QA让步放行和批记录追溯的真实页面验收；待执行收尾清理与最终状态同步。
- 偏差候选MyBatis查询已有H2真实Mapper回归；MySQL已验证迁移/Mapper级SQL/锁语义，不声称Spring完整Java事务在MySQL上的端到端竞态测试。
- 已按授权将 `20260924_mes_edhr_deviation_management.sql` 与 `20260924_mes_edhr_deviation_ncr.sql` 应用到本机 `int-ruoyi-mysql`，并完成表、权限、GxP策略和重复执行只读核对。
- Playwright 真实夹具已完成：通过产品名搜索找到启用路线工单，创建任务专属批记录 `P6-DEV-20260926-CAP4-577008`，批记录 ID `900000001150`，并完成偏差发起、处理、签名和关闭。

## 设计约束检查

- 两级偏差、常规完整签名、直接转审例外和四项门禁以PRD为唯一需求事实源。
- 不引入fallback/静默降级/假成功；身份缺失按正式来源解析，不由订单/批号猜测。
- 唯一处理正文版本和签名事实分离；转审关闭和验证合格关闭原因不可混淆。
- 只修改本任务worktree；不覆盖主工作区或并行任务文件。

## Deliverables

先读[产品需求](prd.md)、[前端交互](frontend-interaction.md)、[一致性审查](consistency-review.md)。
实施按[开发计划](development-plan.md)、[测试计划](test-plan.md)和[验收标准](acceptance-criteria.md)推进；前后端/数据/权限设计在同目录。
当前实测范围和限制见[验证报告](verification-report.md)。

## Cleanup Keep

- doc/tasks/20260923-edhr-deviation-management/task.md
- doc/tasks/20260923-edhr-deviation-management/execution-log.md
- doc/tasks/20260923-edhr-deviation-management/verification-report.md
- doc/tasks/20260923-edhr-deviation-management/prd.md
- doc/tasks/20260923-edhr-deviation-management/user-flows.md
- doc/tasks/20260923-edhr-deviation-management/acceptance-criteria.md
- doc/tasks/20260923-edhr-deviation-management/frontend-design.md
- doc/tasks/20260923-edhr-deviation-management/frontend-interaction.md
- doc/tasks/20260923-edhr-deviation-management/backend-api-design.md
- doc/tasks/20260923-edhr-deviation-management/data-model.md
- doc/tasks/20260923-edhr-deviation-management/config-security-deployment.md
- doc/tasks/20260923-edhr-deviation-management/development-plan.md
- doc/tasks/20260923-edhr-deviation-management/test-plan.md
- doc/tasks/20260923-edhr-deviation-management/test-report.md
- doc/tasks/20260923-edhr-deviation-management/consistency-review.md
- doc/tasks/20260923-edhr-deviation-management/task-state.json
- doc/tasks/20260923-edhr-deviation-management/validate-docs.py
- doc/tasks/20260923-edhr-deviation-management/bug-regression-evidence.md

## 2026-09-29 集成复核更正
历史手动创建批次的通过记录不证明取消该入口后的正式主链通过。当前融合与验证见 ../20260929-deviation-main-integration/verification-report.md。本轮不执行真实前端E2E；四项冻结完整主链、管理者代表缺签拒绝及多偏差同一NCR关联的完整页面证据仍须按正式来源重新核对，不宣称全链路全部通过。

当前阻塞仅指尚缺正式主链完整真实页面E2E和发布审计门禁；本地代码融合由20260929-deviation-main-integration记录，不把历史手动批次验收当作正式来源验收。旧策略登记段为历史记录；当前合入配置版本20260929-edhr-deviation-01，未执行数据库策略激活。
