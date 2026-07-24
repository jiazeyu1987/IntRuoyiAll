# Execution Log

BDD: 预览重排解析路线工序身份 -> Given 历史排产工序和当前路线中存在相同工序编码的多个 routeProcess 候选，When 预览重排需要解析当前路线工序身份，Then 必须使用可追溯的路线工序身份输入唯一解析，不能因为 routeId 缺失而抛出误导性的候选歧义。

RED: mvn -pl yudao-module-mes -Dtest=MesProRouteProcessServiceImplTest#getProcessIdentityMap_shouldPreserveExplicitTargetsWhenTargetCodesDuplicate test -> FAIL, expected reason: `getProcessIdentityMap` 把同编码的两个显式目标工序 `900394/922894` 当作歧义候选并抛出 `PRO_ROUTE_PROCESS_IDENTITY_AMBIGUOUS`，复现用户报错 `routeId=null，sourceProcessId=922894，processCode=Z2630，candidateRouteProcessIds=[900394, 922894]`。

GREEN: mvn -pl yudao-module-mes -Dtest=MesProRouteProcessServiceImplTest#getProcessIdentityMap_shouldPreserveExplicitTargetsWhenTargetCodesDuplicate test -> PASS，显式目标工序同编码时保留自身身份映射。

GREEN: mvn -pl yudao-module-mes -Dtest=MesProRouteProcessServiceImplTest test -> PASS，11 tests；同时覆盖重复目标编码保留显式身份、外部别名遇重复目标编码继续 fail fast。

REGRESSION: python -X utf8 -m pytest script/tests/test_mes_scheduling_identity_keys.py script/tests/test_mes_scheduling_domain_contracts.py -> FAIL，静态契约仍要求旧 `process.getRouteProcessId()` 返回片段，当前实现已通过 `resolveCurrentRouteProcess` 固化当前路线工序身份；需同步契约到当前口径后复跑。

GREEN: python -X utf8 -m pytest script/tests/test_mes_scheduling_identity_keys.py script/tests/test_mes_scheduling_domain_contracts.py -> PASS，5 passed；静态契约已更新为当前路线工序身份解析口径。

REGRESSION: mvn -pl yudao-module-mes -Dtest=MesProRouteProcessServiceImplTest test -> PASS，11 tests；修复后重复目标编码、外部别名歧义和原有历史工序映射均保持预期。

GREEN: merge-int_main -> PASS，后端 `int_main` 合并提交 `ef19a2d1f3`。

GREEN: merged-python-contracts -> PASS，`python -X utf8 -m pytest script/tests/test_mes_scheduling_identity_keys.py script/tests/test_mes_scheduling_domain_contracts.py` 在合并后主线通过，5 passed。

GREEN: merged-route-process-regression -> PASS，`mvn -pl yudao-module-mes -Dtest=MesProRouteProcessServiceImplTest test` 在合并后主线通过，11 tests。
