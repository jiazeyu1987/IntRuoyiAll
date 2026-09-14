# 测试管理 3 个串行路线验证

## Task Goal

验证 `系统管理 > 测试管理` 下测试租户中的 3 条正式串行路线是否都能通过真实前端路径完整跑完，并给出 PASS / BLOCKED / FAIL 结论。

## Milestones

- [x] M1: 核对测试管理、登录、Runner、本机前后端、Playwright 与测试租户前置条件
- [x] M2: 通过真实测试管理页面确认 3 条目标串行路线与节点完整性
- [x] M3: 分别执行 3 个串行路线并记录执行结果、检查点和清理状态
- [x] M4: 汇总验证报告并标记最终状态

## Expected Verification

- 使用本机前端 `http://127.0.0.1:8081` 或 `http://localhost:8081` 真实页面路径登录测试租户。
- 在 `系统管理 > 测试管理` 中筛选项目 `工艺路线`、测试租户 `芋道源码`、串行路线 `工艺路线节点闭环`。
- 验证 3 条正式串行路线分别为：
  - `工艺路线节点闭环`
  - `批记录节点闭环`
  - `智能排产节点闭环`
- 每条路线必须按节点串顺序完整执行，不能用 API-only、静态合同、人工跳过、Runner 离线跳过或顺序执行降级代替。
- 记录 Runner 注册、领取、执行期心跳、结构化回写、检查点结果、最终 UI 状态和清理闭环。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务仅做真实路径验证并按项目门禁暴露阻塞。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- Codex Runner 自动测试门禁：真实执行前必须确认本机前端/后端入口、目标测试租户、测试管理员账号、Runner token 或 tokenless 模式、Codex CLI、Playwright 浏览器、Runner 本地凭据映射、注册/领取/心跳/结构化回写；不得用 Runner 进程存在、API-only、静态合同或离线跳过替代真实执行。
- 测试管理串行节点串门禁：必须验证节点串字段、页面筛选、串内排序、完整连续选择、前置失败阻断和真实页面清理闭环；不得把 Runner 单并发、人工只选首节点、前端排序或后续手工取消当作串行能力。
- 测试管理测试节点闭环门禁：每个测试节点必须包含固定样本或任务自有测试标识、前置复位、页面操作、页面可见验证和清理/恢复方式；缺任一闭环条件必须阻塞。

## Cleanup Candidates

- doc/tasks/20260730-test-management-serial-routes-verification/serial-routes-real-e2e.cjs
- output/playwright/20260730-test-management-serial-routes-verification/

## Final Verification

- Task outcome: completed verification.
- Product verdict: FAIL，3 条正式串行路线当前均不能完整跑完。
- Cleanup: preview/apply 均通过，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- Git: 验证证据提交 `d814e7fa` 已推送到 `origin/int_main`；最终收尾记录单独提交并推送。
