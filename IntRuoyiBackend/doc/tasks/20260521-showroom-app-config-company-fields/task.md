# Task: Showroom App Config Company Fields

## Goal

在 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 的匿名 `GET /showroom/display/app-config` 中扩展公司节点，新增 `company.publicFields`，让 `Website` 的 `/showroom` 可以读取公司公开字段并渲染公司入口与详情；匿名访问面以当前 showroom 公开展示合同为准，本任务不单独扩大或收缩它。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-app-config-company-fields\**`

## Non-Scope

- 不单独改写既有 showroom 公开展示面的权限边界；若该边界后续由独立任务调整，以该任务为准。
- 不修改 `Website` 前端渲染代码。
- 不改数据库 schema，不引入 fallback、mock 或兼容回退。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-display-company-field-labels\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 公司字段标签合同已统一，本次可以直接在 `app-config` 中复用现有公司公开字段输出，不需要额外前端映射。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 目标 showroom 文件存在与本任务无关的未提交改动。
- Impact: 本次必须仅在 `app-config` 相关代码与本任务文档范围内追加改动，不能覆盖现有在途更改。

## Dependencies

- `ShowroomApiRuntime` 必须能从当前公司 live revision 提取公开字段。
- `ShowroomDisplayController.getAppConfig()` 必须保持匿名可读，并与当前 showroom 公开展示面保持一致。
- `ShowroomHttpApiIntegrationTest` 可用于锁定匿名访问边界与 payload 合同。

## Milestones

1. 创建任务文档和后端证据骨架，冻结 `company.publicFields` 合同。
2. 先补 RED 测试，锁定匿名 `app-config` 需要返回 `company.publicFields`，并且其他 display 路由仍不可匿名放通。
3. 最小实现 `AppConfigCompany.publicFields` 聚合输出，缺失对象时 fail fast，显式空数组允许通过。
4. 跑通定向后端测试并回写 GREEN 与证据。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomAppConfigCompanyFieldsContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-app-config-company-fields\backend-api-evidence.md`

## Milestone Status

### Milestone 1

- Status: Completed
- Completed work:
  - 已创建任务目录、执行日志和后端证据骨架。
  - 已锁定本次只扩展 `app-config.company.publicFields` 合同，不额外引入 fallback 或新数据来源。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-app-config-company-fields\task.md`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-app-config-company-fields\execution-log.md`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-app-config-company-fields\backend-api-evidence.md`
- Remaining blockers:
  - RED tests and app-config payload implementation are still pending.

### Milestone 2

- Status: Completed
- Completed work:
  - 已先补 RED 契约断言，锁定 `AppConfigCompany.publicFields` 新字段和匿名访问边界。
  - 在现有大集成测试文件暴露出与本需求无关的历史链路阻塞后，已把本次验证拆到独立 `ShowroomAppConfigCompanyFieldsContractTest`。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\integration\ShowroomAppConfigCompanyFieldsContractTest.java`
  - `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest#appConfigEndpointShouldBeAnnotatedPermitAllWithoutOpeningOtherDisplayRoutes+appConfigShouldAggregateCompanyHallProductAndBilingualMedia+appConfigShouldReturnExplicitEmptyCompanyPublicFieldsWhenNoCompanyDisplayFieldHasValue" "-Dsurefire.failIfNoSpecifiedTests=false" test` (RED)
- Remaining blockers:
  - None.

### Milestone 3

- Status: Completed
- Completed work:
  - 已扩展 `ShowroomDisplayController.AppConfigCompany`，加入 `publicFields`。
  - 已在 `ShowroomApiRuntime.toAppConfigCompany(...)` 中聚合公司公开字段，显式空数组允许通过。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\display\ShowroomDisplayController.java`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\controller\ShowroomApiRuntime.java`
- Remaining blockers:
  - None.

### Milestone 4

- Status: Completed
- Completed work:
  - 已跑通新的独立契约测试并通过后端证据校验。
  - 已验证 `app-config` 公开合同与当前 showroom 展示路由公开面保持一致。
- Verification evidence:
  - `mvn -pl yudao-module-showroom "-Dtest=ShowroomAppConfigCompanyFieldsContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-app-config-company-fields\backend-api-evidence.md`
- Remaining blockers:
  - None.

## Current Status

- Status: Completed
- Completed work:
  - 匿名 `app-config` 已新增 `company.publicFields`。
  - 新契约已通过独立后端合同测试，且未扩大其他 `showroom/display/*` 的匿名访问范围。
- Remaining blockers:
  - None.

## Final Verification Result

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomAppConfigCompanyFieldsContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-app-config-company-fields\backend-api-evidence.md`
