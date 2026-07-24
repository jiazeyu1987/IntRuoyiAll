# Task: IntRuoyi Showroom App Config Provider

## Goal

在 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro` 的 showroom 后端模块中新增一个面向 `Website` 的前台聚合接口，使 Website 可以一次性读取公司、展厅、产品、预览图、双语字幕和双语音频数据。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-intruoyi-showroom-app-config-provider\**`

## Non-Scope

- 不修改 `Website` 前端渲染逻辑。
- 不读取后台 Vue 页面源码作为数据源。
- 不引入运行时 mock、兼容回退或假成功返回。

## Dependencies

- 需要沿用 showroom 现有真实业务表与 runtime 聚合逻辑：
  - `showroom_company`
  - `showroom_company_revision`
  - `showroom_hall`
  - `showroom_hall_product`
  - `showroom_product`
  - `showroom_product_revision`
  - `showroom_narration_version`
  - `showroom_preview_asset_version`

## Milestones

1. 定义 `Website` 可直接消费的 app-config DTO 和字段契约。
2. 在 showroom runtime 中聚合公司、展厅、产品、预览图、双语字幕和双语音频地址。
3. 暴露公开展示接口，建议 `GET /showroom/display/app-config`。
4. 补齐后端契约测试与 HTTP 集成测试。
5. 支持最终与 `Website` 的真实联调验证。

## Milestone Status

- [x] M1 定义 `Website` 可直接消费的 app-config DTO 和字段契约
- [x] M2 在 showroom runtime 中聚合公司、展厅、产品、预览图、双语字幕和双语音频地址
- [x] M3 暴露 `GET /showroom/display/app-config`
- [x] M4 补齐后端契约测试与 HTTP 集成测试
- [x] M5 输出正式 JSON contract，供 `Website` 仓库执行后续真实联调

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 聚合接口的真实 HTTP 返回体验证
- 与 `Website` 联调前导出或固定一份正式 JSON contract

## Current Status

- Status: Completed
- Completed work:
  - 已在 `ShowroomDisplayController` 新增 `GET /showroom/display/app-config`。
  - 已在 `ShowroomApiRuntime` 聚合公司、展厅、产品、预览图、双语字幕和双语音频 URL。
  - 已将 payload 冻结为 `company + showrooms[].products[]`，并输出 `app-config-contract.json`。
  - 已补充成功聚合与缺 preview asset fail-fast 两条集成测试。
  - 已运行 `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。
- Remaining blockers:
  - Website 仓库侧真实浏览器联调不在本仓库执行范围内，待其 consumer 任务接入已冻结契约后完成。

## Final Verification Result

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: 已输出正式契约文件 `app-config-contract.json`

## Cleanup Keep

- `doc/tasks/20260520-intruoyi-showroom-app-config-provider/development-plan.md`
- `doc/tasks/20260520-intruoyi-showroom-app-config-provider/app-config-contract.json`
- `doc/tasks/20260520-intruoyi-showroom-app-config-provider/backend-api-evidence.md`
