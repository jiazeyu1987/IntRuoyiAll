# 受控浏览完整 E2E

## Task Goal

在真实前端页面中使用仓库指定的 E2E 测试租户和账号，完整走一遍受控浏览流程，记录页面入口、列表、详情、预览和关键页面状态。业务动作必须由浏览器页面完成，API 仅允许只读辅助核验。

## BDD

BDD: 受控浏览主流程 -> Given 测试账号可登录且运行态可归因；When 从工作台进入受控浏览、打开可见受控文件、进入详情并触发在线预览；Then 页面完成真实导航，目标文件信息和预览内容可见，控制台/页面错误按规则记录。

## Milestones

1. 完成文档、分支、端口、前后端健康和浏览器入口预检。
2. 用真实浏览器完成登录和受控浏览页面路径。
3. 记录页面证据、错误、阻断原因和最终结论。
4. 清理本任务临时证据并完成任务收尾检查。

## Expected Verification

- 真实前端登录成功，租户为“芋道源码”，账号标签为 `admin`。
- 通过页面进入受控浏览，不手工拼接业务详情 URL。
- 页面可见受控文件列表、至少一个详情入口和在线预览结果。
- 记录页面导航、目标请求状态、page error、console error 和截图/trace 情况。
- 若运行态端口与当前分支不匹配且无法解释，结论为 `BLOCKED`，不得写成 E2E PASS。

## Design Constraints

- 使用 Playwright/真实浏览器页面完成被验收动作；禁止 `fetch`、`apiGet`、直接 API 写入或数据库写入。
- 业务通过依据只能来自真实页面的点击、导航、可见 DOM、输入值、弹窗、按钮状态和预览渲染；网络响应监听仅用于记录自然请求状态，不解析接口 JSON 作为业务通过依据。
- 不删除、修改或清理共享 E2E 租户基线数据。
- 不停止或重启现有 `int_main` 后端；不执行 Git 提交、推送。
- 不记录密码、token、Cookie 或敏感文件内容。

## Current Status

ready_for_closeout

## Cleanup Keep

- doc/tasks/20260919-controlled-browse-e2e/task.md
- doc/tasks/20260919-controlled-browse-e2e/execution-log.md
- doc/tasks/20260919-controlled-browse-e2e/verification-report.md
- doc/tasks/20260919-controlled-browse-e2e/real-evidence.json
- doc/tasks/20260919-controlled-browse-e2e/controlled-browser-blocked.png
- doc/tasks/20260919-controlled-browse-e2e/controlled-browse-final.png
- doc/tasks/20260919-controlled-browse-e2e/controlled-file-detail.png
