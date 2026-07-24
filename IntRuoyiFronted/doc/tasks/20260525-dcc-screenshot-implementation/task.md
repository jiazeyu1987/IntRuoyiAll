# 任务：DCC 截图需求实现（前端）

- 任务编号：`20260525-dcc-screenshot-implementation`
- 创建日期：`2026-05-25`
- 状态：`已完成`
- 仓库：`yudao-ui-admin-vue3`
- Worktree：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260525-dcc-screenshot-implementation\yudao-ui-admin-vue3`
- 分支：`task/20260525-dcc-screenshot-implementation`

## 任务目标

按用户授权的默认业务规则，实现 DCC 截图需求对应的前端表单、列表、详情、流程动作、下载确认、发放、打印和密码交互。后端监督任务文档位于 `ruoyi-vue-pro/doc/tasks/20260525-dcc-screenshot-implementation/`。

## 里程碑

- [x] M1：创建成对实现 worktree。
- [x] M2：确认并记录前端必须遵循 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- [x] M3：T2 上传、列表和下载入口。
- [x] M4：T4 流程动作与第四节点页面。
- [x] M5：T5 发放、打印、外来文件和密码交互。
- [x] M6：真实路径 E2E 与收口。

## 预期验证

- 前端类型检查或项目既有验证命令。
- Playwright 从本任务 worktree 前端 `http://127.0.0.1:8089` 使用真实测试租户验证关键路径，代理后端 `http://127.0.0.1:48089`。
- 不新增测试专用入口、mock endpoint 或前端自行放权。

## 当前状态

已完成。T2/T4 前端静态回归、类型检查和真实 E2E 均通过；`/user/profile` 可进入个人中心 `密码设置`，DCC 详情暴露接收人加签、流程打印和流程导出 Word，外来文件评审复用现有上传流程并传递 `EXTERNAL_REVIEW`。
