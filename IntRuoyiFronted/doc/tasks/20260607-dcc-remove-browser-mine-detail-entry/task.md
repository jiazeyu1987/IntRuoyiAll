# 任务：移除 DCC 浏览与我的文件详情入口

## 任务目标

移除 `DCC受控浏览` 和 `DCC我的文件` 列表页中的“详情”按钮。这两个页面不再需要进入受控文件详情页。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260607-dcc-onlyoffice-anonymous-name-prompt/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只修改浏览页、我的文件页及其任务记录。

## BDD 场景

- BDD: 浏览页不再显示详情入口 -> Given 用户打开 DCC受控浏览 / When 查看每行操作区 / Then 页面不显示“详情”按钮，也不再从该页跳详情页。
- BDD: 我的文件不再显示详情入口 -> Given 用户打开 DCC我的文件 / When 查看每行操作区 / Then 页面不显示“详情”按钮，也不再从该页跳详情页。

## Milestones

- [x] M1：建立任务文档。
- [ ] M2：移除两个页面的详情按钮和未使用跳转函数。
- [ ] M3：做静态回归并记录结果。

## Expected Verification

- `rg -n "详情|openDetail|DccControlledFileDetail" src/views/dcc/controlled-file/browser/index.vue src/views/dcc/controlled-file/mine/index.vue`

## 当前状态

in_progress
