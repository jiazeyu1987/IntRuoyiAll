# 批记录测试新增批记录映射页签

## Task Goal

在“批记录测试”页面的“订单分配”后新增内部页签“批记录映射”，把活跃订单放行资料生成 V4 需求整理为可查看、筛选、增删改和执行只读测试的条目。

## Milestones

- [x] 确认截图红框对应 `BatchRecordTestPage.vue` 内部页签位置。
- [x] 先补充“批记录映射”页签聚焦静态合同并记录 RED。
- [x] 新增页签、V4 需求条目和独立列表状态。
- [x] 更新相邻批记录测试合同并完成 GREEN/回归验证。
- [x] 完成页面结构、类型、格式和任务证据验证。
- [x] 完成任务清理并收尾。
- [x] 恢复正式本机后端运行前置并完成真实页面 Playwright 验证。

## Expected Verification

- `node tests/e2e/edhr-batch-record-test-mapping-static.spec.cjs`
- `node tests/e2e/edhr-batch-record-test-tab-static.spec.cjs`
- `node tests/e2e/edhr-batch-record-test-order-allocation-static.spec.cjs`
- `pnpm ts:check`
- `git diff --check -- IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchRecordTestPage.vue IntRuoyiFronted/tests/e2e/edhr-batch-record-test-mapping-static.spec.cjs IntRuoyiFronted/tests/e2e/edhr-batch-record-test-tab-static.spec.cjs doc/tasks/20260809-batch-record-mapping-tab`
- `python -X utf8 C:/Users/BJB110/.codex/skills/frontend-feature-delivery/scripts/validate_frontend_feature.py --evidence doc/tasks/20260809-batch-record-mapping-tab/frontend-feature-evidence.md`

## Current Status

completed

已完成第五个内部页签、15 条 V4 需求映射、独立筛选/分页/列状态和共享 CRUD/测试能力接入。正式本机 `8081/48081` 运行态健康，官方登录预检通过；真实 Playwright 页面验证覆盖 1440x900 与 1024x768，确认第五个 Tab、15 条内容、标题/描述换行、操作按钮边界、无页面/控制台/本机请求错误及无 MES 写请求。聚焦合同、相邻回归、TypeScript、Vite 模块转换、UTF-8、空白检查和 task-closeout-cleanup preview/apply 均通过；任务自有诊断脚本、截图和结果 JSON 已清理，三份核心任务记录保留。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。新增页签完整接入现有列表状态和持久化边界，不用静态文本块或 CSS 占位绕过正式列表能力。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

### 页面内部页签口径门禁

- 用户已明确“批记录测试”是页面，“批记录映射”是同页内部 Tab；不得新增 Excel 工作簿、主导航菜单或独立路由。
- 新页签必须具有独立 tab key、列表 `table-key`、查询、筛选、分页和列配置状态。
- 相邻四个页签的既有行为必须保留。

### 前端静态合同隔离门禁

- 使用任务专用静态合同锁定新增页签、需求条目和共享能力分派。
- 合同边界使用明确的相邻声明或稳定 DOM 标记，不以文件结尾作宽泛截取。

### UTF-8 门禁

- 中文任务文档和源码修改使用 `apply_patch`；验证时以 UTF-8 读取。

## Cleanup Keep

- doc/tasks/20260809-batch-record-mapping-tab/task.md
- doc/tasks/20260809-batch-record-mapping-tab/execution-log.md
- doc/tasks/20260809-batch-record-mapping-tab/verification-report.md
