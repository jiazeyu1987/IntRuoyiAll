# 活跃订单加入仅保留 QA 与重复检测门禁

## Task Goal

将生产组长“新增活跃订单”的候选与加入限制调整为：订单只要求存在正式 QA/PQC 规程上下文，并执行重复活跃订单检测；移除已确认状态、有效排产唯一、产品路线绑定、ACTIVE 路线版本快照、ERP 数量、计划日期和检验数量规则等非 QA/重复限制。

## Milestones

- [x] M1 记录需求变更与 BDD/TDD 验收口径
- [x] M2 增加后端 RED 测试覆盖放宽后的准入规则
- [x] M3 修改活跃订单候选与新增服务逻辑
- [x] M4 运行定向后端验证并记录结果
- [x] M5 收尾文档与验证报告

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderActiveOrderErpPlannedStartTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260808-active-order-qa-only-gate/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --evidence docs/changes/20260808-active-order-qa-only-gate.md`

## Experience Gates

- 命中 `docs/backend-development.md#零排产活跃订单必须使用发布态正式路线`，但本次用户明确变更准入规则为只保留 QA 与重复检测，旧路线/排产/ERP 数量门禁作为被替换的历史基线记录。
- 命中 `docs/backend-development.md#mes-pqc-项目级检验快照门禁`，保留 QA/PQC 正式身份，不用默认 QA 或空成功冒充。

## Current Status

completed - 已完成 QA-only 准入实现、定向后端验证、证据校验与任务收尾清理。

## Verification Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，旧实现/旧契约缺少 `selectCandidatesByKeyword(...)`，证明测试先行暴露旧候选查询门禁。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesTeamLeaderActiveOrderErpPlannedStartTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，21 tests / 0 failures / 0 errors / 0 skipped。
- Evidence validation: `validate_backend_api.py` -> PASS；`validate_change_request.py` -> PASS。
- Cleanup: `task_closeout.py --task-id 20260808-active-order-qa-only-gate --mode preview/apply` -> PASS，delete `<none>`，blocked `<none>`。
- Scope check: 未执行 Git stage/commit/push，项目当前策略默认不要求 Git 操作。

## Cleanup Keep

- doc/tasks/20260808-active-order-qa-only-gate/backend-api-evidence.md

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；本次按用户明确需求删除非 QA/重复门禁，不吞异常。
- `是否从根因和长期维护角度解决`：是；统一调整候选与新增服务的准入口径，避免前后端或候选/新增不一致。
- `是否存在临时补丁或绕过`：否。
