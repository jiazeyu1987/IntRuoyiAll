# 填写配置红框区域隐藏

## Task Goal

按用户截图反馈，修复“填写配置 / 辅助表单映射”页面红框区域仍显示的问题：隐藏顶部右侧“关闭 / 重新读取 / 保存填写配置”操作组、左侧原表单说明栏、中央辅助表单预览说明栏；保留原表格、辅助表格卡片、右侧映射控制栏与必要填写配置能力。

补充 2026-07-29 用户截图反馈：隐藏原表单格子和辅助表格格子内的次级说明行（如“未...”和“原...”），只保留格子主标题/字段名与映射能力。

## Milestones

1. `completed`：确认截图对应组件、页面入口和既有静态合同。
2. `completed`：补充 RED 静态回归，锁定红框区域不显示且必要配置能力保留。
3. `completed`：实施最小前端修复，不引入 fallback 或静默降级。
4. `completed`：运行目标静态合同和相邻回归验证。
5. `completed`：更新证据、状态和剩余阻塞。
6. `in_progress`：补充卡片次级说明隐藏 RED/GREEN 回归。

## Expected Verification

- `node tests/e2e/edhr-fill-config-redbox-hide-static.spec.js`
- `node tests/e2e/edhr-visual-fill-config-static.spec.js`
- `pnpm ts:check`

## Applicable Gates

- 前端静态契约隔离门禁：用聚焦静态合同覆盖本次红框隐藏范围，不用无关全量检查替代当前需求。
- PowerShell / UTF-8 门禁：中文任务文档和静态合同使用 UTF-8 写入。
- 无 fallback 门禁：只收敛目标 DOM 渲染边界，不隐藏错误、不吞异常、不改接口契约。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接收敛填写配置页目标区域的渲染边界。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress

## Final Verification Result

- `node tests/e2e/edhr-fill-config-redbox-hide-static.spec.js` -> PASS
- `node tests/e2e/edhr-visual-fill-config-static.spec.js` -> PASS
- `pnpm ts:check` -> PASS
- `task_closeout.py --task-id 20260729-fill-config-redbox-hide --mode preview` -> ready
- `task_closeout.py --task-id 20260729-fill-config-redbox-hide --mode apply` -> applied, deleted none
- Commit/push -> BLOCKED by unrelated concurrent dirty changes and branch ahead state
- 2026-07-29 补充反馈处理中：卡片次级说明隐藏尚待 RED/GREEN 与复验。

## Cleanup Keep

- doc/tasks/20260729-fill-config-redbox-hide/frontend-feature-evidence.md
- doc/tasks/20260729-fill-config-redbox-hide/bug-regression-evidence.md
