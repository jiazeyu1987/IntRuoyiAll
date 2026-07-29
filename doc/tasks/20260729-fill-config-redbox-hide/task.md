# 填写配置红框区域隐藏

## Task Goal

按用户截图反馈，修复“填写配置 / 辅助表单映射”页面红框区域仍显示的问题：隐藏顶部右侧“关闭 / 重新读取 / 保存填写配置”操作组、左侧原表单说明栏、中央辅助表单预览说明栏；保留原表格、辅助表格卡片、右侧映射控制栏与必要填写配置能力。

## Milestones

1. `in_progress`：确认截图对应组件、页面入口和既有静态合同。
2. `pending`：补充 RED 静态回归，锁定红框区域不显示且必要配置能力保留。
3. `pending`：实施最小前端修复，不引入 fallback 或静默降级。
4. `pending`：运行目标静态合同和相邻回归验证。
5. `pending`：更新证据、状态和剩余阻塞。

## Expected Verification

- `node tests/e2e/edhr-fill-config-redbox-hide-static.spec.js`
- 相邻填写配置静态合同（定位后补充具体命令）
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

- 待执行。
