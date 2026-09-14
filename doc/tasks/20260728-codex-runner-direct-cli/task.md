# Codex Runner direct CLI startup

## Task Goal

修复系统管理 > 测试管理按需启动 Codex Runner 时，因为本机前端入口 `http://127.0.0.1:8081` 不可达而直接失败的问题。用户要求改为“裸调/裸掉 Codex CLI”，本任务按“去掉启动脚本对前端入口的硬阻断，保留后端受控 Runner 与 Codex CLI 执行链路”实施。

## Milestones

- [x] 复现当前按需启动失败并定位前端入口检查来源。
- [x] 增加回归测试锁定启动脚本不得因前端入口不可达而阻断 Codex CLI/Runner 启动。
- [x] 修改最小启动链路，保留 token、后端和 Codex CLI 前置校验。
- [x] 运行目标回归验证并记录结果。
- [x] 提交并推送本任务相关前后端代码与任务文档。

## Expected Verification

- PowerShell/Node 静态或脚本回归测试先 RED 后 GREEN。
- 受影响后端/前端测试通过，至少覆盖 Codex Runner 按需启动脚本契约。
- Git 提交前检查 staged 文件清单，推送到 `origin/int_main`。

## Current Status

completed

## Applicable Gates

- Codex Runner 自动测试门禁：真实执行前必须确认 Runner token、Codex CLI、Playwright 浏览器和受控启动脚本；不得用 mock/API-only 冒充执行。
- 本地运行态门禁：不得随机换端口；`8081/48081` 属于 `E:\IntRuoyi` 的 `int_main`。
- PowerShell 编排门禁：不得使用 `&&`；测试命令逐条记录退出码。
- Git 提交门禁：提交前检查分支、remote、staged 文件清单；推送后确认不再 ahead。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。去除的是 Runner 启动脚本对前端入口的非必要硬阻断，不吞掉 Codex CLI、token 或后端注册失败。
- 是否从根因和长期维护角度解决：是。按需 Runner 启动不应依赖当前浏览器前端入口健康，执行时仍由测试方法里的真实页面路径暴露前端不可达问题。
- 是否存在临时补丁或绕过：否。

## Cleanup Keep

- doc/tasks/20260728-codex-runner-direct-cli/bug-regression-evidence.md
