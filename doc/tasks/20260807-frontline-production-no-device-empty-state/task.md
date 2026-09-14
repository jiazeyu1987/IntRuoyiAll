# Task: 一线生产填设备无设备空态

## Task Goal

当一线生产“填设备”区域没有可填设备时，在该区域内明确显示“无设备”，不使用默认成功、mock 设备或其它降级数据掩盖正式设备列表为空。

## Milestones

- [x] M1: 定位一线生产固定模板设备区域与正式设备数据来源。
- [x] M2: 先补充静态合同，验证无设备空态当前缺失并 RED。
- [x] M3: 最小实现“无设备”空态，不改变有设备时的参数填写链路。
- [x] M4: 运行目标合同、相关静态回归、类型检查或记录阻塞。
- [x] M5: 更新验证报告并完成收尾状态。

## Expected Verification

- `node tests/e2e/frontline-production-no-device-empty-state-static.spec.cjs`
- 受影响相邻静态合同（按定位结果选择）
- `pnpm ts:check`（若存在无关历史阻塞，记录首个阻塞点，不冒充通过）
- `git diff --check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260807-frontline-production-no-device-empty-state/frontend-feature-evidence.md`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务只展示正式设备列表为空时的空态，不构造替代设备。
- `是否从根因和长期维护角度解决`：是。空态挂在正式设备渲染分支上，保留有设备时的参数输入链路。
- `是否存在临时补丁或绕过`：否。

## Experience Gates

- 已读取 `docs/experience-index.md`。本任务命中前端用户可见空态与一线生产固定模板相关门禁，采用专用静态合同锁定目标区域，不用 mock/default 数据代替正式空列表。

## Cleanup Candidates

- doc/tasks/20260807-frontline-production-no-device-empty-state/frontend-feature-evidence.md
