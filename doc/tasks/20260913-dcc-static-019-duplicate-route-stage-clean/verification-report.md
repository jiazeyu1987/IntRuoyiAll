## Current Status

blocked

## Scope

验证 DCC-STATIC-019：固定四阶段审批路线中的重复阶段必须在保存边界明确拒绝，运行态不得静默丢弃重复阶段的后续人员。未执行真实 E2E、服务启动或数据库写入；用户于 2026-09-14 授权 Git commit/push，任务实现已提交到分支 `codex/dcc-static-019-duplicate-route-stage-clean`。

## Bug

固定四阶段路线允许保存阶段 1、2、2、3、4；运行态按 `stageCode` 建 Map 时重复键使用第一条值，导致第二组具备资格的审批人员被静默丢弃。

## Expected

每个固定阶段 1、2、3、4 必须恰好一条。重复阶段在保存或预览边界明确拒绝；历史重复数据进入运行解析时也必须显式失败，不能继续消费不完整人员集合。

## Reproduction

构造两个阶段 2 节点并分别配置不同用户，调用保存、预览和运行态 assignee Map 构建路径。修复前保存/预览不抛异常，Map 构建保留第一组；修复后分别抛出固定阶段错误或运行态不匹配错误。

## Root Cause

固定阶段校验先对阶段编号执行 `distinct()`，把重复项隐藏后再比较 1、2、3、4；两个运行态 `Collectors.toMap` 合并函数 `(left, right) -> left` 又静默丢弃重复 `stageCode` 的后续人员。

## BDD Acceptance

- `Given` 阶段 1、2、2、3、4 且两条阶段 2 配置不同合格人员，`When` 保存或预览路线，`Then` 后端明确拒绝；运行态若遇到历史重复 `stageCode` 也明确失败。
- `Given` 阶段 1、2、3、4 各一条，`When` 保存固定路线，`Then` 继续通过固定策略校验并保持四阶段数据一致。

## Implementation Evidence

- `DccFixedApprovalRoutePolicy` 对保存节点和持久化节点校验总数及集合，重复阶段不再被 `distinct()` 隐藏。
- `DccControlledFileApprovalRouteAssigneeResolver` 对重复 `stageCode` 抛出 `CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH`，不再保留第一组并丢弃后续人员。
- `RouteForm.vue` 在调用 `saveApprovalRoute` 前检查固定阶段 1、2、3、4 各恰好一条。

## Verification

- RED：DCC-019 前端静态合同、后端保存/预览回归测试、运行态重复 Map 回归测试均按预期暴露修复前缺陷。
- GREEN：前端静态合同 PASS；`DccApprovalRouteAdminServiceImplTest` 与 `DccControlledFileApprovalRouteAssigneeResolverTest` 合计 27 tests PASS；已有 DCC 路线摘要静态合同 PASS；`pnpm ts:check` PASS；`git diff --check` PASS。
- 依赖准备：`corepack pnpm install --frozen-lockfile` PASS，pnpm v10.25.0。
- 授权后复验：任务分支 rebase 到本地 `int_main` 后，`git diff --check`、两个前端静态合同、`pnpm ts:check`、后端 27 项定向测试均 PASS；实现提交随后再次 rebase 到最新本地 `int_main`，当前实现提交为 `34b5585ed`。
- 合并请求复验：用户要求合并到 `int_main` 后，任务分支再次 rebase 到本地 `int_main` 的 `cd376a2a2`，`git diff --check int_main..HEAD`、两个前端静态合同、`pnpm ts:check`、后端 27 项定向测试均 PASS。
- 提交融合复核：用户继续要求“提交并融合进int_main”后，主工作区资源改动先独立基线提交为 `02e0e32a6`；任务分支合入最新本地 `int_main` 为 `5c93f7b34`，再合入最新 `origin/int_main` 为 `59214c275`，合入模拟无 DCC-019 冲突。
- 远端融合验证：最终推送前 `git diff --check origin/int_main..HEAD` PASS、端口守卫 PASS、两个前端静态合同 PASS、`pnpm ts:check` PASS、DCC 后端 27 项定向 Maven 测试 PASS；`git push origin HEAD:int_main` 非强制快进成功，`origin/int_main` 更新到 `59214c275`。

RED: 修复前静态合同、保存/预览回归测试和运行态重复 Map 回归测试均失败，原因分别为前端未拦截、后端未拒绝和后续人员被静默丢弃。

GREEN: 修复后静态合同、27 项后端定向测试、路线摘要静态合同、TypeScript 类型检查和 diff 检查全部通过。

## Remaining Risk

- 未执行真实页面 E2E、服务运行态和数据库写入验证；本任务按用户约束仅做静态逻辑、定向单元测试、类型检查和 diff 检查。
- 数据库未增加唯一约束；历史重复路线在预览或运行解析时会显式失败，需要后续数据治理或人工修复既有脏数据。

## Blockers

- 未执行真实页面 E2E、服务启动或数据库写入；这是本任务用户约束的验证边界，不是代码测试失败。
- 用户授权后已创建具名任务分支并登记 runtime slot 17，detached HEAD blocker 已解除。
- cleanup preview after authorization 保留三份任务记录、删除项为空；远端 `origin/int_main` 已通过非强制快进完成融合；cleanup apply / 本地 ff-only 合入 / worktree 删除仍 BLOCKED：主工作区 `E:\IntRuoyi` 当前 `int_main...origin/int_main [ahead 1, behind 7]`，且存在其它任务 dirty 文件，按 closeout 规则不能作为安全本地合入目标。

## Closeout Blocker

实现与验证已完成，用户授权后已创建任务分支、登记 runtime slot 并提交实现。用户要求合并到 `int_main` 后，任务分支已吸收最新远端主线并通过非强制推送融合进 `origin/int_main`。本地 `E:\IntRuoyi` 仍存在其它任务 dirty/ahead-behind 状态；cleanup apply 需要本地主工作区干净后才能执行本地 ff-only 合入和删除当前 worktree。因此本地 closeout 仍为 `blocked`，不能标记 `completed`。
