# Codex Runner token 无效或未配置

## Task Goal

修复系统管理测试管理页面点击“执行”时提示“Codex Runner token 无效或未配置”的问题，确保执行入口使用当前有效的 Runner 配置，并在配置确实缺失或失效时明确暴露根因。

## Milestones

1. 定位执行入口、Runner token 配置来源、后端校验和现有测试。
2. 用 BDD 场景建立先失败的回归测试，确认根因。
3. 实施最小根因修复并通过 GREEN。
4. 完成相关前端/后端回归验证和真实页面路径验证（若环境前置条件齐备）。
5. `ready_for_closeout` 后执行 cleanup preview/apply，完成任务记录和推送。

## Expected Verification

- 任务专用回归测试先 RED 后 GREEN。
- 相关前端静态检查/构建或后端测试通过，或在日志中记录与本任务无关的既有阻塞。
- 若运行真实页面，必须核对前端入口、后端入口、测试租户、测试账号、Runner token、Codex CLI、Playwright 和 Runner heartbeat。
- `git diff --check` 通过，任务提交已推送到当前分支 `origin`，且分支不再 ahead。

## Applicable Experience Gate

### Codex Runner 自动测试门禁

- Trigger: 修改、运行或验收系统管理测试管理、Codex Runner、自然语言测试方法、检查点截图或由 Codex 调用 Playwright 的自动测试流程。
- Preflight check: 真实执行前确认前后端入口、目标测试租户、测试管理员账号、Runner token、Codex CLI、Playwright 浏览器、Runner 本地凭据映射和测试数据清理责任；后端用当前 token 完成注册探针，并核对执行中/空闲 heartbeat。
- Blocker: token 与后端运行态不一致、注册失败、heartbeat 过期、租户或测试数据前置条件缺失时停止；不得把 Runner 进程存在当作在线证明。
- Verification: 记录页面执行入口、Runner 注册/领取/执行期心跳/空闲心跳/回写证据、最终 UI 状态和必要的只读核验。
- Forbidden action: 禁止 API-only、mock 成功、离线跳过、裸调用 Codex CLI、前端硬拦截或顺序降级冒充真实 E2E。
- Evidence: `docs/e2e-rules.md` 的“Codex Runner 自动测试门禁”及相关历史任务验证报告。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；不增加降级路径，不隐藏 token 或 Runner 错误。
- 是否从根因和长期维护角度解决：是；先确认 token 配置契约和校验链路，再做最小修复。
- 是否存在临时补丁或绕过：否；不改用 API-only、mock 或默认成功。

## Current Status

in_progress
