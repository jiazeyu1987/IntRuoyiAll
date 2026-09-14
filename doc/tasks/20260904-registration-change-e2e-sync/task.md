# 20260904 Registration Change E2E Sync

## Task Goal

根据 `E:\IntRuoyi\e2e_test\registration\biangeng\registration-certificate-change-e2e-acceptance.md`，在同步到最新 `int_main` 的隔离 worktree 中继续执行注册证变更 E2E 验证；用户确认后，将注册证变更审批流程改为不需要走短信/手机号校验，去除缺少注册证提醒任务配置时阻塞审批的限制，并将注册证 BPM 候选审批人改为共享待办、先审批者实际办理。

## Milestones

- [x] 读取项目 E2E、登录、本机运行、worktree 与收尾规则。
- [x] 创建最新 `int_main` 同步 worktree 并预约端口槽位。
- [x] 安装/确认运行依赖并启动 worktree 前后端。
- [x] 执行注册证变更 E2E 验证并记录每个用例结果。
- [x] 对失败场景做代码与运行态原因分析。
- [x] 补充注册证审批通知回归测试，先复现短信路径失败。
- [x] 实现注册证审批通知不依赖短信/手机号。
- [x] 去除缺少注册证提醒任务配置时阻塞审批的限制。
- [x] 静态检查注册证审批链路是否还有同类配置/通知限制。
- [x] 运行定向后端测试、必要打包验证和真实页面审批续验。
- [x] 继续执行 E2E-6 到 E2E-9 并记录新的阻塞点。
- [x] 按用户选择实现候选审批人共享待办、选中候选人先审批即 claim。
- [x] 验证 E2E-6/E2E-7，记录 E2E-8/E2E-9 剩余阻塞。
- [x] 将缺少项目代码的注册证/变更批件文件下载申请改为可提交，并同步 24 小时验收文档。
- [x] 运行静态合同与定向回归验证。
- [x] 更新验证报告。

## Expected Verification

- Playwright 通过真实前端页面执行业务动作，业务动作不得由 API、DB 或 mock 替代。
- 每个 E2E 用例单独记录 PASS / FAIL / BLOCKED、停止位置、页面证据、自然触发请求证据。
- 后端修复必须有 BDD、RED、GREEN 记录；注册证审批通过/驳回/待办/超时通知不得依赖短信手机号；缺少注册证提醒任务配置不得阻塞审批主流程；注册证候选审批人能共同看到同一个待办，非候选人不能抢占审批。
- 缺少项目代码时，注册证主文件和变更批件文件仍允许普通用户通过真实页面申请下载；下载授权有效期统一按 24 小时描述和验证。

## Current Status

completed - 已按用户授权完成 E2E-8 修复和 E2E-9 数据模拟验证，并融合进 `int_main`。主干先提交 `ca4a1fc33 feat: prepare DCC source governance manifests`，随后本任务以 `7afa6b11c 修复注册证变更下载E2E链路` 快进合入，当前 `int_main` HEAD 为 `7afa6b11c`。目标静态合同、BPM/DCC 后端单测和 `git diff --check` 均通过；closeout apply 已清理任务证据并移除 Git worktree 注册。物理目录仍残留任务前端 `node_modules` 缓存，递归删除命令被本地策略拦截，需手工清理。

## Design Constraints Check

- No fallback: 不切换租户、端口、数据源或 mock 路径；E2E-9 仅在用户明确授权后模拟授权过期时间，未用 DB/API 代替提交、审批或下载业务动作。
- Code modification scope: 修改 BPM 注册证流程通知路由、注册证业务事件通知对缺失提醒任务配置的处理、注册证候选审批人待办可见性与 claim 校验、缺少项目代码时下载申请的前后端限制，以及对应测试和验收文档。
- Frontend-only business actions: 提交、审批、下载申请、下载均必须由真实页面操作触发。
- Worktree isolation: 使用 worktree `D:\IntRuoyiWorktree\20260904-registration-change-e2e-sync`，profile `int_main`，slot `20`，端口 `8154/48154`。
