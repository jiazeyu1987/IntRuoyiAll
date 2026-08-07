# 排产工单来源工单号显示已完成标识

## Task Goal

在“排产工单”页签中，对已完成的排产工单，在“来源生产工单号”文本后追加“(已完成)”；未完成工单保持原展示。

## Milestones

- [x] M1：冻结现有展示契约并建立 RED 静态测试。
- [x] M2：实现已完成标识并取得 GREEN。
- [ ] M3：完成类型检查和真实页面只读验证。
- [ ] M4：完成清理、提交与推送。

## Expected Verification

- `node tests/e2e/mes-schedule-order-completed-source-label-static.spec.cjs`
- `pnpm ts:check`
- Playwright 本机真实页面只读验证：已完成行显示“(已完成)”，未完成行不显示该标识，MES 写请求为 0。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260807-schedule-order-completed-label/frontend-feature-evidence.md`
- `git diff --check`

## Applicable Experience Gates

- 使用任务专用最小静态契约完成 RED/GREEN，避免无关历史失败掩盖当前行为。
- Playwright 必须按可见业务唯一文本定位目标行；本次只读验证不得发送 MES 写请求。
- PowerShell 中每条验收命令单独执行并记录退出码，不能用后续 PASS 掩盖前序失败。
- 提交前先隔离并提交任务开始前的既有脏工作区基线，本任务文件不得混入基线。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；直接依据正式排产工单完成状态生成展示文本。
- 是否存在临时补丁或绕过：否。

## Current Status

in_progress：目标合同与相邻合同验证完成，正在执行类型检查和真实页面只读验证。
