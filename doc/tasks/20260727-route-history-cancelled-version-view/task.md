# 已取消工艺路线历史版本只读查看修复

## Task Goal

修复在工艺路线版本工作区点击“查看”已取消历史版本时，页面提示“工艺路线候选版本未满足发布条件，routeVersionId=262，status=CANCELLED”的问题。已取消版本必须从自身冻结快照只读展示，写操作仍严格禁止。

## Milestones

1. `completed`：定位前后端版本查看链路和状态校验不一致根因。
2. `completed`：创建隔离 worktree，并补充 BDD 与 RED 回归测试。
3. `completed`：实现历史关闭版本统一只读读取契约，不放宽任何写入校验。
4. `completed`：运行目标后端测试、前端静态合同和必要回归。
5. `completed`：完成经验沉淀、提交与远端推送；closeout apply / ff-only 合并等待主工作区恢复干净。

## Expected Verification

- 后端关系图读取测试证明 `CANCELLED` 版本从 `routeSnapshotJson.configSnapshots.flowGraph` 返回历史图。
- 后端工艺流程配置读取测试证明 `CANCELLED` 版本从自身快照返回排产、批记录与附件负责人配置。
- 回归测试证明 `CANCELLED` 版本仍不能保存关系图、排产配置或批记录配置。
- 前端静态合同证明版本工作区对已取消版本提供“查看”，并以只读版本上下文传递 `routeVersionId`。
- 相关 Maven 目标测试和前端静态合同通过；本任务未改前端 TypeScript 生产代码，类型检查不作为必须门禁。

## Current Status

ready_for_closeout

## Closeout Blocker

- Implementation commit: `3d809a8e fix: allow readonly cancelled route versions`。
- Closeout evidence commit: `2b7a5dc3 docs: record route history closeout status`。
- Remote branch pushed: `origin/codex/20260727-route-history-cancelled-version-view`。
- Worktree runtime slot registered: `int_main slot=8`，frontend `8089`，backend `48089`。
- `task-closeout-cleanup --mode preview` keeps `task.md`、`execution-log.md`、`verification-report.md`、`bug-regression-evidence.md` and deletes nothing。
- Closeout apply / ff-only merge / worktree removal is blocked because main worktree `E:\IntRuoyi` is dirty; current task branch remains `ready_for_closeout` until the main workspace can receive the merge safely。

## Root Cause

- 前端版本工作区允许除草稿外的历史版本进入只读查看。
- 后端关系图与工艺流程配置读取白名单只包含 `DRAFT`、`PENDING_APPROVAL`、`READY_TO_PUBLISH`，另行允许 `ACTIVE`、`SUPERSEDED`。
- `CANCELLED` 与 `REJECTED` 作为已关闭候选版本保留冻结快照，但读取校验将其错误归入“不可发布”异常，造成只读查看失败。

## Completed Work

- 后端关系图读取新增历史快照可读状态：`REJECTED`、`CANCELLED`、`SUPERSEDED`。
- 后端流程配置读取新增关闭候选快照读取：`REJECTED`、`CANCELLED`。
- 后端排产配置读取新增候选只读快照读取：`PENDING_APPROVAL`、`READY_TO_PUBLISH`、`REJECTED`、`CANCELLED`。
- 写入侧仍只允许 `DRAFT`；`CANCELLED` 保存关系图、流程配置、排产配置均保持 fail-fast。
- 前端新增静态合同，锁定已取消历史版本通过“查看”进入只读版本上下文。
- 长期经验已并入 `docs/backend-development.md#历史关闭候选版本只读快照边界`，并在 `docs/experience-index.md` 增加可命中关键词。

## 经验门禁

- 版本读取和版本写入必须使用独立状态集合；扩展历史只读范围不得放宽草稿专属写入校验。
- 历史关闭版本只能读取自身冻结快照，不得回退到当前生效配置或实时工序设置。
- 缺少或损坏冻结快照时必须明确失败，不得返回空图、当前图或默认配置。
- 当前主工作区存在并发任务脏改动，本任务必须在 `D:\IntRuoyiWorktree\` 下隔离实施。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；统一历史版本只读状态契约，并保持写入状态校验不变。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260727-route-history-cancelled-version-view/bug-regression-evidence.md
