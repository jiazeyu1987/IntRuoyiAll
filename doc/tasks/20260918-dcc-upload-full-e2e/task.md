# DCC Upload Full E2E

## Task Goal

使用本机 `int_main` 运行态、真实浏览器页面和真实账号完整走一遍 DCC 受控文件上传主流程：登录租户、进入上传页、选择正式文件分类叶子节点、上传任务自有 PDF、完成提交前预览、提交审批，并记录页面结果、自然触发请求和临时上传清理结果。

## Milestones

1. `PASS`：完成运行态、测试文件、账号入口和页面权限前置检查。
2. `BLOCKED`：真实 Playwright 已完成登录、进入上传页和选择项目，但最新后端运行包因缺少正式 MES 独立回执配置无法启动，未能继续到上传预览。
3. `NOT_REACHED`：未产生本轮上传票据，因此没有提交后页面状态或临时上传会话可核对。
4. `blocked`：数据库模板表已按正式迁移补齐，旧本机运行态已恢复；最新运行包仍缺少不可猜测的正式配置，任务不能进入收尾完成态。

## BDD

### BDD-01 Complete controlled-file upload

- Given：本机前端和后端健康，租户为“芋道源码”，登录账号为 `admin`，任务拥有一份唯一 PDF 文件。
- When：用户从真实 DCC 上传页面选择正式文件分类叶子节点，选择任务自有文件并提交审批。
- Then：页面完成上传预览，显示文件信息和正式分类/目录，提交动作返回业务成功并进入明确完成状态；自然触发的目标请求 HTTP 和业务码可核对。

### BDD-02 Temporary upload cleanup

- Given：上传预览已完成且尚未创建正式受控文件。
- When：通过真实页面移除本轮临时上传文件或结束页面流程。
- Then：临时上传会话清理请求成功，未产生非预期 DCC 写入请求，任务自有临时数据不残留。

## Expected Verification

- Playwright 真实页面完成登录和上传，不使用 `fetch`、APIRequest 或数据库写入代替业务动作。
- 页面记录租户/账号标签、目标页面、分类叶子、文件名、上传预览和提交响应。
- 记录 `pageerror`、console error、DCC 目标网络错误和所有非 GET DCC 请求。
- 保存 `trace.zip`、关键步骤截图和 `verification-report.md`。
- 运行态使用本机 `http://127.0.0.1:8081` / `http://localhost:8081` 和 `http://127.0.0.1:48081`，不操作远端环境；首次登录超时后按 `docs/login-access.md` 使用同一登录入口的 `localhost` 别名复验。
- 任务数据使用唯一 run id；本轮临时上传文件和目录在完成后清理，若正式提交产生业务数据则按页面可见状态记录保留/清理边界，不直接改库。

## Design Constraints Check

- 仅通过真实前端页面执行登录、选择、上传、预览和提交动作。
- API/数据库只允许做 health 或最终只读核验；不调用写接口，不直接改状态。
- 不记录密码、token、Cookie、私钥、连接串密钥。
- 不修改并行工作区、无关代码或共享基线数据。
- 不操作远端环境；仅在用户本轮授权范围内修复本机 `int_main` 运行依赖，必要时按标准脚本重启本机后端并记录 PID、Jar 和健康检查。
- 不执行 Git 提交、推送或远程操作。

## Current Status

in_progress

## Cleanup Keep

- doc/tasks/20260918-dcc-upload-full-e2e/task.md
- doc/tasks/20260918-dcc-upload-full-e2e/execution-log.md
- doc/tasks/20260918-dcc-upload-full-e2e/verification-report.md
- doc/tasks/20260918-dcc-upload-full-e2e/dcc-upload-full-real.e2e.js
- doc/tasks/20260918-dcc-upload-full-e2e/upload-source.pdf
- doc/tasks/20260918-dcc-upload-full-e2e/migration-policy-gate-dcc-project-template.json
