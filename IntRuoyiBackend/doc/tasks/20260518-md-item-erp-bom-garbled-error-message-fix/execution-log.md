# Execution Log

- 2026-05-18: Created backend follow-up task package `20260518-md-item-erp-bom-garbled-error-message-fix`.
- BDD: Missing local child item shows readable Chinese error -> Given the ERP BOM contains child material codes that are not mapped into local `mes_md_item`, When the product ERP BOM sync runs, Then the thrown error message clearly says `ERP BOM 子项物料未映射到本地 MES 物料` and includes the missing codes in readable Chinese.
- BDD: Success path remark stays readable -> Given the product ERP BOM sync succeeds, When local BOM rows are rebuilt, Then the operator-facing remark prefix remains readable as `ERP BOM版本`.
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesKingdeeProductBomSyncServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, the missing-child assertion expected readable Chinese but the thrown message remained garbled.
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -am -Dtest=MesKingdeeProductBomSyncServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, the missing-child exception text and success remark prefix are now readable Chinese.
