# 任务：展柜后台产品分页切换异常排查

## Goal

分析 `芋道源码 / admin` 登录进入 `展柜 -> 产品管理` 后，点击列表底部分页 `1 / 2 / 3 / 4` 时，切到第 `3`、`4` 页列表不再变化的原因，并明确问题是在前端分页状态、接口请求、后端分页切片还是数据排序。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\components\ProductListTable.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\showroom-admin\index.ts`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\admin\ShowroomAdminController.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\content\service\ShowroomPersistentContentService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\dal\mysql\content\ShowroomProductMapper.java`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-product-pagination-diagnosis\**`

## Non-Scope

- 不在本任务中直接修复分页问题，除非排查中确认必须做最小修复且用户明确要求继续
- 不改动与分页无关的 showroom 产品管理功能
- 不引入 fallback、mock、静默降级或兼容分支

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-product-cover-prompt-management\task.md`
- Status before this task: `In progress with explicit blocker recorded`
- Impact on this task:
  上一任务已在任务文档中明确记录真实验证阻塞和脏工作区边界；本次仅做分页异常分析，不覆盖其在途源码改动，也不混入其提交边界。

## Milestones

- [x] M1：核对同仓前置任务状态并建立本任务文档、执行日志。
- [x] M2：复核前后端分页实现与现有测试，梳理分页事件、请求参数和接口切片逻辑。
- [x] M3：通过真实登录路径复现 `1 / 2 / 3 / 4` 页切换，抓取网络请求与返回差异。
- [x] M4：汇总根因、影响范围和最小修复方向，回写证据。

## Expected Verification

- `node --test yudao-ui-admin-vue3/scripts/showroom-admin-frontend.test.mjs`
- Playwright 真实路径复现：从 `http://127.0.0.1:8081/login` 登录后进入 `showroom/product`
- 如需后端佐证，补充接口或数据库查询证据

## Current Status

- Completed on 2026-05-24.
- 当前已完成：
  - 确认前端分页事件链路存在：`ProductListTable -> page-change -> handleProductPageChange -> loadProductRows`
  - 确认后端接口入口：`GET /showroom/product/page`
  - 确认后端代码存在基础分页切片实现与分页集成测试
  - 通过本地 `127.0.0.1:8081 -> 48082` 真实复现链路确认：
    - 第 `3`、`4` 页请求都发出了不同 `pageNo`
    - 后端接口返回的 `list` 已切换到新页数据
    - 前端表格 DOM 却停留在第 `2` 页
  - 确认触发渲染中断的首个坏数据为 `product_049`
  - 确认根因是“前后端完整性契约不一致”：
    - 后端 `ShowroomPublishContract` 只把 `name_cn` / `name_en` 当作产品完整性必填
    - 前端列表渲染却把 `owner_company_id` 当作非 incomplete 行的必填字段
    - `product_049` 缺少 `owner_company_id`，但后端仍返回 `incomplete=false`
    - 翻到第 `3` 页时 `resolveOwnerCompanyId()` 抛错，Vue 渲染更新中断，因此页面看起来“点到 3、4 了，但列表不变”

## Verification Evidence

- PASS: `node --test scripts/showroom-admin-frontend.test.mjs`
- PASS: 本地后端 `http://127.0.0.1:48082/admin-api/showroom/product/page?pageNo=1..4&pageSize=20` 返回四批不同分页数据
- PASS: Playwright 诊断脚本
  `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-product-pagination-diagnosis\scripts\inspect-showroom-product-pagination-live.mjs`
  证明：
  - `pageNo=3` 响应首条已变为 `product_040`
  - `pageNo=4` 响应首条已变为 `product_060`
  - 但 DOM 仍停留在第 `2` 页的 `product_021..product_039`
  - 浏览器 `pageerror` 首个异常为：`产品列表第 10 行字段为空：owner_company_id`
- PASS: 直接接口核对 `product_049`
  - 页码：第 `3` 页第 `10` 条（1-based）
  - `revision.fields.owner_company_id` 缺失
  - `displayRevision.fields.owner_company_id` 缺失
  - 但接口仍返回 `incomplete=false`

## Risks / Blockers

- 当前前端仓为脏工作区，`src/views/showroom-admin/index.vue` 等文件已存在上一任务未提交改动，本任务分析阶段不得覆盖。
- 用户截图使用的实际入口与正式环境 `admin` 菜单不完全一致；本次根因结论来自当前源码、本地真实运行态和后端真实分页数据交叉验证。
