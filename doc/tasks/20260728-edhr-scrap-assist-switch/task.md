# 20260728-edhr-scrap-assist-switch

## Task Goal
修复 eDHR 切换填写人时，选择“张可莹 / 工艺路线表单槽位 / 损耗单”应切换到损耗单填写链路，但当前弹出“eDHR 批次缺少唯一批记录路线”的问题。

## Milestones
1. 记录缺陷场景与可观察 BDD 期望。
2. 定位切换填写人链路的数据来源和错误触发点。
3. 先补失败回归测试，再实现最小正式修复。
4. 运行目标测试和相关回归验证。
5. 合并回 int_main、推送并完成收尾记录。

## Expected Verification
- 后端/前端目标回归测试覆盖：同一工序存在批处理表单与表单槽位候选时，选择表单槽位候选不得再要求唯一批记录路线，应进入对应损耗单链路。
- 相关静态或单元测试通过。
- 无新增 fallback、吞异常、默认成功或静默降级。

## Current Status
ready_for_closeout

## Applicable Gates
- `docs/frontend-development.md#切换填写人-formcenter-槽位导航门禁`：FormCenter 表单槽位候选必须跳转批次详情 `openRouteForm=1`，并在详情页二次 `openTask` 透传 `assistUserId`。
- `docs/backend-development.md#切换填写人快照读取边界`：切换填写人候选来自执行详情快照，动态路线表单不得回落到传统批记录 execution snapshot。
- `docs/worktree-memory.md#并行主工作区远端快进融合门禁`：当前本地 `int_main` 有并行脏改动，最终融合必须避免把非本任务基线提交推入主线。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，目标是区分“批记录表单”和“表单槽位”链路，按候选来源进入正式路径。
- 是否存在临时补丁或绕过：否。

## Worktree Evidence
- Worktree: `D:\IntRuoyiWorktree\20260728-edhr-scrap-assist-switch`
- Source branch with baseline history: `codex/20260728-edhr-scrap-assist-switch`
- Clean integration branch: `codex/20260728-edhr-scrap-assist-switch-clean`
- Runtime profile: `int_main`, slot `10`, frontend `8091`, backend `48091`
- Dirty baseline commit before task branch: `3fb50fa6`
- Clean integration evidence: implementation commit `b4700d39` was cherry-picked onto clean `origin/int_main`; after rebasing onto `origin/int_main` commit `7d59f3bf`, the clean implementation commit is `5e87b3ef`, followed by this integration-record commit.
- Worktree port registry branch was updated for the current clean branch with slot `10`, frontend `8091`, backend `48091`.

## Cleanup Keep
- `doc/tasks/20260728-edhr-scrap-assist-switch/bug-regression-evidence.md`
