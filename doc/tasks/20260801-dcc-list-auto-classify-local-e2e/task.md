# DCC 列表未分类自动归类本地 E2E 验证

## Task Goal

使用本机 `int_main` 运行态和用户提供的本地 `芋道源码/admin` 账号继续验证 DCC 项目代码列表页“按文件名归类未分类”入口。默认执行只读/取消确认路径：登录、进入 `基础数据 / DCC项目代码`、确认列表页按钮存在、触发确认框并取消，验证不会发起 DCC 元数据写入。

## Milestones

- [x] 创建任务文档并记录本地 E2E 边界。
- [x] 保存既有脏工作区基线，隔离本任务脚本和证据。
- [x] 确认本机前端 `8081`、后端 `48081` 运行态可用。
- [x] 编写并运行 Playwright 只读/取消确认 E2E。
- [x] 记录验证报告并完成 cleanup preview/apply。
- [ ] 提交并推送本任务记录。

## Expected Verification

- `Invoke-WebRequest http://127.0.0.1:8081/`
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health`
- `node doc/tasks/20260801-dcc-list-auto-classify-local-e2e/dcc-list-auto-classify-readonly.e2e.mjs`

## Current Status

ready_for_closeout

## Scope Boundary

- 本轮仅验证真实登录和页面入口可见性，并在确认框点击取消。
- 不点击确认执行批量归类，因为该动作会批量修改真实受控文件元数据。
- 登录密码只通过临时环境变量传入脚本，不写入任务文档、日志或提交记录。

## 适用经验门禁

- `芋道源码/admin` 仅执行只读验证；缺少可写测试数据和清理授权时，写入型 E2E 必须阻塞。
- Playwright 必须分别记录本机/DCC 目标链路错误与外部资源异常；不得通过全局忽略 console/network 错误制造通过。
- 只读/取消确认路径必须证明 DCC 写请求数量为 0。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，使用真实本机运行态与真实前端页面验证入口行为。
- `是否存在临时补丁或绕过`：否。
