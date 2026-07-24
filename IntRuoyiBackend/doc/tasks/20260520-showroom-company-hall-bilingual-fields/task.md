# Task: Showroom Company Hall Bilingual Fields

## Goal

为 showroom 公司与展厅补齐英文名称/描述字段，并把这些字段输出到 `GET /showroom/display/app-config`，供 `Website` 完成完整的中英文切换。

## Current Status

- Status: Completed
- Completed work:
  - 新增公司英文名字段：`showroom_company.display_name_en`
  - 新增展厅英文名/英文描述字段：`showroom_hall.name_en`、`showroom_hall.description_en`
  - 扩展了 DO、领域模型、持久化服务、Admin DTO 和 `app-config` DTO
  - 本地 MySQL `127.0.0.1:23306/ruoyi-vue-pro` 已补列并填充主公司与 8 个展厅的英文名
  - 当前 `48081` runtime 已重新补丁并返回双语 `app-config`
- Remaining blockers:
  - 当前本地库里的展厅描述字段仍为空字符串，因此 `descriptionEn` 也是空字符串

## Final Verification Result

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomFoundationContractTest,ShowroomCompanyContentTest,ShowroomHallContentTest,ShowroomPersistentContentServiceTest,ShowroomHttpApiIntegrationTest#appConfigShouldAggregateCompanyHallProductAndBilingualMedia" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: local MySQL schema now includes `display_name_en`, `name_en`, `description_en`
- PASS: local runtime `GET http://127.0.0.1:48081/showroom/display/app-config` returns bilingual company/hall fields
