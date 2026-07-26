# Codex Runner 长期可用性治理

## Task Goal

- 解决测试管理执行时反复出现 `没有在线 Codex Runner` 的问题。
- 从根因上补齐 Runner 启动前置检查、在线状态诊断、自动启动或明确失败说明，避免靠人工重启或临时打补丁。

## Milestones

- [ ] 复现当前 Runner 离线判定和用户可见错误。
- [ ] 梳理 Runner 注册、心跳、领取任务、前端执行按钮和本机启动脚本链路。
- [ ] 补充 RED 回归测试，覆盖无 Runner、Runner 过期、Runner 可启动/不可启动时的行为。
- [ ] 实现长期方案：启动/探测/诊断/运行态状态闭环。
- [ ] 运行后端、前端、真实页面或接口验证，并记录证据。

## Expected Verification

- 后端或前端静态/单元测试先 RED 后 GREEN。
- Runner 在线状态接口能区分 `在线`、`离线但可启动`、`离线且缺前置条件`。
- 页面执行测试项前能给出可操作状态，不再只反复提示 `没有在线 Codex Runner`。
- 若本机缺 Codex CLI、token、Node 或启动脚本，必须 fail fast 显示具体缺失项。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；本任务不允许把 Runner 离线伪装成成功。
- `是否从根因和长期维护角度解决`：是；目标是补齐 Runner 生命周期和可诊断运行态。
- `是否存在临时补丁或绕过`：否；若发现必须依赖人工启动，将记录为 blocker 而不是伪通过。

## 经验门禁

- Codex Runner 自动测试门禁：执行前必须确认本机前后端入口、目标测试租户、Runner token、Codex CLI、Playwright 浏览器、Runner 本地凭据映射；Runner loop 必须持续 heartbeat。
- 本地运行态门禁：不得强杀未知进程、不得随机换端口、不得只看 health 或未登录响应就宣称修复加载。
- 前端/后端 strict no-fallback：缺少 Runner 前置条件时必须明确展示缺失项，不允许默认成功或静默降级。
