# 20260902 Merge Process Material Route Worktree

## Task Goal

将已在附加 worktree 中完成并验证的“工艺路线批记录物料改为输出物料，并新增输入物料”改动融合进 `int_main` 主工作区。

## Milestones

- [x] 读取并核对合并、worktree、收尾规则
- [x] 识别待融合 worktree、分支、提交和验证证据
- [x] 在 `int_main` 主工作区执行受控融合并处理冲突
- [x] 运行要求的端口契约、构建/测试/E2E 或记录明确阻塞
- [x] 完成任务记录、经验沉淀和收尾

## Expected Verification

- `git status --short --branch` 确认主工作区状态和融合结果
- `scripts\preflight\branch-runtime-port-guard.ps1` 合并后通过
- 若 worktree 已提供 E2E 证据，复核并补充主工作区必要回归；若缺少真实 E2E 前置条件，按 fail-fast 记录 blocker
- 收尾前运行 `task-closeout-cleanup` preview/apply

## Current Status

ready_for_closeout - worktree 内容已融合到 `int_main` 本地提交 `89db09fcd`，验证通过；按项目规则，推送和最终 `completed` 状态需用户明确授权。

## Design Constraints Check

- 遵守 `docs\task-closeout-rules.md`、`docs\worktree-restrictions.md`、`docs\branch-runtime-ports.md`
- 不引入 fallback、降级、吞异常或模拟成功
- 不提交、推送、删除 worktree 或停止/重启主工作区服务，除非获得当轮明确授权且规则允许
- 仅融合当前任务相关 worktree 内容，不覆盖无关并行改动
