# 任务：展厅前端 F1 公司工作台 / Dashboard / 版本历史

## 目标

把展厅后台中与公司内容管理相关的前端能力补齐到设计文档基线：包含 Dashboard、公司编辑工作台、公司版本历史与差异查看。

## 里程碑

- [x] 记录 BDD 与 TDD 目标
- [x] 实现 Dashboard / 公司工作台 / 版本历史页面
- [x] 运行测试与 lint
- [x] 更新任务记录并提交

## 范围

- 新增或重构以下页面/组件：
  - `src/views/showroom-admin/dashboard/**`
  - `src/views/showroom-admin/company/**`
  - `src/views/showroom-admin/history/**`
  - 相关仅前端展示组件，例如 `VersionDiffDrawer.vue`
- 对接设计文档中的公司结构化字段、Dashboard 汇总项、公司版本历史行为。

## 非范围

- 不修改 `src/router/modules/showroom.ts`
- 不修改 `src/views/showroom-admin/index.vue`
- 不实现产品详情、展厅映射、审批、指派、讨论、讲解工作台
- 不修改后端 Java 代码

## 写入边界

- 仅允许写：
  - `src/views/showroom-admin/dashboard/**`
  - `src/views/showroom-admin/company/**`
  - `src/views/showroom-admin/history/**`
  - `scripts/showroom-admin-company-dashboard-history*.mjs`
  - `doc/tasks/20260519-showroom-remediation-f1-admin-company-dashboard-history/**`

## 依赖

- 设计依赖：
  - `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\product\prd.md`
  - `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\system\frontend-design.md`
  - `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\system\backend-api-design.md`
- 后端依赖：
  - `B2` 至少补齐公司 current/history/dashboard 所需契约；若缺失，必须失败并记录阻塞。

## 预期验证

- `node --test scripts/showroom-admin-company-dashboard-history*.mjs`
- `pnpm exec eslint src/views/showroom-admin/dashboard src/views/showroom-admin/company src/views/showroom-admin/history`

## 完成定义

- 公司页不再是摘要占位，而是结构化编辑/展示工作台。
- 有单独 Dashboard 视图，展示设计文档定义的汇总信息。
- 版本历史页可看到版本列表与字段差异视图入口。
- 全程记录 BDD / RED / GREEN 到 `execution-log.md`。

## 当前状态

已完成：F1 worker 范围内交付、验证、cleanup 预览与任务提交均已完成；剩余后端契约缺口已在 Dashboard 中以显式不可用状态暴露，待后续后端/集成任务收口。

## Current Status

completed

## 阻塞记录

- 已解除：
  - `/showroom/assignment/page` 与 `/showroom/assignment/get` 已在后端 `ShowroomAdminController` 存在；Dashboard 的补充指派待办数量可从真实接口读取，之前“assignment/page 缺失”的判断已撤销。
  - `F2` 已按同一模式交付 `product/**`、`hall/**` 独立工作台并交由 `F5` 集成，因此 `showroom-admin/index.vue` 当前未挂载新页面不构成 F1 自身 blocker。
- 已显式保留：
  - Dashboard 精确“讲解音频陈旧 / stale narration assets”汇总仍缺稳定契约。当前后端未提供 `/showroom/dashboard` 聚合接口，也未提供后台 `GET /showroom/narration/get` 一类读取接口；`ProductDetailRespVO.narrations` 仅暴露 `status/live/audioReady`，不足以区分“无音频”与“脚本更新后音频已陈旧”。
  - 当前实现未伪造该统计，而是在 Dashboard 卡片和告警中明确标记“统计暂不可用 / 待后端契约补齐”。

## 本轮验证

- RED: `node --test scripts/showroom-admin-company-dashboard-history*.mjs` 失败，原因是 `company/**`、`history/**`、`dashboard/**` 与测试脚本产物尚不存在。
- GREEN: `node --test scripts/showroom-admin-company-dashboard-history*.mjs` 通过，6 个测试全部通过。
- GREEN: `pnpm exec eslint src/views/showroom-admin/dashboard src/views/showroom-admin/company src/views/showroom-admin/history` 通过。

## 最终验证结果

- 任务结果：通过
- 代码提交：`f4167877`
- 残余说明：Dashboard 的“讲解音频陈旧数”仍等待后端精确契约，但前端已按 fail-fast 约束显式暴露不可用状态，没有伪造统计。

## 交付说明

- 新增了 `src/views/showroom-admin/company/**`，提供结构化公司字段工作台、差异预览、保存草稿与提交审批承接。
- 新增了 `src/views/showroom-admin/history/**`，提供公司版本历史列表与 `VersionDiffDrawer.vue` 字段级差异查看。
- 新增了 `src/views/showroom-admin/dashboard/**`，通过真实分页接口统计展厅数、产品数、资料未完善产品数、待审批数、补充指派待办数。
- 对于尚无后端精确契约的“讲解音频陈旧数”，页面显式展示不可用状态，没有用近似规则或伪数据替代。
- 按写入边界，本 worker 未修改 `src/router/modules/showroom.ts`、`src/views/showroom-admin/index.vue` 与现有 API wrapper；后续由 `F5` 负责入口集成。

## Cleanup Keep

- `doc/tasks/20260519-showroom-remediation-f1-admin-company-dashboard-history/task.md`
- `doc/tasks/20260519-showroom-remediation-f1-admin-company-dashboard-history/execution-log.md`
- `scripts/showroom-admin-company-dashboard-history.test.mjs`

## 无上下文 LLM 提示词

```text
你在仓库 D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 工作。

先读：
1. D:\ProjectPackage\Int\IntRuoyi\AGENTS.md
2. 当前任务文档：
   D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-remediation-f1-admin-company-dashboard-history\task.md
3. 设计文档：
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\product\prd.md
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\system\frontend-design.md
   - D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260518-showroom-product-doc-package\docs\system\backend-api-design.md

任务目标：
- 实现展厅后台的 Dashboard、公司工作台、公司版本历史页面。
- 不要改 router，不要改 showroom-admin/index.vue，不要碰产品/展厅/审批/指派/讨论/讲解页面。

写入边界：
- 仅可修改 src/views/showroom-admin/dashboard/**
- 仅可修改 src/views/showroom-admin/company/**
- 仅可修改 src/views/showroom-admin/history/**
- 仅可修改你的 task 目录和你的测试脚本

硬约束：
- 严格 TDD：先写失败测试，再最小实现，再回归。
- 不允许 mock 成功、静默降级、伪数据兜底。
- 如果后端契约不足以完成页面，立即失败并在 execution-log.md 记录 blocker。
- 不要回退其他人的改动，不要修改未授权路径。

交付物：
- 页面组件
- 最小测试
- task.md / execution-log.md 更新

完成后运行：
- node --test scripts/showroom-admin-company-dashboard-history*.mjs
- pnpm exec eslint src/views/showroom-admin/dashboard src/views/showroom-admin/company src/views/showroom-admin/history
```
