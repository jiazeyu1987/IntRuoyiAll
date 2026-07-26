# 批记录源选择框缺少生产工单选项回归证据

## Bug Summary

- 用户截图反馈：批记录单元格链接页面左侧源选择框只显示批记录表单，例如“产品信息、硅化 I 工序生产记录”等，看不到“生产工单”选项。
- 期望行为：打开左侧源选择框时，应能直接选择“生产工单”；选择后左侧表格展示生产工单字段，用户再点击具体字段作为链接来源。

## Expected

- 源选择框本身必须直接包含“生产工单”选项。
- 选择“生产工单”后左侧字段矩阵必须可见且可选择具体字段。
- 只读验证不得在 `芋道源码/admin` 基线数据上保存规则或发送 MES 写请求。

## Reproduction

- Path: `批记录表单 -> 链接 -> 左侧源选择框`
- Screenshot evidence: `C:\Users\BJB110\AppData\Local\Temp\codex-clipboard-533d3eae-e378-4d25-bf52-e304e6158492.png`
- RED: `node tests\e2e\mes\batch-record-cell-link-static.spec.js` -> FAIL，静态合同缺少 `<el-option label="生产工单" :value="PRODUCTION_WORK_ORDER_SOURCE_REPORT_ID" />`。

## Root Cause

- 之前实现把“生产工单字段”放在独立的来源类型下拉里，源选择框本身仍然只列批记录表单。
- 用户实际操作路径关注的是截图中的源选择框，因此生产工单必须作为源选择框的一项，而不是依赖额外来源类型控件。

## Fix

- 将来源类型控件折叠进源选择框：源选择框第一项为“生产工单”，后续仍列出批记录表单。
- 选择“生产工单”后，内部 `sourceType` 切换为 `PRODUCTION_WORK_ORDER`，左侧字段矩阵继续展示生产工单字段。
- 保留生产工单字段点击、目标单元格点击、建立链接按钮启用和只读 E2E 无 MES 写请求的验证。

## Verification

- GREEN: `node tests\e2e\mes\batch-record-cell-link-static.spec.js` -> PASS。
- GREEN: `node --check tests\e2e\mes\batch-record-cell-link-work-order-field-readonly.e2e.mjs` -> PASS。
- GREEN: `node tests\e2e\mes\batch-record-cell-link-work-order-field-readonly.e2e.mjs` -> PASS，真实页面打开源选择框并点击“生产工单”，`forms=15`、`sourceFields=12`、`mesWriteRequests=0`。
- GREEN: `pnpm ts:check` -> PASS。

## Risk And Scope

- Scope: `IntRuoyiFronted/src/views/mes/pro/batchrecordcelllink/index.vue` 和对应静态/真实 E2E。
- Risk: 源选择框选项数量增加一项；目标表单排除逻辑仍只在批记录表单来源下排除同表单，生产工单来源下允许所有批记录表单作为目标。
- Non-goal: 未在 `芋道源码/admin` 基线数据上执行保存写入；保存链路由既有后端/静态合同覆盖，真实 E2E 保持只读。

## Blockers

- Closeout apply / ff-only merge / worktree removal 仍受主 worktree `E:\IntRuoyi` 并行脏改动阻塞。
- 本次回归修复自身无未解决验证 blocker。
