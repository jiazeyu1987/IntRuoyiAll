# 20260726 Batch Record Notebook Default Open

## Task Goal

在批记录/工艺路线配置右侧动态表单列表中隐藏“记录本”开关，并确保新增或既有表单配置默认按记录本开启处理。

## Milestones

- [ ] M1: 定位右侧动态表单列表的记录本开关、默认值来源和现有静态合同。
- [ ] M2: 先补充可复现的静态回归断言，记录 RED。
- [ ] M3: 最小化修改前端配置逻辑，隐藏记录本选项并默认开启。
- [ ] M4: 运行目标静态合同和相关回归验证，记录 GREEN/REGRESSION。
- [ ] M5: 完成任务证据、经验沉淀、cleanup 和提交推送。

## Expected Verification

- RED: 目标静态合同在当前实现下失败，证明“记录本”开关仍显示或默认值未开启。
- GREEN: 目标静态合同通过，证明右侧配置列表不再显示“记录本”开关且默认启用。
- REGRESSION: 受影响 eDHR/批记录静态合同通过，不破坏动态表单卡片与记录本配置保存。

## Current Status

in_progress

## Experience Gates

- `docs/frontend-development.md#前端静态契约隔离门禁`: 当前为窄范围 UI 行为修复；若宽合同存在无关历史失败，新增/运行任务专用静态合同，不顺手修改无关逻辑。
- `docs/e2e-rules.md#静态合同与真实-e2e-同步门禁`: 修改 `tests/e2e/*static.spec.js` 时需归一化 CRLF/LF，并让静态合同锁定真实页面行为。
- `docs/e2e-rules.md#eDHR-右侧红框元信息隐藏门禁`: 截图红框区域相关修改必须避免误删卡片内必要展示信息；本任务仅隐藏“记录本”配置开关。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是调整正式 UI 配置和默认值口径，不增加兼容分支。
- `是否存在临时补丁或绕过`：否。

