# Execution Log: 展厅公司菜单可见即编辑直存（后端）

BDD: 登录用户可直接保存公司文字 -> Given 用户已登录且从真实菜单进入公司页 / When 调用 `PUT /showroom/company/publish` 保存公司文字 / Then 后端必须直接发布新公司版本并使 `revisionNo` 递增 1，不再以 `showroom_publicity` 角色拦截。

BDD: 登录用户可直接生成并保存公司语音 -> Given 用户已登录且当前公司存在 live 版本 / When 调用公司语音生成与保存接口 / Then 后端必须允许当前调用，直接发布 narration 版本，且不新增公司 revision、也不新增 showroom 审批记录。

RED: `mvn "-Dsurefire.failIfNoSpecifiedTests=false" -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest test` -> FAIL，`companyPublishShouldAllowLoggedInUserAndIncrementRevisionWithoutApproval` 与 `companyNarrationGenerateAndPublishShouldAllowLoggedInUserWithoutChangingCompanyRevision` 仍被 `requirePublicityRole()` 以 `403 / 只有企宣角色可以编辑公司信息` 拦截。

GREEN-PARTIAL: 本次已去掉 `/showroom/company/publish`、`/showroom/company/generate-narration-audio`、`/showroom/company/publish-narration` 的企宣角色门控，并保留登录用户校验。
GREEN-PARTIAL: `mvn "-Dmaven.test.skip=true" -pl yudao-module-showroom -am compile` -> PASS，showroom 主源码编译通过。

BLOCKED: `mvn "-Dsurefire.failIfNoSpecifiedTests=false" -pl yudao-module-showroom -Dtest=ShowroomHttpApiIntegrationTest test` -> FAIL，同一测试文件中与本任务无关的旧展示接口断言仍引用不存在的 `displayController.getAppConfig()` / `AppConfigPayload`，导致无法完成整套 integration 放行。
