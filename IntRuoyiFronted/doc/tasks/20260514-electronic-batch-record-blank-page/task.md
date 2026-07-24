# Task: 电子批记录空白页修复

## Goal

修复 `电子批记录` 菜单 `/mes/pro/batch-record-template` 打开后静默空白的问题，并用回归测试证明菜单指向的前端组件真实存在。

## Milestones

1. [x] 复现空白页并确认运行时菜单 `component` 配置。
2. [x] 编写回归测试，证明菜单指向的组件文件在当前分支缺失。
3. [x] 恢复 `mes/pro/batchrecordtemplate/index` 入口组件，避免页面静默空白。
4. [x] 在入口页中显式探测后端接口状态，并 fail fast 显示缺少的前置条件。
5. [x] 运行 GREEN 验证并记录结果。

## Expected Verification

- 运行时菜单中的 `电子批记录` 条目能解析到真实前端视图文件。
- 页面不再是静默空白。
- 当后端 `/admin-api/mes/pro/batch-record-template/**` 缺失时，页面直接展示阻塞信息。

## Current Status

Completed. 当前修复已恢复非空白入口页，并将“后端批记录接口缺失”显式暴露给用户。

## Final Verification Result

- `node --test scripts/electronic-batch-record-route.test.mjs` -> PASS
- `pnpm exec eslint scripts/electronic-batch-record-route.test.mjs src/views/mes/pro/batchrecordtemplate/index.vue` -> PASS
- `pnpm build:local` -> PASS
- `pnpm ts:check` -> blocked by existing workspace-wide Node OOM, not by this task's changed files

## Completion Status

Completed.
