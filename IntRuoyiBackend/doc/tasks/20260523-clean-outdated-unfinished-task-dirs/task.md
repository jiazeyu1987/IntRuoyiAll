# 任务：清理过时未完成任务目录

## Goal

- 按用户明确要求，清理当前仓库工作区里已经过时、且尚未完成的未跟踪任务目录。
- 仅删除当前工作区中的未跟踪 `doc/tasks/**` 或 `yudao-module-showroom/doc/tasks/**` 目录，不删除已提交的 completed / blocked 历史记录，不修改业务代码。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\2026-05-22-product-004-showroom-cover\`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\2026-05-22-product-046-showroom-cover\`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-e2e-publish-1779350997526-showroom-cover-single-rerun\`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-product-004-showroom-cover-single-native\`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-product-010-showroom-cover\`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-product-027-showroom-cover-native-single-pass-20260522-222331\`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-product-042-showroom-cover-single-native\`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-product-046-showroom-cover-single-native\`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-product-047-showroom-cover-single-native\`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-product-048-showroom-cover-single-native\`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-product-051-showroom-cover-single-native\`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\product-038-showroom-cover\`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\task-product-021-showroom-cover\`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\doc\tasks\20260522-batch-cover-fail-showroom-cover-095540\`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\doc\tasks\20260522-batch-cover-ok-showroom-cover-095452\`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260523-clean-outdated-unfinished-task-dirs\**`

## Non-Scope

- 不删除任何已提交的 completed / blocked 任务记录。
- 不修改业务源码、SQL、测试代码或图片产物。
- 不清理 `yudao-module-infra` 的 `CodegenEngineAbstractTest.java`。

## Milestones

1. 创建清理任务文档并锁定待删除目录清单。
2. 复核这些目录当前均为未跟踪且状态为 `in-progress`。
3. 删除目录并复核 `git status` 中对应噪音已消失。
4. 提交本次清理任务记录。

## Expected Verification

- `git status --short`
- 待删除目录路径全部不存在

## Current Status

- Status: Completed

## Completed Work

- 已复核 15 个待清理目录在当前工作区里均为未跟踪任务目录，且任务状态为 `in-progress`。
- 已按用户要求删除这 15 个过时未完成任务目录。
- 已额外删除误写到 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-pro\doc\tasks\20260523-clean-outdated-unfinished-task-dirs\` 的临时任务目录，避免污染错误仓库。
- 已复核 `git status --short` 中上述 15 个目录项全部消失。

## Final Verification Result

- PASS: 15 个目标目录路径均已不存在
- PASS: `git status --short` 不再出现这些目录
