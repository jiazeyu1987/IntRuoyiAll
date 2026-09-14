# DCC-STATIC-019 Duplicate Route Stage Guard

## Task Goal

修复 `DCC-STATIC-019`：固定四环节 DCC 审批路线在前后端都必须每个环节恰好一条；重复 `stageCode` 或重复固定环节必须在保存时明确拒绝，不能保存后在流程启动、预览、快照或运行授权中静默丢弃后续人员。

## Source Evidence

- 缺陷登记：`docs/bugs/20260912-dcc-90-step-static-audit.md`
- 当前复核结论：`DCC-STATIC-019` 未修复，场景为保存 1、2、2、3、4 环节时两条 2 号环节分别配置不同合格人员，后续 `stageCode` Map 构建只保留第一组人员。
- 基线要求：本 worktree 为 `origin/int_main` 干净基线，HEAD `49a6ec8b20670fec0862849519fce016c787cc07`。

## Milestones

- [x] M1：读取仓库规则、closeout 规则和相关技能规则，确认基线与任务边界。
- [x] M2：定位后端保存校验、前端固定路线编辑、预览/快照/运行授权中 `stageCode` 唯一性链路。
- [x] M3：先新增能暴露重复 `stageCode` 保存通过的 RED 回归测试或静态合同。
- [x] M4：做最小修复，前后端保存边界均明确拒绝重复固定环节。
- [x] M5：运行定向单元/合同测试、必要编译/类型检查和 `git diff --check`，记录 GREEN 与剩余风险。
- [x] M6：按 closeout 规则执行收尾复核；用户已授权 Git 提交/推送，已创建任务分支、登记运行槽位、提交实现，并把最新本地 `int_main` 合入任务分支；因主工作区再次出现并行未合并冲突，local cleanup apply / worktree 删除仍记录为 `blocked`。

## BDD Scenarios

### BDD: reject duplicate fixed approval stage on save

Given 文控配置固定四环节 DCC 审批路线
And 请求中包含阶段 1、2、2、3、4，且两条阶段 2 分别配置不同合格人员
When 保存审批路线
Then 保存请求被明确拒绝为重复审批环节
And 路线节点、审批预览、快照和运行授权不得保存或消费只保留第一组人员的结果

### BDD: accept exactly one row for every fixed approval stage

Given 文控配置固定四环节 DCC 审批路线
And 请求中包含阶段 1、2、3、4 且每个阶段恰好一条
When 保存审批路线并生成预览
Then 保存成功
And 预览、快照和流程授权使用同一组四个固定阶段及其候选人员

## Expected Verification

- RED：目标回归测试或静态合同在修复前失败，失败原因指向重复 `stageCode` 未被保存校验拒绝。
- GREEN：同一测试/合同在修复后通过。
- REGRESSION：受影响 DCC route/backend 测试、前端静态合同或类型检查按实际修改范围执行。
- Hygiene：`git diff --check` 通过；不执行 E2E、不启动/重启服务、不写数据库；用户于 2026-09-14 授权 Git 提交/推送。

## Design Constraints Check

- 只处理 `DCC-STATIC-019`，不修复 017、018、020、021、024、025、026、027 或其它 DCC 编号。
- 不引入 fallback、降级、吞异常、兼容补丁或默认成功。
- 固定路线按四个正式阶段建模；重复 `stageCode` 必须 fail fast。
- 若发现运行侧仍存在重复 Map 静默丢弃风险，应改为显式拒绝重复，而不是合并或猜测优先级。
- 前端校验只是用户体验边界；后端保存校验必须作为权威边界。
- 本线程已于 2026-09-14 获得 Git commit/push/合入 `int_main` 授权；若主工作区存在并行未合并冲突导致 cleanup apply/worktree 删除无法安全执行，最终记录为 `blocked` 而非 `completed`。

## Cleanup Keep

- doc/tasks/20260913-dcc-static-019-duplicate-route-stage-clean/task.md
- doc/tasks/20260913-dcc-static-019-duplicate-route-stage-clean/execution-log.md
- doc/tasks/20260913-dcc-static-019-duplicate-route-stage-clean/verification-report.md

## Verification Evidence

- 后端保存与预览对重复固定阶段抛出 `APPROVAL_ROUTE_FIXED_STAGE_INVALID`；运行态重复 `stageCode` 抛出 `CONTROLLED_FILE_ROUTE_RUNTIME_MISMATCH`。
- 前端固定路线提交前检查四个阶段各恰好一条；前端静态合同、后端定向 Maven 测试、类型检查和 diff 检查均通过。
- 用户授权后创建分支 `codex/dcc-static-019-duplicate-route-stage-clean`，登记 `int_main` slot 17（前端 8098 / 后端 48098），实现提交在 rebase 到最新本地 `int_main` 后为 `34b5585ed`。
- 2026-09-14 继续融合：主工作区资源改动独立基线提交为 `02e0e32a6`；任务分支合入最新本地 `int_main` 的提交为 `5c93f7b34`，当前任务分支相对本地 `int_main` 为 `0 5`，可作为远端快进融合候选。
- 详见 `execution-log.md` 与 `verification-report.md`。

## Current Status

blocked - 实现与定向验证已完成并提交到任务分支；任务分支已吸收最新本地 `int_main`，但主工作区 `E:\IntRuoyi` 又出现并行未合并冲突，按 closeout 规则暂不能执行 local cleanup apply 或删除当前 worktree；将改走非强制远端快进融合门禁。
