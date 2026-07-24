# Task: website-config 跳过缺少 live preview 的产品

## Goal

按用户明确要求调整 `GET /showroom/display/website-config` 聚合行为：当某个公共展厅产品缺少已发布 `live PRODUCT preview` 时，不再整包失败，而是仅跳过该产品并继续返回其他可展示产品。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-website-config-skip-missing-product-preview\**`

## Non-Scope

- 不修改 `Website` 前端代码
- 不恢复 mock，不增加前端降级分支
- 不改变 company / hall / narration / current revision 的 fail-fast 校验
- 不批量补齐缺失 preview 数据本身

## Previous Task Check

- 上一条同仓最新任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-product-032-showroom-cover-single-native\task.md`
- 启动前状态：`Blocked`
- 结论：该任务阻塞原因为上游图片生成 `503`，与本次 showroom 聚合后端改动无关，不阻塞本次新任务。

## User-Approved Fallback Scope

- 仅在 `website-config` 聚合产品列表阶段，对“缺少已发布 live PRODUCT preview”的产品执行跳过。
- 触发条件：产品映射进入 `website-config` 构建时抛出 `SHOWROOM_TARGET_NOT_FOUND: live product preview asset is required`。
- 风险：返回结果中的展厅产品数会变少，部分展厅可能变为空列表。
- 回滚/移除策略：当上游 preview 数据补齐后，可删除该跳过分支并恢复严格 fail-fast。

## Milestones

1. 建立任务文档并确认现有 500 原因为产品级 preview 缺失，而非整个接口结构错误。
2. 先写 RED 测试，锁定“缺 preview 产品应被跳过，聚合其余产品继续返回”的行为。
3. 实现最小后端改动，并保留清晰日志/注释标记该用户批准的局部降级。
4. 运行定向测试与真实运行验证，更新任务记录并收尾提交。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#websiteConfigShouldSkipProductsWhoseLivePreviewAssetIsMissingInsteadOfFailingWholeAggregate,ShowroomHttpApiIntegrationTest#publicityPublishProductShouldReuseCurrentRevisionBilingualNarrationCloseWholeAssignmentAndSkipApproval" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `GET http://127.0.0.1:48081/showroom/display/website-config`
- 浏览器刷新 `http://127.0.0.1:4173/`

## Current Status

- Status: Completed
- Completed work:
  - 已确认当前真实接口返回体为 `code=500 / SHOWROOM_TARGET_NOT_FOUND: live product preview asset is required`。
  - 已确认这是产品级数据触发的聚合失败，而不是整个接口结构错误：
    - 当前活动公共映射产品数 `165`
    - 其中缺少已发布 `PRODUCT preview` 的产品数 `164`
    - 缺少公开 `ZH/EN narration` 的产品数均为 `0`
  - 已确认首个实际触发 500 的产品是 `product_002(id=2)`，位于 `hall_id=1 / display_order=2`，其 `current_revision_id=1363` 但 `preview_source_revision_id=NULL`。
  - 已补 RED 集成测试，锁定“缺 preview 产品被跳过、有效产品继续返回”的目标行为。
  - 已在 `ShowroomApiRuntime.toWebsiteConfigShowroom(...)` 中实现用户批准的局部降级：
    - 仅当产品抛出 `SHOWROOM_TARGET_NOT_FOUND: live product preview asset is required` 时跳过该产品
    - 其他产品数据错误仍保持 fail-fast
    - 运行时会输出明确 `warn` 日志，标记被跳过的 `hallId / productId / displayOrder`
  - 已完成定向回归：
    - 直发链路的 live preview 同步测试仍通过
    - 新的 website-config skip 行为测试通过
  - 已完成真实运行验证：
    - 重新打包 `yudao-server` 并重启本地前后端
    - `GET http://127.0.0.1:48081/showroom/display/website-config` 已恢复为 `code=0`
    - 当前真实返回中保留 `8` 个展厅，合计返回 `1` 个有效产品，其余缺 preview 产品被跳过
    - 当前应用内浏览器中的 `http://127.0.0.1:4173/` 已不再停留在“展厅数据加载失败”错误页
- Remaining blockers:
  - None.

## Final Verification Result

- PASS: `mvn -pl yudao-module-showroom clean "-Dtest=ShowroomHttpApiIntegrationTest#websiteConfigShouldSkipProductsWhoseLivePreviewAssetIsMissingInsteadOfFailingWholeAggregate+publicityPublishProductShouldReuseCurrentRevisionBilingualNarrationCloseWholeAssignmentAndSkipApproval" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn -pl yudao-server -am -DskipTests package`
- PASS: `D:\ProjectPackage\Int\IntRuoyi\restart-ruoyi.bat`
- PASS: `GET http://127.0.0.1:48081/showroom/display/website-config` -> `code=0`, `showrooms=8`, `total_products=1`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-website-config-skip-missing-product-preview --mode preview`
