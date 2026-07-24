# 执行日志：20260630-erp-production-material-list-grouped-popup

BDD: 后端可返回生产用料清单单据汇总页 -> Given 当前租户存在多条生产用料清单明细 / When 调用 group-page / Then 返回按 sourceBillNo 聚合的单据汇总行。
BDD: 后端可返回整单子项明细 -> Given 某 sourceBillNo 存在多条子项分录 / When 调用 detail-list / Then 返回该单据完整子项明细及关联工单字段。

GREEN: `Get-Content -Encoding utf8` 读取经验门禁与根仓任务证据 -> PASS。
RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesKingdeeProductionMaterialListSchemaTest,MesKingdeeProductionMaterialListQueryServiceImplTest,MesKingdeeProductionMaterialListMapperXmlTest" -Dsurefire.failIfNoSpecifiedTests=false test` -> 初次 FAIL，`BaseDbUnitTest` 默认大基包扫描卷入 DCC 现有依赖异常，不属于本任务业务逻辑缺陷。
GREEN: `MesKingdeeProductionMaterialListQueryServiceImplTest` 增加 `yudao.info.base-package=cn.iocoder.yudao.module.mes` 范围隔离后回跑目标测试 -> PASS。
