# DF06 Active Order QA Version Lock

## Task Goal

新建活跃订单时，在同一事务中锁定 dccProjectCodeId、qaRegulationId、qaRegulationVersionId，并按 QA 发布版本里的正式规则 key 生成 PQC 任务。移除后重新激活的订单必须保留原锁定快照和历史任务，不重新读取当天最新 QA。

## Milestones

- [x] 建立 DF06 任务文档、BDD 场景和 RED 证据。
- [x] 修正 active-order 创建链路：使用正式 route-DCC 关系锁定 DCC/QA 快照。
- [x] 修正 PQC task 生成：以 inspectionRuleKey 区分 FIRST/PATROL_AM/PATROL_PM/FINAL，不能按 inspectionType=PATROL 合并上午/下午巡检。
- [x] 运行目标 Maven 和相关静态/证据校验。

## Expected Verification

- mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolActiveOrderMapperTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
- python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260812-frontline-pqc-dcc-qa-df06/backend-api-evidence.md
- git diff --check

## Current Status

ready_for_closeout - DF06 implementation and required verification are complete; supervisor independent gate may run next.

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；订单锁定快照与任务规则身份来自正式 route-DCC 和 QA 发布版本，不从产品、路线名或 MES 工序推断 QA。
- 是否存在临时补丁或绕过：否。

## Applicable Gate Summary

- 后端实现必须先有 BDD/RED，再做 GREEN。
- 一线 PQC 工序与 QA 自有工序独立；DF06 只负责订单锁定快照和 PQC task 规则身份，不负责 DF07+ 的 QA 工序读取组装。
- QA 规程只通过 DCC 项目代码关联；不引入 DCC 侧 QA 绑定表或产品推算。
