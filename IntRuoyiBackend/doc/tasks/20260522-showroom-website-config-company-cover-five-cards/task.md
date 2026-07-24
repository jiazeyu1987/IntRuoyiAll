# 任务：展厅 website-config 公司封面与 5 张卡片对齐

## Goal

修复 `GET /showroom/display/website-config` 的公司信息聚合逻辑，使其：

- `company.homeImageUrl` 直接返回公司信息里的 `cover_image`；
- `company.publicFields` 与 `company.bilingualPublicFields` 只返回公司工作台可见的 5 个字段；
- 不再把公司预览资产当作首页/详情页封面来源。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomAppConfigCompanyFieldsContractTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomHttpApiIntegrationTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-website-config-company-cover-five-cards\**`

## Non-Scope

- 不修改 `Website` 前端代码
- 不修改公司编辑页 UI
- 不修改产品详情 contract
- 不向数据库写入新业务数据

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-batch-narration-script-recovery\task.md`
- Status before this task: `Blocked`
- Impact: 旧任务已显式阻塞，允许开始本次 website-config contract 修复。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在大量未提交的 MES / showroom 在途改动。
- Impact: 本任务只允许修改 website-config 公司聚合逻辑、定向测试与本任务文档，不覆盖无关改动。

## Milestones

1. 建立任务文档并锁定公司封面与 5 卡片 contract。
2. 先补 RED，覆盖封面来源改为 `cover_image`、字段缩减为 5 项。
3. 最小实现 `ShowroomApiRuntime` 聚合逻辑修复。
4. 跑定向回归，更新任务记录并执行 closeout preview。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomAppConfigCompanyFieldsContractTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-website-config-company-cover-five-cards --mode preview`

## Current Status

- Status: Completed on 2026-05-23
- Completed work:
  - 已确认 live `website-config.company.homeImageUrl` 当前来自公司预览资产，而非公司信息 `cover_image`
  - 已确认 live `company.publicFields` / `bilingualPublicFields` 当前返回 7 项，而非公司工作台可见的 5 项
  - 已将 `website-config.company.homeImageUrl` 改为直接返回公司信息 `cover_image`
  - 已将 `website-config.company.publicFields` 与 `company.bilingualPublicFields` 收敛为 5 项可见字段
  - 已重建 `yudao-server.jar` 并重启本地 `48081` 服务
  - 已验证真实 `GET /showroom/display/website-config` 返回公司封面为公司信息封面，且公司字段缩减为 5 项
  - 已修正过期回归场景：原 `ShowroomHttpApiIntegrationTest.websiteConfigShouldSkipProductsWhoseLivePreviewAssetIsMissingInsteadOfFailingWholeAggregate` 在产品已存在 `cover_image` 的情况下仍期待被跳过，与当前“产品封面优先于 preview asset”合同不一致。
  - 已将该回归场景收敛为“只有当产品既没有 `cover_image`，也没有 live preview asset 时，website-config 才应跳过该产品而不是拖垮整包聚合”。
- Remaining blockers:
  - None.

## Final Verification Result

- PASS: `mvn "-pl" "yudao-module-showroom" "-Dmaven.test.skip=true" compile`
- PASS: `mvn "-pl" "yudao-server" "-am" "-Dmaven.test.skip=true" package`
- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomAppConfigCompanyFieldsContractTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `GET http://127.0.0.1:48081/showroom/display/website-config` -> `company.homeImageUrl=/admin-api/infra/file/28/get/20260521/开园活动图-压缩版.jpg`
- PASS: `GET http://127.0.0.1:48081/showroom/display/website-config` -> `company.publicFields.size == 5`
- PASS: `GET http://127.0.0.1:48081/showroom/display/website-config` -> `company.bilingualPublicFields.size == 5`
- PASS: `Website` 运行态 `/showroom` 与根 `/` 首页封面已切到公司信息封面，且公司详情只展示 5 张卡片
- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#websiteConfigShouldSkipProductsWhoseDisplayImageIsMissingInsteadOfFailingWholeAggregate" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomAppConfigCompanyFieldsContractTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-website-config-company-cover-five-cards --mode preview`
