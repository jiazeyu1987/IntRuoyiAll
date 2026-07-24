# 任务：展厅前端 F2 产品详情 / 展厅管理可操作化

## 目标

把展厅后台的产品管理与展厅管理从“只看列表”推进到“可操作工作台”，补齐产品详情编辑/历史入口，以及展厅 CRUD 和产品映射入口。

## 里程碑

- [x] 记录 BDD 与 TDD 目标
- [x] 实现产品详情 / 历史 / 展厅映射承接页
- [x] 运行测试与 lint
- [x] 更新任务记录并提交

## 范围

- 新增或重构：
  - `src/views/showroom-admin/product/**`
  - `src/views/showroom-admin/hall/**`
  - `src/views/showroom-admin/components/HallProductMappingDialog.vue`
- 复用已存在的 `ProductListTable` 和 `HallListTable`。

## 非范围

- 不修改 `src/router/modules/showroom.ts`
- 不修改 `src/views/showroom-admin/index.vue`
- 不实现审批、指派、讨论、讲解工作台
- 不修改后端 Java 代码

## 写入边界

- 仅允许写：
  - `src/views/showroom-admin/product/**`
  - `src/views/showroom-admin/hall/**`
  - `src/views/showroom-admin/components/HallProductMappingDialog.vue`
  - `scripts/showroom-admin-product-hall-operability*.mjs`
  - `doc/tasks/20260519-showroom-remediation-f2-admin-product-hall-operability/**`

## 依赖

- `B2` 需补齐产品详情、产品历史、展厅真实契约。
- 当前 `ProductListTable` / `HallListTable` 已存在，可以直接复用。

## 预期验证

- `node --test scripts/showroom-admin-product-hall-operability*.mjs`
- `pnpm exec eslint src/views/showroom-admin/product src/views/showroom-admin/hall src/views/showroom-admin/components/HallProductMappingDialog.vue`

## 完成定义

- 产品列表有真实“编辑/查看历史”承接入口。
- 展厅列表有真实“维护映射/编辑展厅”承接入口。
- 不再只有按钮 emit 而没有页面工作流承接。

## 当前状态

已完成 F2 worker 范围内交付，待 F5 做入口集成。

## 验证结果

- RED: `node --test scripts/showroom-admin-product-hall-operability*.mjs` 失败，原因是 `src/views/showroom-admin/product/**`、`src/views/showroom-admin/hall/**`、`src/views/showroom-admin/components/HallProductMappingDialog.vue` 尚不存在。
- GREEN: `node --test scripts/showroom-admin-product-hall-operability*.mjs` 通过，5 个测试全部通过。
- GREEN: `pnpm exec eslint src/views/showroom-admin/product src/views/showroom-admin/hall src/views/showroom-admin/components/HallProductMappingDialog.vue` 通过。

## 交付说明

- 新增了可复用的产品详情编辑对话框、产品历史抽屉、展厅编辑对话框和展厅产品映射对话框。
- 进一步补齐了 `ProductWorkbench.vue` 与 `HallWorkbench.vue`，把“编辑详情 / 查看历史 / 编辑展厅 / 维护映射”承接动作在组件层串成可接线的工作台壳。
- 组件严格依赖 B2 已稳定的真实后端契约：`/showroom/product/get`、`/showroom/product/history`、`/showroom/hall/update-product-mapping`。
- 按 task 边界，本 worker 未修改 `router`、`showroom-admin/index.vue`、`ProductListTable.vue`、`HallListTable.vue`；后续由 `F5` 负责入口接线和端到端联调。

## Remaining Blockers

- 当前按钮入口仍在 `src/views/showroom-admin/index.vue` 与列表组件之外，本 worker 无权改动；要让“编辑 / 查看历史 / 维护映射”真正从列表点开，必须由 `F5` 集成这些新组件。

## Cleanup Keep

- `doc/tasks/20260519-showroom-remediation-f2-admin-product-hall-operability/task.md`
- `doc/tasks/20260519-showroom-remediation-f2-admin-product-hall-operability/execution-log.md`

## 无上下文 LLM 提示词

```text
你在仓库 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 工作。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. 当前任务文档：
   D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-f2-admin-product-hall-operability\task.md
3. 设计文档：
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\product\prd.md
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\system\frontend-design.md
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\system\backend-api-design.md

目标：
- 落地产品详情/历史入口、展厅编辑/产品映射入口。
- 复用已有 ProductListTable / HallListTable。
- 不改 router，不改 showroom-admin/index.vue。

写入边界：
- src/views/showroom-admin/product/**
- src/views/showroom-admin/hall/**
- src/views/showroom-admin/components/HallProductMappingDialog.vue
- 你的测试脚本和 task 目录

要求：
- 严格 TDD。
- 如果后端接口不够，立即暴露 blocker，不要伪造交互成功。
- 不得改审批、指派、讨论、讲解相关页面。

完成后运行：
- node --test scripts/showroom-admin-product-hall-operability*.mjs
- pnpm exec eslint src/views/showroom-admin/product src/views/showroom-admin/hall src/views/showroom-admin/components/HallProductMappingDialog.vue
```
