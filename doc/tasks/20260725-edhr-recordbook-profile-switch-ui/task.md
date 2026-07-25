# eDHR 记录本全局开关个人中心 UI 清理

## Task Goal

删除个人中心 eDHR 记录本全局开关截图红框中的元信息块，并让蓝框中的开关区域整体可点击。

## Milestones

- [x] 创建任务记录、读取前端与静态合同门禁。
- [x] 更新静态合同，先覆盖红框删除和蓝框点击区域。
- [x] 修改个人中心记录本全局开关组件。
- [x] 运行相关静态验证和类型检查。
- [ ] 完成收尾、提交和推送。

## Expected Verification

- `node tests/e2e/edhr-recordbook-global-setting-static.spec.js`
- `pnpm ts:check`

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接调整组件结构和静态合同。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- `docs/frontend-development.md`：保持现有 Vue 3 / Element Plus 模式，不引入无关设计体系；接口错误仍需显式展示。
- `docs/e2e-rules.md#静态合同与真实-e2e-同步门禁`：修改静态合同后必须重跑目标静态合同，窄范围修复不得顺手改无关逻辑。
- `docs/powershell-memory.md#任务提交推送前置门禁`：提交前检查分支、remote、staged 清单；已有脏改动基线提交为 `b727bb0c`。

## Cleanup Keep

- `doc/tasks/20260725-edhr-recordbook-profile-switch-ui/frontend-feature-evidence.md`
- `doc/tasks/20260725-edhr-recordbook-profile-switch-ui/bug-regression-evidence.md`
