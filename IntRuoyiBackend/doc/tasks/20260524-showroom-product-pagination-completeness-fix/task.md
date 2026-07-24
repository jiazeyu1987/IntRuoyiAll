# 任务：展柜产品分页完整性契约修复

## Goal

修复 `展柜 -> 产品管理` 第 `3`、`4` 页切换时列表不更新的问题，正式收紧后端产品完整性契约，确保缺少 `owner_company_id`、`product_owner_type`、`lifecycle_stage` 的产品不会再以 `incomplete=false` 返回给前端，从而避免列表渲染阶段抛错并卡死在上一页。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\foundation\contract\ShowroomPublishContract.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\content\service\ShowroomPersistentContentService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\content\service\ShowroomContentService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomHttpApiIntegrationTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-product-pagination-completeness-fix\**`

## Non-Scope

- 不改分页组件 UI 样式或前端分页事件链路
- 不引入 fallback、静默吞错或“坏数据先跳过”的临时兼容分支
- 不顺手清理与本次缺陷无关的 showroom 历史数据

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-lipid-resistant-stopcock-list-card\task.md`
- Status before this task: `Completed`
- Impact on this task:
  上一同仓任务已完成，不阻塞本次代码修复任务；本任务只修改 showroom 后端契约与回归测试。

## Milestones

- [x] M1：记录诊断结论、建立任务文档和执行日志。
- [x] M2：先补 RED，锁定“缺少关键归属字段的产品必须返回 incomplete=true”的后端可观察行为。
- [x] M3：最小修改 showroom 产品完整性判定与发布态同步逻辑。
- [x] M4：运行定向回归验证，确认分页接口与真实前端复现链路恢复。
- [x] M5：更新证据、检查提交边界并完成 closeout 预览。

## Expected Verification

- `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest#productPageShouldMarkRowsMissingOwnershipFieldsAsIncomplete test`
- `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest#productPageShouldReturnTotalAndRespectRequestedPageSlice test`
- 如本地联动可用，补充真实前端路径复现：`http://127.0.0.1:8081/showroom/product`

## Current Status

- Completed on 2026-05-24.
- 已知诊断输入：
  - 第 `3` 页接口本身返回了新数据，但前端 DOM 停在第 `2` 页。
  - 首个触发异常的产品为 `product_049`。
  - 该产品缺少 `owner_company_id`，同时缺少 `product_owner_type`，但接口仍返回 `incomplete=false`。
  - 前端列表会把这类非 incomplete 行当成完整数据强校验并直接抛错。

## Completed Work

- 新增后端回归测试 `productPageShouldMarkRowsMissingOwnershipFieldsAsIncomplete`，先锁定 RED。
- 在 `ShowroomPublishContract` 中显式拆分：
  - `requiredProductPublishFields()` 继续只约束发布最低门槛 `name_cn / name_en`
  - `requiredProductCompletenessFields()` 作为列表/快照完整性判定，纳入 `owner_company_id / product_owner_type / lifecycle_stage`
- 更新 `ShowroomPersistentContentService` 与 `ShowroomContentService`：
  - 草稿保存时按新完整性字段集计算 `incomplete`
  - revision 映射时按新完整性字段集计算 `revision.incomplete`
  - 发布后同步把产品 master 的 `incompleteFlag` 刷成新完整性结果
- 保持发布校验不扩散，只继续阻止缺少 `name_cn / name_en` 的发布，避免影响现有 showroom 发布链路和大量既有测试基线。

## Verification Evidence

- RED:
  - `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest#productPageShouldMarkRowsMissingOwnershipFieldsAsIncomplete test`
  - 失败信息：`expected: <true> but was: <false>`
- GREEN:
  - `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest#productPageShouldMarkRowsMissingOwnershipFieldsAsIncomplete test`
  - `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest#productPageShouldReturnTotalAndRespectRequestedPageSlice test`
- Closeout preview:
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260524-showroom-product-pagination-completeness-fix --mode preview`
  - 结果：`ready`，仅保留 `task.md` 与 `execution-log.md`，无删除项、无阻塞项、无警告
- Runtime verification:
  - 当前仓 `d2f8e7b184` 已重新打包到本地 `48081`
  - 直接接口探针确认 `product_049` 在 `pageNo=3` 时返回：
    - `incomplete=true`
    - `revision.incomplete=true`
    - `displayRevision.incomplete=true`
  - 由于主前端仓存在不相关脏改动，本次使用干净前端临时 worktree
    `D:\ProjectPackage\Int\IntRuoyi\worktrees\20260524-showroom-pagination-frontend-verify`
    启动 `8081`
  - Playwright 真实翻页验证通过：
    - 第 `3` 页接口与 DOM 同步为 `product_040..product_059`
    - 第 `4` 页接口与 DOM 同步为 `product_060..product_079`
    - `pageErrors=[]`

## Risks / Blockers

- 当前前端仓存在大量不相关脏改动，本任务优先通过后端正式契约修复消除根因，避免把缺陷修复范围扩散到前端仓。
- 后续若需要把“发布门槛”也同步收紧到 `owner_company_id / product_owner_type / lifecycle_stage`，应另开任务评估对既有 showroom 发布测试和历史数据的影响。
