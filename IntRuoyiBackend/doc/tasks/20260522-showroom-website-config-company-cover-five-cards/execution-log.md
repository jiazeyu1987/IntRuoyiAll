BDD: website-config 公司封面对齐公司信息 -> Given 公司工作台保存了 `cover_image`, When 匿名 `GET /showroom/display/website-config` 返回 company payload, Then `homeImageUrl` 必须直接等于公司信息里的 `cover_image`，而不是公司预览资产 URL。
BDD: website-config 公司卡片对齐 5 项可见字段 -> Given 公司工作台只对外显示 5 张介绍卡片, When 匿名 `GET /showroom/display/website-config` 返回 company fields, Then `publicFields` 和 `bilingualPublicFields` 只返回 `development_history`、`park_introduction`、`incubation_platform`、`subsidiary_overview`、`stock_info`，并排除 `core_manufacturing_capability` 与 `honors_awards`。
RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomAppConfigCompanyFieldsContractTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 旧 contract 仍断言 `homeImageUrl` 来自 preview asset、公司字段仍为 7 项；同时工作区里 `ShowroomApiRuntimeBatchPublishTest` 与主代码签名未对齐。
GREEN: `mvn "-pl" "yudao-module-showroom" "-Dmaven.test.skip=true" compile` -> PASS
GREEN: `mvn "-pl" "yudao-server" "-am" "-Dmaven.test.skip=true" package` -> PASS
GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomAppConfigCompanyFieldsContractTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
GREEN: `restart-ruoyi.bat` -> PASS，本地 `48081/8081` 环境已重启
GREEN: 实时探针 `GET http://127.0.0.1:48081/showroom/display/website-config` -> PASS，`company.homeImageUrl` 已变为公司信息封面 `/admin-api/infra/file/28/get/20260521/开园活动图-压缩版.jpg`
GREEN: 实时探针 `GET http://127.0.0.1:48081/showroom/display/website-config` -> PASS，`company.publicFields` 与 `company.bilingualPublicFields` 均为 5 项
GREEN: 前台运行态核验 -> PASS，`/showroom` 与根 `/` 首页封面均为公司信息封面，两个公司详情页字段数均为 5
RED: `mvn -pl yudao-module-showroom "-Dtest=ShowroomAppConfigCompanyFieldsContractTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`ShowroomHttpApiIntegrationTest.websiteConfigShouldSkipProductsWhoseLivePreviewAssetIsMissingInsteadOfFailingWholeAggregate` 仍把“已有 admin `cover_image` 但没有 live preview asset”的产品当成应该被跳过的旧合同，断言 `expected: <1> but was: <2>`。
GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#websiteConfigShouldSkipProductsWhoseDisplayImageIsMissingInsteadOfFailingWholeAggregate" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，回归场景已收敛为“既没有 `cover_image` 也没有 live preview asset 时才跳过产品”。
GREEN: `mvn -pl yudao-module-showroom "-Dtest=ShowroomAppConfigCompanyFieldsContractTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
ROOT CAUSE: 当前 `ShowroomApiRuntime.resolveProductDisplayImageUrl()` 已明确定义产品展示图优先取 `cover_image`，旧集成测试仍按“缺 live preview asset 必跳过产品”假设构造场景，导致合同测试与现行运行时语义不一致。
