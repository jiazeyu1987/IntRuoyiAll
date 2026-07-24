# 任务：后端多任务收尾提交

## 任务目标

- 将后端仓库当前已完成且已验证的改动按一次收尾提交入库。
- 仅提交当前后端任务直接产出的源码、测试、SQL 和任务记录，不混入未验证内容或无关临时产物。
- 提交前执行收尾预览，确认暂存范围只覆盖本次已完成任务。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260626-replan-apply-partial-active-task-block\task.md`
- 状态：`COMPLETED`
- 处理说明：已核对当前后端近期任务完成态；本次不新增功能，只做收尾筛选、清理预览与提交。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：无
- 适用强制门禁：
  - 本次不执行真实 E2E、服务器写入、发布、恢复或 worktree 合并；无需 `experience-preflight`，但仍需先做收尾预览。
  - 只暂存已完成任务的正式交付物；不得把未确认的辅助文件或无关改动混入后端提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。本次通过任务归属核对和收尾预览控制提交边界，不用临时全量暂存。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 后端收尾提交只包含正式交付物 -> Given 后端仓库存在多个已完成任务改动 / When 执行收尾筛选和提交 / Then 提交只包含已验证源码、测试、SQL 和正式任务记录。`

## 里程碑

1. M1：核对后端变更与任务完成态。`COMPLETED`
2. M2：执行收尾预览并确认提交范围。`COMPLETED`
3. M3：暂存后端正式交付物并创建提交。`COMPLETED`
4. M4：回写提交结果并完成任务文档。`COMPLETED`

## 预期验证

- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260627-backend-closeout-commit --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro status --short`
- `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro diff --cached --stat`

## 最终验证结果

- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260627-backend-closeout-commit --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` -> PASS
- `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro diff --cached --stat` -> PASS，暂存范围已复核为后端正式交付物、SQL 与任务记录。
