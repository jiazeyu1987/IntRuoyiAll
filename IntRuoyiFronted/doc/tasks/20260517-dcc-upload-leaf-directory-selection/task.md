# Task: DCC 上传目录下钻到叶子目录

## Goal

让 DCC 受控文件上传页从“只选文件类别”升级为“文件类别绑定目录起点 + 叶子目录级联选择”，并让目录浏览页支持父目录汇总子孙文件。

## Scope

- 先确认上一条任务已完成，再创建本任务记录。
- 上传页增加目录级联选择与绑定路径展示。
- 浏览页调用分页接口时开启子孙目录汇总。
- 同步更新前端 API 类型、提交参数与真实验证脚本。
- 不改业务为 fallback，不用 mock 数据掩盖缺失目录。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260517-mes-pro-route-list-owner-last-process/task.md`
- Status before this task: completed.
- Impact: no unfinished frontend task blocks this DCC upload-path change.

## BDD

BDD: 上传页要求继续选到叶子目录 -> Given 用户已选择某个绑定到多层目录树的文件类别 / When 用户打开上传页并选择目录 / Then 页面必须展示绑定路径并要求继续选到最后一层叶子目录才能提交。

BDD: 浏览页点击父目录可汇总子孙文件 -> Given 用户在目录浏览页选择一个父目录 / When 页面查询受控文件列表 / Then 列表应显示该父目录及其子孙目录中的文件记录。

## Milestones

- [x] M1: 记录前端 RED 级联选择与浏览汇总场景。
- [x] M2: 改造上传页目录选择与提交参数。
- [x] M3: 改造浏览页分页参数与文案。
- [x] M4: 运行 `pnpm ts:check` 与真实 Playwright 验证。
- [x] M5: 完成前端任务文档与提交准备。

## Expected Verification

- `pnpm ts:check`
- 真实 Playwright 路径验证 `http://127.0.0.1:8081`

## Current Status

Completed.

## Final Verification Result

- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-upload-leaf-directory-selection run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260517-dcc-upload-leaf-directory-selection\scripts\verify-dcc-upload-leaf-directory-selection.mjs` -> PASS
- 真实页面结果：
  - 上传页显示绑定路径 `3.DMR/01.图纸`
  - 未选叶子目录时阻止提交
  - 选择叶子目录后提交 payload 含真实 `directoryId`
  - 浏览页点击父目录后可看到刚提交的深层目录文件
