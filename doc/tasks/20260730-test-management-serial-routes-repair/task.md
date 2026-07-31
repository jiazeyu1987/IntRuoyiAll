# 测试管理串行路线修复与复验

## Task Goal

定位并修复测试租户中 3 条正式串行路线无法完整跑完的问题，使 `工艺路线节点闭环`、`批记录节点闭环`、`智能排产节点闭环` 均可通过真实测试管理页面按顺序完整执行。

## Milestones

- [x] M1: 复现并隔离 Runner/Codex CLI 首节点退出与超时根因
- [x] M2: 建立失败回归测试并记录 RED
- [ ] M3: 实施最小正式修复并记录 GREEN（Runner 隔离、固定样本和 artifact 本地配置已补齐；等待运行态加载后复验）
- [ ] M4: 通过真实页面完整复验 3 条串行路线并完成任务自有清理
- [ ] M5: 完成经验沉淀、收尾校验、提交与推送

## Expected Verification

- 受控短预算 Codex CLI 自检返回结构化成功结果，不受错误插件认证或未知 feature 配置阻断。
- Runner 注册、领取、执行期心跳、结果回写和空闲心跳均正常，终态 `currentRunningCount=0`。
- 从 `系统管理 > 测试管理` 真实页面分别完整选择并顺序执行：
  - `工艺路线节点闭环`
  - `批记录节点闭环`
  - `智能排产节点闭环`
- 3 条路线的全部节点均完成，检查点通过，最终在 `系统管理 > 测试记录` 核对终态。
- 不使用 API-only、mock、人工跳过、Runner 离线跳过、降级执行或静默切换认证/模型/插件配置。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，先复现 Runner 实际命令与环境继承，再通过回归测试约束正式修复。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `docs/experience-index.md` 存在。
- Codex Runner 自动测试门禁：真实执行前必须验证注册、心跳、领取、结构化回写、Windows 子进程收敛和只读预算。
- Codex Runner 运行态重启与 CLI 自检门禁：Runner 在线不足以证明可执行；正式节点串前必须通过短预算 CLI 自检。
- 测试管理串行节点串门禁：完整连续选择，首节点失败时后续节点必须阻断；最终验收不能以阻断语义正确替代全链路通过。
- 测试管理测试节点闭环门禁：每个节点必须具备正式样本、页面操作、可见断言和清理恢复闭环。

## Cleanup Candidates

- output/playwright/20260730-test-management-serial-routes-repair/
- doc/tasks/20260730-test-management-serial-routes-repair/tmp/

## Cleanup Keep

doc/tasks/20260730-test-management-serial-routes-repair/run-serial-routes-real-e2e.mjs

## Current Blocker

- 后端已恢复 `UP`，但标准 `restart-int-ruoyi-local.ps1 -Component frontend` 启动的 Vite 在 `8081` 监听后，真实页面模块请求持续零字节超时。最小 Vite 诊断服务可返回 `200`，但不是本任务允许使用的正式运行态。执行 `40` 尚未通过真实“测试记录”页面取消；在用户明确授权改变 `VITE_OPTIMIZE_PROFILE=windows-safe` 启动路径前，不得切换非标准前端运行态或创建新批次。
