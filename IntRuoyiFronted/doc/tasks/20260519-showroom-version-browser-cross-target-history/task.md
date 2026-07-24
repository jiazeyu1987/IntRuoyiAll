# 任务：展厅版本页跨目标历史视图补齐

## 目标

把当前展厅后台 `/showroom/history` 从“仅公司版本历史承接页”提升为更接近 workbook / 设计文档基线的跨目标版本浏览器，至少统一承接公司与产品两类真实历史，并对讲解 / 预览资产缺失的历史契约做显式暴露。

## 前置任务检查

- 上一个前端任务：`yudao-ui-admin-vue3/doc/tasks/20260519-showroom-owner-company-mapping-log-fix/task.md`
- 启动前状态：已显式标记为 `blocked`，原因是用户优先级切换。
- 影响：本任务可以独立推进，但不得顺手接管产品归属日志修复。

## 已确认约束

- 设计文档 `doc/tasks/20260518-showroom-product-doc-package/docs/system/frontend-design.md` 定义了：
  - `/showroom/company/history`：公司版本历史与字段差异
  - `/showroom/product/:productId/history`：产品版本历史与字段差异
  - `/showroom/version`：公司、产品、讲解、预览资产的跨目标版本浏览器
- 当前真实后端契约只有：
  - `GET /showroom/company/history`
  - `GET /showroom/product/history?id={id}`
  - `GET /showroom/narration/get`
- 当前后端不存在：
  - `/showroom/version`
  - 讲解历史列表接口
  - 预览资产历史列表接口

## 里程碑

- [x] M1：完成前置任务检查并创建当前任务文档。
- [x] M2：记录 BDD 场景与后端契约缺口。
- [x] M3：补 RED 测试，证明当前版本页仍只覆盖公司历史、无法跨目标浏览。
- [x] M4：完成最小前端实现，统一承接公司/产品历史，并显式展示讲解/预览资产历史契约缺口。
- [x] M5：完成定向验证、证据更新与 cleanup 预览。

## 范围

- 重构或新增：
  - `src/views/showroom-admin/history/**`
  - `scripts/showroom-admin-version-browser-history.test.mjs`
- 必要时仅在当前目录内新增子组件、contracts 与只读说明。

## 非范围

- 不修改后端 Java 代码。
- 不新增不存在的后台接口。
- 不伪造讲解历史、预览资产历史或跨目标聚合数据。
- 不接管产品归属日志修复、讲解工作台编辑逻辑、审批中心逻辑。
- 尽量不修改当前已有脏改动较多的 `src/views/showroom-admin/index.vue`、`src/api/showroom-admin/index.ts`。

## 写入边界

- `src/views/showroom-admin/history/**`
- `scripts/showroom-admin-version-browser-history.test.mjs`
- `doc/tasks/20260519-showroom-version-browser-cross-target-history/**`

## 预期验证

- `D:\Programs\node.exe --test scripts/showroom-admin-version-browser-history.test.mjs`
- `pnpm exec eslint src/views/showroom-admin/history scripts/showroom-admin-version-browser-history.test.mjs`

## 完成定义

- `/showroom/history` 不再只显示公司历史，而是提供跨目标浏览入口。
- 公司与产品历史都可在版本页内统一浏览真实 revision / diff 数据。
- 讲解与预览资产因缺少历史列表契约而无法达到 workbook 完整水平时，页面必须明确暴露“接口未补齐”，不能伪造历史列表。
- BDD / RED / GREEN 证据持续记录在 `execution-log.md`。

## 当前状态

已完成：`/showroom/history` 现已承接公司与产品历史，并对讲解/预览资产历史缺口做显式提示；但由于后端仍未提供 `/showroom/version`、讲解历史列表、预览资产历史列表，本任务只能把前端补齐到“跨目标浏览器壳 + 真实可用子链路”的水平。

## Current Status

completed

## 最终验证

- PASS：`node --test scripts/showroom-admin-version-browser-history.test.mjs`
- PASS：`pnpm exec eslint src/views/showroom-admin/history scripts/showroom-admin-version-browser-history.test.mjs`

## 残余阻塞

- 后端仍未提供 `/showroom/version` 聚合接口。
- 后端仍未提供讲解历史列表接口。
- 后端仍未提供预览资产历史列表接口。
- 因此 narration / preview asset 仍只能显示“当前快照 + 明确缺口”，无法形成完整历史链路。

## 交付说明

- `src/views/showroom-admin/history/CompanyHistoryWorkbench.vue`
  - 从单一公司历史页升级为跨目标版本浏览器。
  - 产品历史改为可在版本页内直接浏览，不再只藏于产品详情内部抽屉。
  - 讲解与预览资产切换后，读取真实当前快照，并明确显示历史接口缺失。
- `src/views/showroom-admin/history/VersionDiffDrawer.vue`
  - 抽屉支持跨目标 diff 摘要，显示对象、版本与状态。
- `scripts/showroom-admin-version-browser-history.test.mjs`
  - 为当前任务新增独立 RED/GREEN 回归脚本，避免混入共享脏工作区中其他 showroom 任务的未收口断言。
