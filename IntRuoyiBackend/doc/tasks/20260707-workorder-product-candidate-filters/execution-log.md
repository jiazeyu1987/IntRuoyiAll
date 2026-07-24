# 生产工单产品候选过滤后端执行日志

BDD: 候选产品 ID 过滤生产工单分页 -> Given 前端选择产品名称或产品编码候选 / When 调用 `/mes/pro/work-order/page` / Then 后端按候选产品 ID 过滤生产工单。

BDD: 多个产品过滤条件保持一致 -> Given 请求同时包含现有产品选择器和候选产品 ID / When 产品 ID 一致 / Then 后端按该产品 ID 查询；When 产品 ID 不一致 / Then 返回空结果，不静默改用某一个条件。

RED: `mvn.cmd -pl yudao-module-mes -Dtest=MesProWorkOrderServiceImplTest test` -> FAIL，`MesProWorkOrderPageReqVO` 尚无 `setProductNameFilterId` / `setProductCodeFilterId`。

GREEN: `node yudao-module-mes/src/test/js/workorder-product-candidate-filters-static.spec.cjs` -> PASS，后端 VO、service、mapper 与目标单测合同均覆盖新增候选过滤。

GREEN: `mvn.cmd -pl yudao-module-mes -DskipTests compile` -> PASS，后端主代码编译通过。

BLOCKER: `mvn.cmd -pl yudao-module-mes -Dtest=MesProWorkOrderServiceImplTest test` -> BLOCKED，同模块无关测试源码 `ThirdPartyFeedbackImportServiceImplTest` 编译失败，缺少 `MesMdWorkstationMapper.selectListByIds` 与 `ThirdPartyFeedbackImportResult.getDirectWorkReportDetails` / `DirectWorkReportDetail`，导致 Maven testCompile 阶段失败，未进入本次目标测试执行。
