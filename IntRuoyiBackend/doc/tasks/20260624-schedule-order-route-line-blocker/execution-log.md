# 执行日志：排产工单单产线阻断修复

## BDD

- BDD: 排产工单存在可用单产线 -> Given 产品已绑定可用工艺路线和单产线 When 进入排产应用 Then 不应报“缺少可用单产线”。
- BDD: 排产工单缺少单产线 -> Given 工艺路线未配置可用单产线 When 进入排产应用 Then 系统应明确阻断并提示缺少单产线的根因。

## TDD 证据

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#preview_shouldExplainWhenRouteProcessesHaveNoCommonSingleLine" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，当前阻断文案仍是“工单工艺路线缺少可用单产线”。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#preview_shouldExplainWhenRouteProcessesHaveNoCommonSingleLine" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，29 个测试通过。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260624-schedule-order-route-line-blocker --mode preview` -> PASS，无删除项、无阻塞。

## 根因

- 阻断并不是“ERP 同步少了一条生产订单”，而是自动排产要求整条工艺路线能落到同一条启用产线。
- 当各工序分别存在工作站，但没有共同可用产线时，代码会进入 `simulateLineCandidate` 的 `pool == null` 分支，原消息过于笼统。

## 风险与范围

- 风险低，仅修改提示文案，不改变准入逻辑。
- 影响范围仅限自动排产中“没有共同可用单产线”的阻断提示。
