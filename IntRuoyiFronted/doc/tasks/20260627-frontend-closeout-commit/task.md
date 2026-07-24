# 任务：前端多任务收尾提交

## 任务目标

- 将前端仓库当前已完成且已验证的改动按一次收尾提交入库。
- 仅提交当前前端任务直接产出的源码、测试、SQL 无关前端文档与任务记录，不混入临时日志、一次性调试产物或未完成任务辅助脚本。
- 提交前执行收尾预览，确保暂存范围与当前已完成任务一致。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260627-mes-schedule-order-row-actions-trim\task.md`
- 状态：`COMPLETED`
- 处理说明：已核对当前前端近期任务完成态；本次不新增功能，只做收尾筛选、清理预览与提交。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：无
- 适用强制门禁：
  - 本次不执行真实 E2E、服务器写入、发布、恢复或 worktree 合并；无需 `experience-preflight`，但仍需先做收尾预览。
  - 只暂存已完成任务的正式交付物；`tmp/` 日志、一次性辅助脚本和未明确纳入交付的临时产物不得混入提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。本次通过任务归属核对和收尾预览控制提交边界，不用临时全量暂存。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 前端收尾提交只包含正式交付物 -> Given 前端仓库存在多个已完成任务改动与少量临时产物 / When 执行收尾筛选和提交 / Then 提交只包含已验证源码、测试和正式任务记录，不包含 tmp 日志或未确认辅助脚本。`

## 里程碑

1. M1：核对前端变更与任务完成态。`COMPLETED`
2. M2：执行收尾预览并清理不应提交的临时产物。`COMPLETED`
3. M3：暂存前端正式交付物并创建提交。`COMPLETED`
4. M4：回写提交结果并完成任务文档。`COMPLETED`

## 预期验证

- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260627-frontend-closeout-commit --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- `git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 status --short`
- `git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 diff --cached --stat`

## 最终验证结果

- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260627-frontend-closeout-commit --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` -> PASS
- `Remove-Item tmp\vite-8081.stderr.log; Remove-Item tmp\vite-8081.stdout.log` -> PASS
- `git -C D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 diff --cached --stat` -> PASS，暂存范围已复核为前端正式交付物与任务记录。
