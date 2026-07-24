# Execution Log: 生产工单冻结分页排序支持

BDD: 生产工单分页应优先返回未冻结工单 -> Given 生产工单表中同时存在未冻结和冻结工单 / When 前端请求生产工单分页第一页 / Then 返回结果应先出现 `temporaryFrozen=false` 的工单，再出现 `temporaryFrozen=true` 的工单。

RED: real frontend verification before backend page-order fix -> FAIL, page 1 visible rows were all frozen even after the frontend list component applied same-page sorting, proving backend pagination still ordered by `id desc`.

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\pom.xml -Dtest=MesProWorkOrderMapperTest,MesProWorkOrderServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, blocked by unrelated pre-existing missing test classes under `pro.feedback.importer` and `pro.workorder.sync`, so the new mapper test could not reach execution.

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS after stopping the running backend jar to release the file lock.

GREEN: real page verification after backend rebuild -> PASS, page 1 now exposed non-frozen rows first and the first frozen row appeared at visible index 2.
