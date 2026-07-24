# 任务：清理前端过时未完成任务目录

## Goal

- 清理 `yudao-ui-admin-vue3` 当前工作区中已经过时、且尚未完成的未跟踪任务目录。
- 仅删除未跟踪的 `doc/tasks/**` 目录；已完成任务记录保留并随后提交，不修改业务源码。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-barcode-default-template-print-integration\`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-login-remove-secondary-sections\`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-approval-signature-workflow\`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-assignee-lifecycle-notify-list\`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-frontstage-menu-permission-guard\`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-showroom-product-visibility-tighten\`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-display-name-tab-sync\`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-workbench-placeholder-parse-fix\`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-batch-narration-script-recovery\`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-narration-current-product-status\`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-status-column\`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260523-clean-outdated-unfinished-task-dirs-frontend\**`

## Non-Scope

- 不删除已完成任务目录。
- 不修改 `src/**`、`scripts/**` 或业务接口实现。
- 不清理当前仓库之外的 worktree 任务目录。

## Milestones

1. 创建清理任务文档并锁定待删除目录清单。
2. 复核这些目录当前均为未跟踪且状态为 `blocked`。
3. 删除目录并复核 `git status` 中对应噪音已消失。
4. 提交本次清理任务记录，并继续处理已完成任务记录。

## Expected Verification

- `git status --short`
- 待删除目录路径全部不存在

## Current Status

- Status: Completed

## Completed Work

- 已复核 11 个待删除目录当前均为未跟踪且状态为 `blocked`。
- 已按用户要求删除这 11 个过时未完成任务目录。
- 已复核 `git status --short` 中上述 11 个目录项全部消失。

## Final Verification Result

- PASS: 11 个目标目录路径均已不存在
- PASS: `git status --short` 不再出现这些目录
