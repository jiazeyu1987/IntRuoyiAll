# 任务：backend 原始工作区恢复与 DCC 识别改动保留

## 任务目标

在 backend 仓库里保留用户要求继续保留的 DCC 识别改动，同时把真正需要进入主线的后端代码融合进 `int_main`，为后续把原始 backend 工作区切回主线做准备。

## 当前状态

IN_PROGRESS

## Current Status

IN_PROGRESS

## 上一任务检查

- 上一个 backend 任务：`D:\ProjectPackage\Int\IntRuoyiWorktrees\ruoyi-vue-pro-int-main-one-shot-integration\doc\tasks\20260622-mes-auto-schedule-preflight-gate\task.md`
- 状态：`COMPLETED`
- 处理：MES 排产与 preflight 改动已进入 backend `int_main`，本任务只处理继续保留的 DCC 识别改动与原始 backend 工作区恢复问题。

## 经验门禁

- 已读取：`D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
- 本任务适用强制门禁：
  - 不能为恢复直观目录结构而覆盖原始 backend 工作区里的未提交现场。
  - 真正主线仍以后端仓库 `int_main` 为准，原始工作区与 release worktree 只作为比对来源。
  - 合并/切换/清理前先在 clean `int_main` worktree 上验证 DCC 目标测试通过。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是
- `是否存在临时补丁或绕过`：否

## BDD 场景

- `BDD: 保留用户要求的 DCC 识别改动 -> Given 原始 backend 工作区里还有用户要求保留的 DCC 识别本地改动 / When 把真正要保留的后端代码并回 int_main / Then int_main 必须接住这组 DCC 改动，而不是在恢复目录时把它覆盖丢失。`
- `BDD: 原始 backend 工作区只有在主线已接住 DCC 改动后才允许切回 -> Given clean int_main worktree 已吸收 DCC 识别改动并验证通过 / When 再处理原始 backend 工作区与 holding 分支 / Then 后续切回不会丢失 DCC 改动。`

## 里程碑

1. 在 clean backend `int_main` worktree 重放需要保留的 DCC 识别代码。`DONE`
2. 运行 DCC 定向回归，确认主线已接住该组改动。`DONE`
3. 提交到 backend `int_main`，再继续处理原始 backend 工作区恢复。`IN_PROGRESS`
