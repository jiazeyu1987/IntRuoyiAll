# 20260624 排产工单单产线阻断修复

## 任务目标

定位并修复“工单工艺路线缺少可用单产线”阻断，确认它应当是数据前置缺失还是校验逻辑过严，并给出最小可回归修复。

## 经验门禁

- ERP / 金蝶 / OpenAPI：本任务不涉及外部 ERP 调用，仅做本机排产校验定位。
- 生产排产校验：必须先确认阻断来源、准入条件和现有测试，不得直接改成放行或静默降级。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，先定位校验入口再修复。
- 是否存在临时补丁或绕过：否。

## 里程碑

- M1：定位报错来源与触发条件。
- M2：补充/修正回归测试。
- M3：实现最小修复并验证。

## 预期验证

- 针对排产工单准入校验的单测。
- 如需，补充相关服务测试或回归测试。

## 当前状态

- 已完成。

## 完成记录

- M1：完成。定位到自动排产 `simulateLineCandidate` 的共同产线检查。
- M2：完成。补充“无共同单产线”回归测试。
- M3：完成。将阻断提示改为明确的根因说明。

## 最终验证

- `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#preview_shouldExplainWhenRouteProcessesHaveNoCommonSingleLine" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
