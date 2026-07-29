# 20260729-card-text-double

## Task Goal

将 eDHR 填写辅助模式中所有卡片内对应文字字号提高到原来的 2 倍，保持现有卡片布局、数据来源、交互链路和错误暴露方式不变。

## Milestones

1. 建立任务文档、记录既有脏工作区基线提交和适用经验门禁。
2. 定位卡片文字样式和现有静态契约，先补充可 RED 的最小前端静态契约。
3. 用最小样式改动将卡片内标签、输入文字、占位文字、单位文字等对应文字放大为原来的 2 倍。
4. 运行目标静态契约和必要前端回归验证，记录结果。
5. 收尾：任务状态进入 ready_for_closeout，完成清理、经验沉淀、提交和推送后标记 completed。

## Expected Verification

- `node tests/e2e/edhr-fill-workspace-card-density-static.spec.js`
- `node tests/e2e/edhr-assist-fill-mode-static.spec.js`
- `node tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js`
- `pnpm ts:check`

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，通过统一卡片样式变量/选择器调整显示字号，不改变数据或交互链路。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- `docs/experience-index.md` 存在；本任务命中前端页面/样式、前端静态契约隔离、PowerShell/Git 脏工作区基线与同文件选择性暂存门禁。
- 基线提交：`443621b4 chore: baseline dirty worktree before card text sizing`，保存本任务开始前既有脏改动。
- 额外基线提交：`a6cfc066 chore: baseline preexisting worktree changes`，保存 RED 契约与任务文档的前置状态；本任务实现提交只包含源码样式和最终任务证据。

## Cleanup Keep

- doc/tasks/20260729-card-text-double/frontend-feature-evidence.md
