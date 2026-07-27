# 工艺路线版本列表仅显示已生效历史版本

## Task Goal

版本列表中只显示已生效历史版本，即 `ACTIVE` 当前/历史生效版本与 `SUPERSEDED` 已替代历史版本；隐藏 `CANCELLED` 已取消版本以及 `DRAFT`、审核中、待生效、已驳回等未生效候选版本。

## Milestones

1. `completed`：定位版本列表渲染与数据过滤边界。
2. `completed`：补充 RED 静态合同，复现版本列表不是 effective-only 的问题。
3. `completed`：实现前端 effective-only 过滤，不改变后端只读快照读取能力。
4. `completed`：运行目标静态合同、既有深链合同和类型检查。
5. `completed`：启动 slot 8 本机前后端并通过真实 Playwright E2E 复验版本工作区。
6. `completed`：记录证据、提交并推送任务分支。

## Expected Verification

- 静态合同证明版本列表只允许 `ACTIVE` / `SUPERSEDED` 已生效历史版本。
- 静态合同证明不得只排除 `CANCELLED`，`DRAFT`、审核中、待生效、已驳回等未生效候选版本也不得显示。
- 既有“已取消历史版本只读查看”静态合同仍通过，保证后端/深链查看能力不被误删。
- 真实 Playwright E2E 使用本机 slot 8 前端 `http://127.0.0.1:8089` 与后端 `http://127.0.0.1:48089`，从页面打开工艺路线版本工作区，只读断言有效历史版本可见、未生效候选版本和取消版本不可见、无 MES 写请求。

## Current Status

ready_for_closeout

## Closeout Blocker

- Implementation commit: `d1f37893 fix: hide cancelled route versions from list`。
- Remote branch pushed before real E2E continuation: `origin/codex/20260727-route-history-cancelled-version-view`，remote HEAD `778fc54d`。
- Real E2E continuation commit: `5efc7cd1 test: add route version list real e2e evidence`。
- Effective-only audit found the prior implementation still allowed `DRAFT` in the list; follow-up changes are pending commit/push after verification.
- `task-closeout-cleanup --mode preview` keeps `task.md`、`execution-log.md`、`verification-report.md`、`bug-regression-evidence.md`、`frontend-feature-evidence.md` and deletes nothing。
- Closeout apply / ff-only merge / worktree removal is blocked because current branch cannot be fast-forward merged into `int_main` and main worktree `E:\IntRuoyi` is dirty。

## 经验门禁

- 命中 `docs/frontend-development.md#前端静态契约隔离门禁`：当前是窄范围列表展示缺陷，使用任务专用静态合同做 RED/GREEN。
- 命中 `docs/e2e-rules.md#静态合同与真实-e2e-同步门禁`：修改静态合同需确认真实页面路径仍与当前版本工作区行为一致。
- 命中 `docs/backend-development.md#历史关闭候选版本只读快照边界`：列表隐藏已取消版本不得删除后端按 `routeVersionId` 读取冻结快照的能力，写入仍只允许 `DRAFT`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；在列表展示层明确只允许已生效历史状态，并保留深链只读读取契约。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260727-route-version-list-active-history-only/bug-regression-evidence.md
- doc/tasks/20260727-route-version-list-active-history-only/frontend-feature-evidence.md
