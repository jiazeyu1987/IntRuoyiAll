# 不合格评审 MVP 实现任务

## Task Goal

在新的 worktree 中按已确认文档实现并验证不合格评审 MVP：PQC 提交记录与 PQC 生产放行的 `不合格审查` 复用统一不合格评审入口、同一类评审单、同一套 QA 处置流程和最小状态机。

## Milestones

- [x] 创建新 worktree 与任务记录。
- [x] 读取经验索引并补充适用门禁。
- [x] 梳理现有前后端批记录、PQC 放行、工单/批次状态和追溯代码边界。
- [x] 先补最小 BDD/RED 测试或静态契约。
- [x] 实现后端统一评审单、状态机、冻结拦截和追溯读模型。
- [x] 实现前端统一入口、QA 处置页面和冻结拦截提示。
- [x] 运行定向后端、前端和产品流程验证。
- [x] 使用当前 `1..100` worktree 槽位规则完成真实 Playwright E2E。
- [x] 在最新 `int_main` 基线上完成语义融合与全量类型/编译复验。
- [ ] 完成 fast-forward、cleanup 和最终收尾。

## Expected Verification

- 后端定向测试覆盖统一入口创建评审单、冻结工单、三项冻结拦截、QA 让步放行/返工/作废状态流转和追溯字段。
- 前端静态契约或组件测试覆盖两个入口复用统一不合格评审入口、QA 处置必填字段、三项冻结拦截提示和三类追溯展示。
- 运行相关 Maven 定向测试、前端 Node/Vitest/TypeScript 静态验证。
- 如本地运行态和账号前置条件具备，使用 Playwright 验证真实前端路径；缺少前置条件时按 fail-fast 记录具体 blocker，不用 API-only 冒充真实 E2E。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是建立统一不合格评审公共流程，避免 PQC 提交和 PQC 生产放行形成两套审批实现。
- `是否存在临时补丁或绕过`：是，仅限测试租户模拟数据恢复。用户明确允许测试租户模拟数据；旧运行包把本任务评审批次从 `15` 错误覆盖为 `20` 后，按租户、批次、评审 ID、待处理状态和冻结前状态精确恢复 1 行，再由真实页面继续处置。生产代码没有保留 fallback，最终两个模拟批次均为作废终态、待处理评审为 0。

## Worktree Setup

- Worktree path: `D:\IntRuoyiWorktree\nonconformance-review-mvp-20260830`
- Branch: `codex/nonconformance-review-mvp-20260830`
- Runtime profile: `int_batch`
- Integration worktree: `D:\IntRuoyiWorktree\nonconformance-review-mvp-int-main-20260830`
- Integration branch: `codex/nonconformance-review-mvp-int-main-20260830`
- Integration runtime profile: `int_main slot 54`（前端 `8309`、后端 `48309`，本轮仅用于端口门禁，未启动服务）

## Experience Gate Summary

- 已读取 `docs/experience-index.md`。
- 命中通用前后端、数据库、eDHR 批次执行、冻结/作废、真实 E2E、PowerShell/Git 和 worktree 端口门禁。
- 本任务必须避免第二套不合格审批入口、默认成功、静默吞异常、API-only 冒充真实前端路径。
- 启动本地前后端前必须完成 `reserve-worktree-slot.ps1` 槽位登记。
- E2E 写入前默认只读确认测试租户存在正式可选工单/路线组合；数量为 `0` 时不得自行复用其它任务批次或直接造数。本轮用户随后明确允许测试租户模拟数据，因此只读筛选无活跃人工任务、无既有评审的历史 E2E 样本，并最终作废收口。
- 菜单迁移必须用迁移 ID 声明 `dependsOn`，并在执行前检查固定菜单 ID/path/permission 冲突。
- 批次任务状态重算必须把 `FROZEN` 作为业务权威状态显式保留，详情加载和同步不得覆盖冻结。

## Current Blockers

- 无功能或验证 blocker；剩余步骤仅为将已验证的融合提交 fast-forward 到 `int_main`，然后清理本任务 worktree。

## Implementation Summary

- 后端新增 `mes_pro_edhr_nonconformance_review` 最小评审单表、DO、Mapper、Service、Controller。
- 后端状态机：创建评审单后批次 `normal -> frozen(15)`；QA 让步放行/返工关闭后恢复冻结前状态；QA 作废关闭后批次进入 `voided(60)`。
- 冻结拦截：eDHR 批次任务操作、生产报工、PQC提交、PQC放行统一调用 `MesProEdhrNonconformanceReviewService` 的冻结判断。
- 前端入口：PQC 填写页、放行列表、批次详情放行区均进入 `MesProFeedbackEdhrNonconformanceReview`。
- 前端处置页：展示 `QA冻结批次列表`，必填评审材料、评审意见、QA签名，提供让步放行/返工/作废三个按钮。
- 追溯详情页：展示不合格原因、评审材料、评审意见、QA签名、处置结论、冻结/解冻/作废时间。
- 经验沉淀：已将 linked worktree closeout preview 因缺少主线 checked-out worktree 阻塞的门禁补入 `docs\worktree-memory.md`，并更新 `docs\experience-index.md` 路由。
- E2E 前置修复：迁移 `dependsOn` 已改为正式迁移 ID；菜单 ID 从已冲突的 `900170..900173` 调整为 `9008300..9008303`，并增加父菜单、路径、权限和 ID 冲突 fail-fast 检查。
- 真实 E2E 脚本：新增 `tests\e2e\edhr-nonconformance-review-mvp-real.e2e.js`，目标覆盖测试租户登录、菜单进入、页面造批次、两个来源、冻结提示、三类处置和差异化追溯。
- E2E 缺陷修复：`syncBatchStatus` 现在把 `frozen(15)` 作为受保护状态，详情加载或状态同步不再把冻结批次改回任务计算状态；冻结时打开任务也明确拒绝。
- 真实 E2E 最终结果：`ncr-20260830-04` 一次性覆盖 `PQC_RELEASE -> 让步放行`、`PQC_SUBMISSION -> 返工`、`PQC_RELEASE -> 作废`，评审 ID 为 `4/5/6`，最终批次状态为 `voided(60)`，三类追溯均可见。
