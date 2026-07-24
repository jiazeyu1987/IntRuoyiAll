# 任务：展厅前端 F3 审批 / 指派 / 讨论 / 讲解工作台

## 目标

补齐审批中心、补充指派、产品讨论、讲解工作台四类后台页面，使其从摘要占位升级为真实可操作工作台。

## 里程碑

- [x] 记录 BDD 与 TDD 目标
- [x] 实现审批 / 指派 / 讨论 / 讲解子页面
- [x] 运行测试与 lint
- [x] 更新任务记录并提交

## 范围

- 新增或重构：
  - `src/views/showroom-admin/approval/**`
  - `src/views/showroom-admin/assignment/**`
  - `src/views/showroom-admin/discussion/**`
  - `src/views/showroom-admin/narration/**`
  - 相关组件：`ApprovalTaskPanel`、`FieldAssignmentDialog`、`ProductDiscussionPanel`、`NarrationWorkspace`

## 非范围

- 不修改 `src/router/modules/showroom.ts`
- 不改产品/展厅详情页
- 不改前台展示页

## 写入边界

- 仅允许写：
  - `src/views/showroom-admin/approval/**`
  - `src/views/showroom-admin/assignment/**`
  - `src/views/showroom-admin/discussion/**`
  - `src/views/showroom-admin/narration/**`
  - `src/views/showroom-admin/index.vue`
  - `src/api/showroom-admin/index.ts`
  - `scripts/showroom-admin-workflow-workbenches*.mjs`
  - `scripts/showroom-admin-frontend.test.mjs`
  - `doc/tasks/20260519-showroom-remediation-f3-admin-workflow-workbenches/**`

## 依赖

- `B3/B4/B5` 至少提供审批、指派、讨论、讲解的真实 API 契约。

## 预期验证

- `node --test scripts/showroom-admin-workflow-workbenches*.mjs`
- `pnpm exec eslint src/views/showroom-admin/approval src/views/showroom-admin/assignment src/views/showroom-admin/discussion src/views/showroom-admin/narration`

## 完成定义

- 审批中心不再是待办数字占位，而是列表和详情工作台。
- 指派页能看到待办/状态，不再只是“站内信”摘要。
- 讨论页支持线程查看。
- 讲解页支持稿件/音频/预览资产工作流承接。

## 当前状态

已完成。

## Current Status

completed

## 阻塞说明

- 历史阻塞已解除：用户追加批准修改 `src/views/showroom-admin/index.vue`，四个工作台现已接入真实后台壳页入口。

## 核实结果

- B5 任务文档状态已过期，但源码事实显示讲解/预览资产后端能力已存在，不再视为本任务阻塞：
  - `ShowroomAdminController` 已提供 `GET /showroom/narration/get`
  - `ShowroomApiRuntime.getNarration(...)` 已走持久化讲解读取
  - `ShowroomApiRuntime.previewImageUrl(...)` 已从 `showroom_preview_asset_version` 组装 live file URL
- `scripts/showroom-admin-workflow-workbenches*.mjs` 现已补齐为 `scripts/showroom-admin-workflow-workbenches.test.mjs` 并通过验证。
- 当前 admin controller 仍未暴露 preview asset 写入/选择接口，因此讲解工作台对预览资产采用真实 live 状态只读承接，不伪造上传或发布动作。

## 验证结果

- PASS: `node --test scripts/showroom-admin-workflow-workbenches*.mjs`
- PASS: `pnpm exec eslint src/views/showroom-admin/approval src/views/showroom-admin/assignment src/views/showroom-admin/discussion src/views/showroom-admin/narration src/views/showroom-admin/index.vue src/api/showroom-admin/index.ts scripts/showroom-admin-workflow-workbenches.test.mjs scripts/showroom-admin-frontend.test.mjs`
- PASS: `node --max-old-space-size=8192 node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260519-showroom-remediation-f3-admin-workflow-workbenches --mode preview`

## 无上下文 LLM 提示词

```text
你在仓库 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 工作。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. 当前任务文档：
   D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-f3-admin-workflow-workbenches\task.md
3. 设计文档：
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\product\prd.md
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\system\frontend-design.md
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\system\backend-api-design.md

目标：
- 实现审批中心、补充指派、产品讨论、讲解工作台。
- 不改 router，不改 showroom-admin/index.vue。

写入边界：
- src/views/showroom-admin/approval/**
- src/views/showroom-admin/assignment/**
- src/views/showroom-admin/discussion/**
- src/views/showroom-admin/narration/**
- 你的测试脚本和 task 目录

要求：
- 严格 TDD。
- 缺接口就失败并记录 blocker，禁止伪造数据。
- 不触碰产品/展厅详情页和前台页。

完成后运行：
- node --test scripts/showroom-admin-workflow-workbenches*.mjs
- pnpm exec eslint src/views/showroom-admin/approval src/views/showroom-admin/assignment src/views/showroom-admin/discussion src/views/showroom-admin/narration
```
