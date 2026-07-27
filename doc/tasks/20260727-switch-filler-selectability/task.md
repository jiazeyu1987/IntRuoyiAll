# 20260727 Switch Filler Selectability

## Task Goal

修复 eDHR 辅助填写模式“切换填写人”弹窗中非当前登录用户候选人被禁用、无法选择的问题，确保具备金手指/代填能力时可选择其他可填写候选人。

## Milestones

- [x] 建立隔离任务证据并记录 BDD/TDD 验证要求
- [x] 定位候选人禁用根因
- [x] 增加最小静态回归测试，先复现当前禁用逻辑
- [x] 实施最小前端修复并运行定向验证
- [x] 完成验证报告与收尾状态更新

## Expected Verification

- `node tests/e2e/edhr-switch-filler-selectability-static.spec.js` 先 RED 后 GREEN，覆盖“切换填写人”候选项不再被当前登录用户 ID 硬锁死。
- `node --check tests/e2e/edhr-switch-filler-selectability-static.spec.js` 通过。
- 若真实页面 E2E 前置条件不足，记录缺失前置条件和影响，不使用 API-only 或 mock 代替真实路径。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，修复候选人可选态判断的根因，继续保留后端 openTask fail-fast 校验。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 命中 `docs/e2e-rules.md#静态合同与真实 E2E 同步门禁`：当前任务为窄范围页面缺陷，新增或扩展聚焦静态合同覆盖当前行为，不顺手修改无关产品逻辑。
- 命中 `docs/e2e-rules.md#eDHR 单据填写人显示值门禁`：填写人候选项以详情接口返回的 `fillableUsers` 为准，不从当前登录人或历史配置名称推断。
- 命中 `docs/backend-development.md#eDHR 详情回填门禁`：若详情接口 `fillableUsers` 缺失才进入后端链路；本次截图已有候选人，根因限定在前端可选态。

## Cleanup Keep

- doc/tasks/20260727-switch-filler-selectability/bug-regression-evidence.md
- doc/tasks/20260727-switch-filler-selectability/frontend-feature-evidence.md
