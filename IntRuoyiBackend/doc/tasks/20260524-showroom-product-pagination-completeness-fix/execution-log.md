# 执行日志：展柜产品分页完整性契约修复

BDD: 展柜产品分页遇到缺少归属字段的产品时仍应继续渲染 -> Given 产品分页接口包含缺少 `owner_company_id`、`product_owner_type` 或 `lifecycle_stage` 的产品 / When 前端请求该页 / Then 后端必须把该产品标记为 `incomplete=true`，而不是以完整产品返回导致列表渲染崩溃。

BDD: 发布态产品完整性必须与列表契约一致 -> Given 产品已存在 live revision / When 当前 revision 缺少产品归属或生命周期关键字段 / Then 后端发布态快照与分页返回必须反映 `incomplete=true`，避免 live 数据在列表页被误判为完整。

RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest#productPageShouldMarkRowsMissingOwnershipFieldsAsIncomplete test` -> FAIL，断言 `row.incomplete()` 期望为 `true`，实际为 `false`，证明缺少归属字段的 live 产品仍被接口当成完整产品返回。

GREEN: 新增后端回归测试 `productPageShouldMarkRowsMissingOwnershipFieldsAsIncomplete`，并在 `ShowroomPublishContract` 中拆分“发布必填字段”与“完整性字段集”后，`mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest#productPageShouldMarkRowsMissingOwnershipFieldsAsIncomplete test` -> PASS。

GREEN: `mvn -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest#productPageShouldReturnTotalAndRespectRequestedPageSlice test` -> PASS，说明本次修复没有破坏产品分页切片行为。

INFO: 最终修复策略：
- 保持 `requiredProductPublishFields()` 不变，只继续约束 `name_cn / name_en`
- 新增 `requiredProductCompletenessFields()`，将 `owner_company_id / product_owner_type / lifecycle_stage` 纳入 `incomplete` 判定
- 草稿保存、revision 映射、发布后 master `incompleteFlag` 同步都按新完整性字段集计算
- 不新增 fallback，也不通过前端吞错隐藏坏数据；而是让后端明确把这类记录标成 `incomplete`

INFO: closeout preview -> `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260524-showroom-product-pagination-completeness-fix --mode preview`
- status: `ready`
- keep: `task.md`, `execution-log.md`
- delete / blocked / warnings: none

GREEN: 当前仓 `d2f8e7b184` 本地运行态验证 -> PASS
- `mvn -pl yudao-server -am -DskipTests package` -> PASS
- 当前修复版 `yudao-server.jar` 已启动到 `http://127.0.0.1:48081`
- 接口探针 `GET /admin-api/showroom/product/page?pageNo=3&pageSize=20` -> PASS，`product_049` 返回：
  - `incomplete=true`
  - `revision.incomplete=true`
  - `displayRevision.incomplete=true`

GREEN: 干净前端 worktree 真实翻页验证 -> PASS
- 为避免主前端仓脏工作区影响，使用临时 worktree：
  `D:\ProjectPackage\Int\IntRuoyi\worktrees\20260524-showroom-pagination-frontend-verify`
- `pnpm dev` 启动到 `http://127.0.0.1:8081`
- Playwright 脚本
  `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-product-pagination-diagnosis\scripts\inspect-showroom-product-pagination-live.mjs`
  验证结果：
  - `pageNo=3`：接口 `product_040..product_059`，DOM `product_040..product_059`
  - `pageNo=4`：接口 `product_060..product_079`，DOM `product_060..product_079`
  - `pageErrors=[]`
