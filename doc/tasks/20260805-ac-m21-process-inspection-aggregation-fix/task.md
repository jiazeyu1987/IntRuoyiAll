# AC-M21 过程检验记录汇集修复

## Task Goal

修复 AC-M21「系统汇集过程检验记录」未完全符合项：PQC 组长审核通过后，系统不仅标记汇集状态，还必须形成可追溯、结构化、租户隔离、去重且仅来源于最终确认修订的过程检验汇集明细。

## Milestones

- [x] 建立 AC-M21 BDD/TDD 任务证据与现有 PQC 汇集链路基线。
- [x] 用 RED 测试覆盖结构化汇集、未确认排除、重复排除、最终修订与租户隔离。
- [x] 实现正式结构化过程检验记录汇集持久化与幂等更新。
- [x] 运行 GREEN 与相邻回归验证，并更新岗位矩阵验收证据。
- [x] 完成收尾状态、验证报告与遗留阻塞记录。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesPqcProcessInspectionAggregationServiceTest,MesTeamLeaderSubmissionReviewServiceTest,ProcessPoolTimelineQueryTest,MesProcessPoolSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 必要时补跑前端 AC-M21 静态/E2E 合同，仅在前端证据映射需要修改时触发。
- 技能证据校验：
  - `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260805-ac-m21-process-inspection-aggregation-fix/backend-api-evidence.md`
  - `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260805-ac-m21-process-inspection-aggregation-fix/database-schema-evidence.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；新增结构化过程检验汇集事实表/模型，避免仅靠状态标记或 raw payload 作为验收事实。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

### MES PQC 项目级检验快照门禁

- Trigger: PQC 填写、PQC 组长复核、QA 检验规程、检验设备、接收标准、检验方法、参数上下限、`itemResults`、`rawPayload.pqcPieceValues`、`pqcItemDetails`。
- Preflight check: PQC 事实必须来自发布规程和结构化 `itemResults[]`，后端提交时冻结设备、编号、方法、标准、上下限、单位、精度、实测值和判定。
- Blocker: 后端仍把 `rawPayload.pqcPieceValues` 当权威、缺发布规程项目或设备主数据时默认成功，必须停止。
- Verification: 后端回归覆盖项目级明细冻结、组长复核后读取结构化 PQC 明细，拒绝用固定四项字段或 raw payload 替代。
- Forbidden action: 禁止用固定四项字段、前端文案、默认上下限、空标准、raw payload 或 API-only 展示替代正式项目级快照。
- Evidence: `docs/backend-development.md#MES PQC 项目级检验快照门禁`。

### PQC 过程检验汇集最终确认门禁

- Trigger: AC-M21、过程检验记录汇集、PQC 组长复核通过、`aggregateApprovedPqcSubmission`、`mes_pqc_process_inspection_aggregate_detail`。
- Preflight check: 汇集必须来自租户/事件/任务一致的正式 `SUBMITTED` PQC 任务和结构化逐件明细，并在同一事务中确认任务、标记汇集、写入明细。
- Blocker: 只有状态标记、仍从 raw payload 汇集、缺唯一键去重、未排除未确认/旧修订/跨租户/重复数据，必须停止。
- Verification: 覆盖成功汇集、重复 CAS、跨租户拒绝、无明细拒绝、任务确认 CAS 失败和 schema 唯一键。
- Forbidden action: 禁止用前端展示、默认空明细、状态字段或吞唯一键异常替代结构化汇集事实。
- Evidence: `docs/backend-development.md#PQC 过程检验汇集必须形成最终确认明细`。

## Current Status

completed

- AC-M21 代码修复和目标验证已完成。
- 最小 Maven 门禁已在并发 Maven 清空后复跑通过：`MesPqcProcessInspectionAggregationServiceTest`、`MesProcessPoolSchemaTest`、`MesQaPqcSchemaTest` 共 12 个测试 PASS。
- cleanup apply 已完成，仅保留 `task.md`、`execution-log.md`、`verification-report.md`。
- Git 提交/推送仍需按共享工作区的其它任务改动做单独集成处理。
