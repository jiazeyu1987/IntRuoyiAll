# 提交并推送主干代码

## Task Goal

按用户要求提交并推送当前 `int_main` 主干工作区代码到 `origin/int_main`。

## Milestones

- [x] 读取提交/收尾相关规则和技能说明。
- [x] 汇总当前工作区脏改并执行独立基线提交。
- [x] 推送 `int_main` 到 `origin/int_main`。
- [x] 验证本地分支不再领先远端且工作区干净。
- [x] 完成收尾清理并记录最终结果。

## Expected Verification

- `git status --short --branch` 显示 `int_main...origin/int_main` 且无脏改、无 ahead。
- `git push origin int_main` 成功。
- `task-closeout-cleanup` preview/apply 通过。

## Current Status

completed

## Design Constraints Check

- 遵守无 fallback、无强推、无历史改写、无模拟成功。
- 本轮用户已明确授权提交和推送主干代码。
- 当前脏工作区按规则作为独立基线提交，不拆分或推断跨任务归属。
